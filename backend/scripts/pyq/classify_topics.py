"""Assign every official-paper question to exactly one syllabus topic (the `topics` table).

QA     2IIM's per-question chapter heading (when present) narrows the candidate topics; weighted keywords in the
       question then pick the topic. Without a heading, keywords choose among all QA topics.
DILR   the set text decides, by weighted keywords, and every question in a set gets the set's topic.
VARC   verbal-ability questions by their instruction (odd one out, para jumble, summary, sentence placement);
       reading-comprehension questions by what the question asks (inference, main idea, tone, ...), falling back
       to the passage's genre.

Usage: python3 classify_topics.py <2iim.jsonl>
  writes resources/cat_pyq_answer_keys/question_topics.csv (id, section, topic_id, topic, method)
"""
import collections
import csv
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[3]
BANK = ROOT / 'papers/extracted/cat_pyq_questions.jsonl'
KEYS = ROOT / 'resources/cat_pyq_answer_keys/answer_keys.csv'
SYLLABUS = ROOT / 'frontend/src/data/syllabus.js'
OUT = ROOT / 'resources/cat_pyq_answer_keys/question_topics.csv'


def syllabus():
    """(section, chapter, name, id) for every topic, read from the frontend's syllabus (the topics table's source)."""
    text = SYLLABUS.read_text()
    out = []
    for sec_block in re.finditer(r"section: '(\w+)',\s*chapters: \[(.*?)\]\s*,\s*\}", text, re.S):
        section = sec_block[1]
        for ch in re.finditer(r"\{ name: '([^']+)', topics: \[([^\]]*)\]", sec_block[2]):
            for name in re.findall(r"'([^']+)'", ch[2]):
                tid = re.sub(r'[^a-z0-9:]+', '-', f'{section}::{ch[1]}::{name}'.lower())
                out.append((section, ch[1], name, tid))
    return out


TOPICS = syllabus()
BY_NAME = {(s, n): (c, i) for s, c, n, i in TOPICS}

# ---------------------------------------------------------------- QA

QA_KEYWORDS = {
    'Percentages': [(r'\bper ?cent|%', 2), (r'increase[sd]? by|decrease[sd]? by', 1)],
    'Profit, Loss & Discount': [(r'profit|loss\b|discount|cost price|selling price|marked price|sells?\b|sold\b|bought|purchase', 3)],
    'Simple & Compound Interest': [(r'interest|compound|principal|per annum|invested|investment|loan|borrow', 3)],
    'Ratio & Proportion': [(r'\bratio|proportion|in the ratio|:\s*\d', 2), (r'shares?\b|divided among', 1)],
    'Mixtures & Alligation': [(r'mixture|alloy|solution|alligation|concentration|milk|water|acid|ethanol|litre', 3)],
    'Time & Work': [(r'\bwork|job|pipe|tank|cistern|fill|empt|days to complete|efficien', 3)],
    'Time, Speed & Distance': [(r'speed|km|kilomet|train|boat|stream|race|distance|travel|runs?\b|ran\b|walk|car\b|cars\b|hour', 2)],
    'Averages': [(r'average|\bmean\b|arithmetic mean', 3)],
    'Linear equations': [(r'equation|solve|system of|integers? (x|y)|unique solution', 1)],
    'Quadratic equations': [(r'quadratic|roots?\b|x\^?\{?2|x²|\bx2\b|discriminant', 3)],
    'Inequalities & Modulus': [(r'\|\s*x|modulus|absolute value|inequalit|≤|≥|\\leq|\\geq|\\lt|\\gt|\\le\b|\\ge\b|maximum|minimum|largest|smallest|range of', 2)],
    'Functions & Graphs': [(r'f\s*\(|g\s*\(|function|graph of|domain|polynomial', 3)],
    'Logarithms': [(r'\blog|logarithm|\\log', 4)],
    'Progressions (AP/GP/HP)': [(r'arithmetic progression|geometric progression|\bA\.? ?P\b|\bG\.? ?P\b|\bH\.? ?P\b|terms? of the|common difference|common ratio|sum of the first', 3)],
    'Surds & Indices': [(r'\\sqrt|√|power|exponent|indices|\^\s*\{|\^\s*\d|surd', 2)],
    'Divisibility & Remainders': [(r'remainder|divisible|divided by|mod\b|modulo', 4)],
    'HCF & LCM': [(r'\bhcf\b|\blcm\b|\bgcd\b|greatest common|least common|highest common', 4)],
    'Factors': [(r'factors?\b|divisors?\b|\bprimes?\b|composite|perfect square', 5)],
    'Base systems': [(r'base \d|in base|binary|decimal system', 4)],
    'Unit & last digits': [(r'last digit|unit digit|units digit|last two digits|rightmost', 4), (r'\bdigits?\b', 1)],
    'Triangles': [(r'triangle|\\triangle|△|right.angled|isosceles|equilateral|hypotenuse|altitude|median', 3)],
    'Circles': [(r'circle|radius|chord|tangent|diameter|arc\b|semicircle|circumference|inscribed in a circle', 3)],
    'Polygons & Quadrilaterals': [(r'square\b|rectangle|polygon|hexagon|pentagon|quadrilateral|rhombus|trapez|parallelogram|regular \w+gon', 3)],
    'Coordinate geometry': [(r'coordinate|x-axis|y-axis|xy-plane|\(\s*-?\d+\s*,\s*-?\d+\s*\)|straight line|slope|\bline\b.*\bpoints?\b', 3)],
    '3D Mensuration': [(r'cone|cylinder|sphere|cube\b|cuboid|volume|hemisphere|prism|pyramid|surface area', 3)],
    'Trigonometry basics': [(r'\\sin|\\cos|\\tan|\bsin\b|\bcos\b|\btan\b|angle of elevation|degree', 3)],
    'Permutations & Combinations': [(r'ways|arrange|permutation|combination|how many (?:different )?(?:numbers|words|groups|teams)|selected|choose', 3)],
    'Probability': [(r'probab|chance|at random|randomly|likely', 4)],
    'Set theory': [(r'\bsets?\b|both|neither|only one of|venn|surveyed|students (?:like|play|study|chose)', 2)],
    'Sequences & Series': [(r'sequence|series|recurrence|x_\{?n|a_\{?n|t_\{?n|\bx1\b|\ba1\b|\.\.\.', 2)],
}

# 2IIM chapter headings -> allowed syllabus topics (first one is the default when no keyword fits)
QA_HEADINGS = [
    (r'geometry|cogeo|circle|trigonometry|mensuration', ['Triangles', 'Circles', 'Polygons & Quadrilaterals', 'Coordinate geometry', '3D Mensuration', 'Trigonometry basics']),
    (r'co-?ordinate|cogeo', ['Coordinate geometry']),
    (r'trigonometry', ['Trigonometry basics']),
    (r'^mensuration$', ['3D Mensuration', 'Polygons & Quadrilaterals', 'Circles', 'Triangles']),
    (r'function|polynomial', ['Functions & Graphs', 'Quadratic equations', 'Inequalities & Modulus']),
    (r'^percent', ['Percentages', 'Profit, Loss & Discount', 'Simple & Compound Interest']),
    (r'percents profits sici', ['Percentages', 'Profit, Loss & Discount', 'Simple & Compound Interest']),
    (r'profit', ['Profit, Loss & Discount']),
    (r'si & ci|simple interest|compound', ['Simple & Compound Interest']),
    (r'number theory|remainder|digits', ['Divisibility & Remainders', 'Factors', 'HCF & LCM', 'Unit & last digits', 'Base systems', 'Linear equations']),
    (r'progression|sequence|series', ['Progressions (AP/GP/HP)', 'Sequences & Series']),
    (r'linear|quadratic|algebra', ['Linear equations', 'Quadratic equations', 'Inequalities & Modulus', 'Functions & Graphs']),
    (r'inequalit', ['Inequalities & Modulus']),
    (r'average', ['Averages']),
    (r'ratios mixtures averages', ['Ratio & Proportion', 'Mixtures & Alligation', 'Averages']),
    (r'mixture|alligation', ['Mixtures & Alligation', 'Ratio & Proportion']),
    (r'^ratio', ['Ratio & Proportion', 'Mixtures & Alligation']),
    (r'speed|races|distance', ['Time, Speed & Distance']),
    (r'work|pipes|cistern', ['Time & Work']),
    (r'logarithm|exponent|powers', ['Logarithms', 'Surds & Indices']),
    (r'set theory', ['Set theory']),
    (r'permutation|combinatorics', ['Permutations & Combinations', 'Probability']),
]


def score(text, table, allowed):
    t = text.lower()
    best, best_s = None, 0
    for name in allowed:
        s = sum(w * len(re.findall(p, t, re.I)) for p, w in table.get(name, []))
        if s > best_s:
            best, best_s = name, s
    return best


def classify_qa(text, heading):
    h = re.sub(r'^CAT 20\d\d (Question Paper )?Slot ?\d ?-? ?(QA ?-? ?)?', '', heading or '').strip(' -–').lower()
    allowed = None
    if h:
        for part in re.split(r'\s*\|\s*', h):
            for pat, topics in QA_HEADINGS:
                if re.search(pat, part):
                    allowed = (allowed or []) + [t for t in topics if t not in (allowed or [])]
                    break
    if allowed:
        return score(text, QA_KEYWORDS, allowed) or allowed[0], '2iim-chapter+keywords'
    return score(text, QA_KEYWORDS, list(QA_KEYWORDS)) or 'Linear equations', 'keywords'

# ---------------------------------------------------------------- DILR

DILR_KEYWORDS = {
    'Games & Tournaments': [(r'tournament|match(?:es)?\b|played|won\b|lost\b|draw|league|round.robin|knock.?out|points? table|teams?\b|player', 2)],
    'Bar & Line graphs': [(r'graph|bar chart|line chart|plotted|the chart|figure below|axis', 3)],
    'Pie charts': [(r'pie', 5)],
    'Venn diagrams': [(r'venn|exactly two of the|none of the three|overlap', 3)],
    'Arrangements (linear/circular)': [(r'seated|sitting|\bsit\b|row of|circular|around a (?:round )?table|facing|to the (?:immediate )?(?:left|right)|adjacent|floor|grid|arranged in', 2)],
    'Scheduling & Calendars': [(r'schedul|slot|monday|tuesday|wednesday|thursday|friday|saturday|sunday|\bweek|calendar|a\.?m\.?\b|p\.?m\.?\b|hours?\b|minutes?\b|timetable|deadline', 1)],
    'Networks & Routes': [(r'route|\broads?\b|flights?\b|network|\bpaths?\b|stations?\b|connected|airline|\bhubs?\b|\bdirect(?:ly)? (?:to|from)', 2)],
    'Cubes & Dice': [(r'\bcube|dice|\bdie\b|faces?\b painted', 4)],
    'Binary logic (truth/lie)': [(r'truth|lie|liar|truthful|always tells', 4)],
    'Selections & Distributions': [(r'select|committee|distribut|allocat|assigned|chose|chosen', 2), (r'groups?\b|each of the', 1)],
    'Quant-based LR': [(r'\bvotes?\b|scored?\b|scores\b|marks\b|ratings?\b|points? (?:each|per)', 1)],
    'Data sufficiency': [(r'sufficient|data sufficiency', 5)],
    'Tables & Caselets': [(r'\btables?\b|columns?\b|rows?\b|given below|the following data|data on|number of', 2)],
}

# ---------------------------------------------------------------- VARC

def classify_varc(prompt, passage):
    p = prompt.lower()
    if re.search(r'odd (?:one|sentence)|odd one out|does not fit|doesn.t fit', p):
        return 'Odd sentence out'
    if re.search(r'(?:proper|logical|coherent) (?:sequenc|order)|sequence of|key in the sequence|paragraph jumble|jumbled', p) and 'odd' not in p:
        return 'Para jumbles'
    if re.search(r'summar|essence of the passage|captures the essence', p):
        return 'Para summary'
    if re.search(r'(?:sentence|option)\s*\(?\d?\)?.{0,60}(?:blank|best (?:fit|placed)|missing)|where .{0,40}sentence|blank\s*\(\d\)|____\(\d\)', p):
        return 'Sentence placement'
    if re.search(r'tone|attitude|author.s (?:view|stance|position) (?:on|towards)', p):
        return 'Tone & Attitude'
    if re.search(r'weaken|strengthen|undermin|support(?:s|ing)? the (?:author|argument|claim)|supporting|invalidat|undercut|cast doubt|challenge|refute|bolster|contradict|if true', p):
        return 'Strengthen / Weaken'
    if re.search(r'main (?:idea|point|argument)|central (?:idea|theme|argument|concern)|primary (?:purpose|concern)|best title|title for|best (?:describes|captures|summarises|summarizes|sums up)|sums? up|key arguments|purpose of the passage|passage is (?:mainly|primarily)|overall argument', p):
        return 'Main idea / Title'
    if re.search(r'infer|impl(?:y|ies|ied)|suggest|assum|except|not consistent|least likely|most likely|would agree|would disagree|can be concluded', p):
        return 'Inference questions'
    if re.search(r'according to the passage|the author (?:mentions|states|refers|cites|uses|provides)|which of the following is (?:true|correct|not true|false)|in the passage, the|the word|the phrase|examples? of|in paragraph|refers to', p):
        return 'Specific detail'
    t = (passage or '').lower()
    genre = {'Philosophy passages': len(re.findall(r'philosoph|ethic|moral|metaphys|kant|plato|consciousness|truth|reason', t)),
             'Science & Tech passages': len(re.findall(r'scien|biolog|species|evolution|research|technolog|brain|cell|climate|physic|data|algorithm', t)),
             'Economics passages': len(re.findall(r'econom|market|capital|trade|price|growth|inflation|labou?r|firms?|wealth|poverty', t))}
    g, n = max(genre.items(), key=lambda kv: kv[1])
    return g if n >= 4 else 'Inference questions'


def set_key(q):
    t = re.split(r'\nQ\.\s*\d+\s', q['question_text'])[0]
    return re.sub(r'\s+', ' ', t)[:300]


def main(src_path):
    bank = [json.loads(l) for l in BANK.read_text().splitlines() if l.strip()]
    # cleaned prompts (passage split off, options removed) from the generated paper files
    clean = {q['id']: q for f in sorted((ROOT / 'frontend/public/pyq').glob('*.json')) for q in json.loads(f.read_text())['questions']}
    keys = {r['id']: r for r in csv.DictReader(KEYS.open())}
    src = {(s['file'], s['index']): s for s in map(json.loads, pathlib.Path(src_path).read_text().splitlines())}

    def heading(qid):
        r = keys.get(qid)
        if not r or not r['source_index']:
            return ''
        return src[(r['source'].rstrip('/').rsplit('/', 1)[1] + '.html', int(r['source_index']))].get('heading', '')

    def ask(q):
        t = re.split(r'Type in the answer', q['question_text'])[0]
        return t + ' ' + ' '.join(q['options'])

    rows = []
    # DILR: one topic per set (questions sharing their opening text)
    dilr_sets = collections.defaultdict(list)
    for q in bank:
        if q['section'] == 'DILR':
            dilr_sets[(q['paper_id'], set_key(q))].append(q)
    dilr_topic = {}
    for (_, _), qs in dilr_sets.items():
        text = ' '.join(ask(q) for q in qs)
        name = score(text, DILR_KEYWORDS, list(DILR_KEYWORDS)) or 'Tables & Caselets'
        for q in qs:
            dilr_topic[q['id']] = name

    for q in bank:
        if q['section'] == 'QA':
            c = clean[q['id']]
            name, method = classify_qa(c['q'] + ' ' + ' '.join(c['options']), heading(q['id']))
        elif q['section'] == 'DILR':
            name, method = dilr_topic[q['id']], 'set-keywords'
        else:
            c = clean[q['id']]
            # VA instructions sit at the start of a standalone prompt; an RC question's ask is the end of its prompt
            prompt = c['q'] if not c['passage'] else c['q'][-400:]
            name, method = classify_varc(re.sub(r'\\\(|\\\)', '', prompt), c['passage'] or ''), 'question-type'
        chapter, tid = BY_NAME[(q['section'], name)]
        rows.append({'id': q['id'], 'section': q['section'], 'chapter': chapter, 'topic': name, 'topic_id': tid, 'method': method})

    with OUT.open('w', newline='') as f:
        w = csv.DictWriter(f, fieldnames=list(rows[0]))
        w.writeheader()
        w.writerows(rows)
    by = collections.Counter((r['section'], r['topic']) for r in rows)
    for (s, n), c in sorted(by.items()):
        print(f'{s:5} {c:4}  {n}')
    print(len(rows), 'questions;', len(by), 'topics used of', len(TOPICS))


if __name__ == '__main__':
    main(sys.argv[1])
