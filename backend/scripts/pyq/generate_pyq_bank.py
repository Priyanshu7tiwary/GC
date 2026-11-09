"""Build the playable CAT 2017-2025 previous-year papers from the extracted question bank and answer keys.

Inputs (repository-relative):
  papers/extracted/cat_pyq_questions.jsonl        question text + options, from papers/extracted/extract_cat_papers.py
  resources/cat_pyq_answer_keys/answer_keys.csv   verified keys, from match_answer_keys.py

  resources/cat_pyq_answer_keys/source_overlay.jsonl  maths/figure repairs, from build_source_overlay.py

Outputs:
  db/migration/V7__import_cat_pyq_papers.sql            initial import (only written when missing)
  db/migration/V9__repair_pyq_text_and_keys.sql         brings every PYQ question to the current text and key

Papers are served by the API from the database; no paper files are written for the browser.
Question text may contain inline TeX between \\( and \\) and figures as [[img:/pyq/img/<file>]] markers.

Questions whose key could not be verified keep answer = null; they are shown in the paper but not scored.

Usage (from the repository root): python3 backend/scripts/pyq/generate_pyq_bank.py
"""
import collections
import csv
import json
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[3]
BANK = ROOT / 'papers/extracted/cat_pyq_questions.jsonl'
KEYS = ROOT / 'resources/cat_pyq_answer_keys/answer_keys.csv'
OVERLAY = ROOT / 'resources/cat_pyq_answer_keys/source_overlay.jsonl'
TOPICS_CSV = ROOT / 'resources/cat_pyq_answer_keys/question_topics.csv'
MIGRATIONS = ROOT / 'backend/src/main/resources/db/migration'
OUT_SQL = MIGRATIONS / 'V7__import_cat_pyq_papers.sql'          # written once; Flyway checksums it after that
FIX_SQL = MIGRATIONS / 'V9__repair_pyq_text_and_keys.sql'       # written once: text repairs and key fixes
TOPIC_SQL = MIGRATIONS / 'V11__question_topics.sql'             # written once: syllabus topic + label per question
SECTION_ORDER = ['VARC', 'DILR', 'QA']

# ---------------------------------------------------------------- text clean-up

TABULAR = re.compile(r'\S {2,}\S.* {2,}\S')  # a layout line with two or more wide gaps is a table row


def strip_markers(t):
    t = t.replace('\r', '').replace('\f', '\n').replace('\ue020', '√')  # the PDFs' private-use radical glyph
    t = re.split(r'Type in the answer\s*:?', t)[0]
    t = re.sub(r'(?m)^\s*Q\.\s*\d+\s+Question\s+\d+\s*-\s*continued\s*$', '', t)
    t = re.sub(r'(?m)^\s*Q\.\s*\d+\s+', '', t)
    t = re.sub(r'\s*Question Type\s*:\s*[A-Z]+\b', '', t)
    t = re.sub(r'-\s*End of Question Paper\s*-', '', t)
    return t


def drop_page_break_repeat(t):
    """A page break can repeat the opening of a question; keep only the last full copy."""
    head = t.strip()[:80]
    if len(head) < 40:
        return t
    last = t.rfind(head)
    return t[last:] if last > t.find(head) else t


def drop_inline_options(t, options):
    """MCQ choices are stored separately, so cut the trailing '1. ... 2. ...' list off the prompt."""
    if not options:
        return t
    words = options[0].split()[:4]
    if not words:
        return t
    pat = r'(?:^|\s)1\.\s+' + r'\s+'.join(map(re.escape, words))
    hits = list(re.finditer(pat, t))
    return t[:hits[-1].start()] if hits else t


def reflow(t):
    """Join layout-wrapped lines into paragraphs; keep short lines, list items and table rows on their own."""
    out = []
    for line in t.split('\n'):
        s = line.rstrip()
        if not s.strip():
            if out and out[-1] != '':
                out.append('')
            continue
        table = bool(TABULAR.search(s))
        s = s if table else re.sub(r'\s+', ' ', s).strip()
        prev = out[-1] if out else ''
        joinable = (prev and prev != '' and not prev.endswith('\u0000')
                    and len(prev) >= 90 and not table and not re.match(r'^(\d+|[A-E]|[IVX]+)\.\s', s))
        if joinable:
            out[-1] = prev + ' ' + s
        else:
            out.append(s + ('\u0000' if table else ''))
    text = '\n'.join(x.replace('\u0000', '') for x in out)
    return re.sub(r'\n{3,}', '\n\n', text).strip()


def clean_option(o):
    o = o.replace('\ue020', '√')
    o = re.sub(r'\s*\bOptions\s*(?:Type in the answer:?\s*_*)?\s*$', '', o)
    o = re.sub(r'-\s*End of Question Paper\s*-', '', o)
    return re.sub(r'\s+', ' ', o).strip()


def clean(q):
    t = strip_markers(q['question_text'])
    t = drop_page_break_repeat(t)
    t = drop_inline_options(t, q['options'])
    return reflow(t)

# ---------------------------------------------------------------- passages


def dense(t):
    """Non-whitespace characters with their positions, so page-break reflow differences don't break matching."""
    return [(c, i) for i, c in enumerate(t) if not c.isspace()]


def common_prefix(a, b):
    n = 0
    for (x, _), (y, _) in zip(a, b):
        if x != y:
            break
        n += 1
    return n


def split_passages(qs):
    """Questions in a set repeat the same passage or set text; lift it out so the player shows it once."""
    texts = [clean(q) for q in qs]
    dens = [dense(t) for t in texts]
    result = [(None, t) for t in texts]
    i = 0
    while i < len(qs):
        j, prefix = i + 1, len(dens[i])
        while j < len(qs):
            p = common_prefix(dens[i][:prefix], dens[j])
            if p < 200:
                break
            prefix, j = p, j + 1
        if j - i >= 2:
            # snap back to the last paragraph or sentence boundary inside the shared text
            end = dens[i][prefix - 1][1] + 1
            head = texts[i][:end]
            snap = max(head.rfind('\n'), head.rfind('. '), head.rfind('? '), head.rfind('." '))
            if snap > 150 and head.rstrip()[-1:] not in '.?!"”’):':
                end = snap + (0 if head[snap] == '\n' else 1)
                if head[snap:snap + 3] == '." ':
                    end = snap + 2
            shared = sum(1 for c in texts[i][:end] if not c.isspace())
            passage = texts[i][:end].strip()
            for k in range(i, j):
                cut = dens[k][shared][1] if shared < len(dens[k]) else len(texts[k])
                prompt = texts[k][cut:].strip()
                result[k] = (passage, prompt or texts[k])
        i = j
    return result

# ---------------------------------------------------------------- tagging


VARC_TYPES = [
    (r'odd one out|odd sentence', 'Odd sentence out'),
    (r'proper(?:ly)? sequenc|coherent paragraph|proper order|logical order', 'Para jumbles'),
    (r'summar|essence of the passage', 'Para summary'),
    (r'(?:sentence|blank).{0,80}(?:inserted|placed|fit|best placed)|best placed', 'Sentence placement'),
    (r'\btone\b|attitude', 'Tone & Attitude'),
    (r'weaken|strengthen|undermine|support the', 'Strengthen / Weaken'),
    (r'main idea|central (?:idea|theme|argument)|title|primary (?:purpose|concern)|best captures', 'Main idea / Title'),
    (r'infer|impl|suggest|assum|except', 'Inference questions'),
]
DILR_TYPES = [
    (r'tournament|match(?:es)?\b|played|league|round', 'Games & Tournaments'),
    (r'\bpie\b', 'Pie charts'),
    (r'graph|chart|bar\b', 'Bar & Line graphs'),
    (r'venn', 'Venn diagrams'),
    (r'seated|sitting|sit\b|circular table|row of|facing', 'Arrangements (linear/circular)'),
    (r'schedul|slot|calendar|day of the week|monday|tuesday', 'Scheduling & Calendars'),
    (r'route|flight|cities|city|network|road', 'Networks & Routes'),
    (r'\bcube|dice\b', 'Cubes & Dice'),
    (r'truth|lie|liar|truth-teller', 'Binary logic (truth/lie)'),
    (r'selected|selection|committee|distribut', 'Selections & Distributions'),
]
QA_TYPES = [
    (r'probab', 'Probability'),
    (r'ways|arrange|permutation|combination|selected', 'Permutations & Combinations'),
    (r'\blog', 'Logarithms'),
    (r'triangle', 'Triangles'),
    (r'circle|radius|chord|tangent', 'Circles'),
    (r'square|rectangle|polygon|hexagon|quadrilateral|rhombus|trapez|parallelogram', 'Polygons & Quadrilaterals'),
    (r'coordinate|x-axis|y-axis|straight line|\(\s*-?\d+\s*,\s*-?\d+\s*\)', 'Coordinate geometry'),
    (r'cone|cylinder|sphere|cube|volume|hemisphere|prism|pyramid', '3D Mensuration'),
    (r'interest', 'Simple & Compound Interest'),
    (r'profit|loss|discount|cost price|selling price|marked price', 'Profit, Loss & Discount'),
    (r'mixture|alloy|solution|alligation|concentration', 'Mixtures & Alligation'),
    (r'speed|km|train|boat|stream|race|distance', 'Time, Speed & Distance'),
    (r'work|pipe|tank|cistern|days to complete', 'Time & Work'),
    (r'average|mean', 'Averages'),
    (r'percent|%', 'Percentages'),
    (r'ratio|proportion', 'Ratio & Proportion'),
    (r'remainder|divisible|divided by', 'Divisibility & Remainders'),
    (r'\bhcf\b|\blcm\b|gcd', 'HCF & LCM'),
    (r'factor|divisor', 'Factors'),
    (r'last digit|unit digit|units digit', 'Unit & last digits'),
    (r'base \d', 'Base systems'),
    (r'\bset\b|sets\b', 'Set theory'),
    (r'arithmetic progression|geometric progression|\ba\.?p\.?\b|\bg\.?p\.?\b|terms? of|series', 'Progressions (AP/GP/HP)'),
    (r'sequence', 'Sequences & Series'),
    (r'f\s*\(|function|g\s*\(', 'Functions & Graphs'),
    (r'\|\s*x|modulus|inequalit|maximum|minimum|<|>|≤|≥', 'Inequalities & Modulus'),
    (r'quadratic|roots?|x\s*2|x²', 'Quadratic equations'),
    (r'√|sqrt|surd|power|indices|\^', 'Surds & Indices'),
]


def classify(section, text):
    table = {'VARC': VARC_TYPES, 'DILR': DILR_TYPES, 'QA': QA_TYPES}[section]
    low = text.lower()
    for pat, name in table:
        if re.search(pat, low):
            return name
    return {'VARC': 'Specific detail', 'DILR': 'Tables & Caselets', 'QA': 'Linear equations'}[section]


FIGURE = re.compile(r'\b(?:graphs?|charts?|figures?|diagrams?|grid|following (?:map|picture|image)|shown (?:below|above|in the))\b', re.I)

# ---------------------------------------------------------------- build


def build():
    bank = [json.loads(line) for line in BANK.read_text().splitlines() if line.strip()]
    keys = {r['id']: r for r in csv.DictReader(KEYS.open())}
    overlay = {e['id']: e for e in map(json.loads, OVERLAY.read_text().splitlines())} if OVERLAY.exists() else {}
    topics = {r['id']: r for r in csv.DictReader(TOPICS_CSV.open())} if TOPICS_CSV.exists() else {}
    papers = collections.defaultdict(lambda: collections.defaultdict(list))
    for q in bank:
        papers[(q['year'], q['slot'])][q['section']].append(q)

    sql_questions, sql_mocks, totals = [], [], collections.Counter()
    for (year, slot), sections in sorted(papers.items()):
        mock_id = f'pyq-{year}-{slot}'
        paper_qs, ids = [], []
        for section in SECTION_ORDER:
            qs = sorted(sections[section], key=lambda q: q['question_number'])
            items = []
            for q, (passage, prompt) in zip(qs, split_passages(qs)):
                key = keys.get(q['id'], {})
                fix = overlay.get(q['id'], {})
                mcq = q['type'] == 'MCQ'
                if 'answer' in fix:      # options taken in the source's order carry the source's key
                    raw, keyed_by = fix['answer'], 'source'
                elif key.get('status') == 'verified_match':
                    raw, keyed_by = key.get('answer'), 'matched'
                else:
                    raw, keyed_by = None, None
                answer = (int(raw) if mcq else raw) if raw not in (None, '') else None
                prompt = fix.get('prompt', prompt)
                options = fix.get('options') or ([clean_option(o) for o in q['options']] if mcq else [])
                text_for_tags = (passage or '') + '\n' + prompt
                note = {'matched': f"Answer key cross-checked against {key.get('source')}",
                        'source': f"Question and answer key as transcribed by {key.get('source')}",
                        None: 'No verified answer key is available for this question, so it is not scored.'}[keyed_by]
                tp = topics.get(q['id'])
                items.append({
                    'id': q['id'], 'section': section, 'number': q['question_number'],
                    'topic': tp['topic'] if tp else classify(section, prompt if section == 'VARC' else text_for_tags),
                    'topicId': tp['topic_id'] if tp else None,
                    'label': f"{section} › {tp['chapter']} › {tp['topic']}" if tp else None,
                    'type': 'mcq' if mcq else 'tita',
                    'passage': passage, 'q': prompt, 'options': options, 'answer': answer,
                    'explanation': f"Official CAT {year} Slot {slot}, {section} Q{q['question_number']}. {note}",
                    'figure': section != 'VARC' and bool(FIGURE.search(text_for_tags)),
                    '_images': fix.get('images', []),
                })
            # figures belong to the set: attach every figure found for any question of a set to the shared text
            by_passage = collections.defaultdict(list)
            for it in items:
                for img in it['_images']:
                    if img not in by_passage[it['passage']]:
                        by_passage[it['passage']].append(img)
            for it in items:
                imgs = by_passage[it['passage']] if it['passage'] else it['_images']
                if imgs:
                    block = '\n\n' + '\n'.join(f'[[img:{u}]]' for u in imgs)
                    if it['passage']:
                        it['passage'] = it['passage'] + block
                    else:
                        it['q'] = it['q'] + block
                    it['figure'] = False
                del it['_images']
            for item in items:
                paper_qs.append(item)
                ids.append(item['id'])
                totals['questions'] += 1
                totals['keyed' if item['answer'] is not None else 'unkeyed'] += 1
                totals['figure_missing'] += item['figure']
                totals['with_figures'] += '[[img:' in (item['passage'] or '') + item['q']
                totals['with_passage'] += item['passage'] is not None
                totals['math_text'] += '\\(' in item['q'] + ''.join(item['options'])
                sql_questions.append((item, len(sql_questions) + 1))
        counts = {s: len(sections[s]) for s in SECTION_ORDER}
        sql_mocks.append((mock_id, ids, counts, sum(counts.values())))
    # Flyway checksums applied migrations, so each is written once; later changes need a new version
    if not OUT_SQL.exists():
        write_sql(sql_questions, sql_mocks)
    if not FIX_SQL.exists():
        write_fix_sql(sql_questions)
    if not TOPIC_SQL.exists():
        write_topic_sql(sql_questions)
    print(dict(totals), 'papers:', len(papers))


def lit(v):
    return 'NULL' if v is None else "'" + str(v).replace("'", "''") + "'"


def write_sql(questions, mocks):
    lines = [
        '-- Generated by backend/scripts/pyq/generate_pyq_bank.py. Do not edit by hand.',
        '-- Replaces the CAT 2017-2025 paper PDFs with the papers themselves as playable question sets.',
        '',
        '-- PYQ questions belong to a paper; bank-drawn sectional and custom mocks keep using paper_id IS NULL.',
        'ALTER TABLE questions ADD COLUMN paper_id VARCHAR(64);',
        'ALTER TABLE questions ADD COLUMN question_number INTEGER;',
        'ALTER TABLE questions ADD COLUMN has_figure BOOLEAN NOT NULL DEFAULT FALSE;',
        '-- Questions without a verified key are shown but not scored.',
        'ALTER TABLE questions ALTER COLUMN answer DROP NOT NULL;',
        'CREATE INDEX ix_questions_paper ON questions (paper_id);',
        '',
        '-- PDFs are gone: drop their columns, and the 2016 entries that never had an official paper.',
        'ALTER TABLE mocks DROP COLUMN paper_asset_path;',
        'ALTER TABLE mocks DROP COLUMN paper_source_url;',
        'ALTER TABLE mocks DROP COLUMN paper_source_status;',
        "DELETE FROM mocks WHERE kind = 'PREVIOUS_YEAR' AND exam_year < 2017;",
        "UPDATE cat_paper_archive_years SET note = 'Official question paper imported as a playable test.' WHERE source_status = 'OFFICIAL_PAPER_IMPORTED';",
        '',
    ]
    diff = {'VARC': 'MODERATE', 'DILR': 'HARD', 'QA': 'MODERATE'}
    for (q, pos) in questions:
        if (pos - 1) % 200 == 0:
            if pos > 1:
                lines[-1] = lines[-1].rstrip(',') + ';'
            lines.append('INSERT INTO questions (id, section, topic, type, prompt, passage, options, answer, explanation, difficulty, position, paper_id, question_number, has_figure) VALUES')
        opts = "'" + json.dumps(q['options'], ensure_ascii=False).replace("'", "''") + "'::jsonb" if q['type'] == 'mcq' else 'NULL'
        paper = 'pyq-' + q['id'].split('-')[1] + '-' + q['id'].split('-')[2][1:]
        lines.append(f"({lit(q['id'])}, {lit(q['section'])}, {lit(q['topic'])}, {lit(q['type'].upper())}, {lit(q['q'])}, {lit(q['passage'])}, "
                     f"{opts}, {lit(q['answer'])}, {lit(q['explanation'])}, {lit(diff[q['section']])}, {1000 + pos}, {lit(paper)}, {q['number']}, {'TRUE' if q['figure'] else 'FALSE'}),")
    lines[-1] = lines[-1].rstrip(',') + ';'
    lines.append('')
    for mock_id, ids, counts, total in mocks:
        lines.append(f"UPDATE mocks SET question_ids = {lit(json.dumps(ids))}::jsonb, section_question_counts = {lit(json.dumps(counts))}::jsonb, "
                     f"question_count = {total} WHERE id = {lit(mock_id)};")
    OUT_SQL.write_text('\n'.join(lines) + '\n')


def write_topic_sql(questions):
    """Links every question to a syllabus topic and stores a readable label (section › chapter › topic)."""
    lines = [
        '-- Generated by backend/scripts/pyq/generate_pyq_bank.py. Do not edit by hand.',
        '-- One question bank per syllabus topic: every question links to a topic and carries a readable label.',
        '',
        'ALTER TABLE questions ADD COLUMN topic_id VARCHAR(160) REFERENCES topics (id);',
        'ALTER TABLE questions ADD COLUMN topic_label VARCHAR(200);',
        'CREATE INDEX ix_questions_topic ON questions (topic_id);',
        '',
        '-- official-paper questions: topics from backend/scripts/pyq/classify_topics.py (resources/cat_pyq_answer_keys/question_topics.csv)',
    ]
    for q, _ in questions:
        if q.get('topicId'):
            lines.append(f"UPDATE questions SET topic_id = {lit(q['topicId'])} WHERE id = {lit(q['id'])};")
    lines += [
        '',
        '-- everything else (practice bank, imports, AI questions) by exact topic name within its section',
        'UPDATE questions q SET topic_id = t.id FROM topics t',
        ' WHERE q.topic_id IS NULL AND t.section = q.section AND lower(t.name) = lower(q.topic);',
        '',
        "UPDATE questions q SET topic = t.name, topic_label = t.section || ' › ' || t.chapter || ' › ' || t.name",
        '  FROM topics t WHERE t.id = q.topic_id;',
        "UPDATE questions SET topic_label = section || ' › Unassigned › ' || topic WHERE topic_id IS NULL;",
        'ALTER TABLE questions ALTER COLUMN topic_label SET NOT NULL;',
        '',
        '-- the bank for each topic: how many questions it holds, and where they came from',
        'CREATE VIEW question_bank AS',
        "SELECT t.id AS topic_id, t.section, t.chapter, t.name AS topic, t.section || ' › ' || t.chapter || ' › ' || t.name AS label,",
        '       count(q.id) AS questions, count(q.answer) AS keyed,',
        "       count(q.id) FILTER (WHERE q.origin = 'PYQ') AS from_papers,",
        "       count(q.id) FILTER (WHERE q.origin = 'BANK') AS from_practice_bank,",
        "       count(q.id) FILTER (WHERE q.origin = 'IMPORTED') AS imported,",
        "       count(q.id) FILTER (WHERE q.origin = 'AI') AS ai_generated",
        '  FROM topics t LEFT JOIN questions q ON q.topic_id = t.id',
        ' GROUP BY t.id, t.section, t.chapter, t.name, t.position;',
    ]
    TOPIC_SQL.write_text('\n'.join(lines) + '\n')


def write_fix_sql(questions):
    """Idempotent: sets every PYQ question to its current text, figure flag and key.

    Keys an admin has corrected (answer_key_corrections) are left alone.
    """
    lines = [
        '-- Generated by backend/scripts/pyq/generate_pyq_bank.py. Do not edit by hand.',
        '-- Repairs maths lost in PDF text extraction (TeX between \\( and \\)), attaches set figures as',
        '-- [[img:/pyq/img/...]] markers, and fixes answer keys whose option mapping was wrong in V7.',
        '',
    ]
    for q, _ in questions:
        opts = "'" + json.dumps(q['options'], ensure_ascii=False).replace("'", "''") + "'::jsonb" if q['type'] == 'mcq' else 'NULL'
        lines.append(f"UPDATE questions SET topic = {lit(q['topic'])}, prompt = {lit(q['q'])}, passage = {lit(q['passage'])}, options = {opts}, "
                     f"explanation = {lit(q['explanation'])}, has_figure = {'TRUE' if q['figure'] else 'FALSE'} WHERE id = {lit(q['id'])};")
        lines.append(f"UPDATE questions SET answer = {lit(q['answer'])} WHERE id = {lit(q['id'])} "
                     f"AND NOT EXISTS (SELECT 1 FROM answer_key_corrections c WHERE c.question_id = {lit(q['id'])});")
    FIX_SQL.write_text('\n'.join(lines) + '\n')


if __name__ == '__main__':
    build()
