"""Build real question sets for the 24 sectional mocks (sec-VARC-1 ... sec-QA-8) and write a Flyway migration.

Input: a CSV export of the question bank (see the command below). Output: an SQL migration that sets each sectional
mock's question_ids, question counts and difficulty label.

    docker exec catalyst-db-1 psql -U catalyst -d catalyst -c "copy (select q.id, q.section, q.topic_id,
      coalesce(t.chapter,''), coalesce(t.name,''), coalesce(q.paper_id,''), coalesce(q.question_number,0), q.position,
      md5(coalesce(q.passage,'')), coalesce(q.passage,''), q.type, (q.answer is not null), q.origin
      from questions q left join topics t on t.id = q.topic_id) to stdout with csv" > questions.csv
    python3 backend/scripts/build_sectionals.py questions.csv backend/src/main/resources/db/migration/V18__sectional_mock_sets.sql

Rules:
- Only keyed questions, so every question in a sectional is scored.
- Passage sets (RC passages, DILR sets) are taken whole and never split.
- Each sectional leans on its theme (its subtitle), then is filled out like a real CAT section.
- No question is used by two sectionals of the same section.
- Deterministic: the same input always gives the same sets.
"""
import collections
import csv
import json
import random
import re
import sys

csv.field_size_limit(10 ** 9)
rows = list(csv.reader(open(sys.argv[1], newline='', encoding='utf-8')))
OUT = sys.argv[2]
rng = random.Random(99)

Q = collections.namedtuple('Q', 'id section topic chapter topic_name paper qnum position phash passage type keyed origin')
qs = [Q(r[0], r[1], r[2], r[3], r[4], r[5], int(r[6]), int(r[7]), r[8], r[9], r[10], r[11] == 't', r[12]) for r in rows]
qs = [q for q in qs if q.keyed]
year = lambda q: int(m.group(1)) if (m := re.match(r'pyq-(\d{4})-', q.paper)) else 2026


# ---- units: whole passage sets, and standalone questions
def units_of(section):
    sets = collections.defaultdict(list)
    singles = []
    for q in qs:
        if q.section != section:
            continue
        if q.passage.strip():
            sets[(q.paper, q.phash)].append(q)
        else:
            singles.append(q)
    out = []
    for key, members in sets.items():
        members.sort(key=lambda q: (q.qnum or 10 ** 6, q.position))
        out.append(members)
    out.sort(key=lambda u: u[0].id)
    singles.sort(key=lambda q: q.id)
    return out, singles


def topics_of(unit):
    return collections.Counter(q.topic for q in unit)


def reachable(target, sizes):
    """Some subset of sizes sums exactly to target."""
    can = 1  # bitset: bit k set when k is reachable
    for s in sizes:
        can |= can << s
    return bool((can >> target) & 1)


def pick_sets(target, preferred, others, used):
    """Whole sets adding up to exactly `target` questions (or as close as possible below it), preferred ones first."""
    chosen, need = [], target
    pools = [preferred, others]
    for pool in pools:
        for u in pool:
            if need <= 0:
                break
            if any(q.id in used for q in u) or len(u) > need:
                continue
            rest = [len(v) for p in pools for v in p if v is not u and v not in chosen and not any(q.id in used for q in v)]
            if need - len(u) == 0 or reachable(need - len(u), rest):
                chosen.append(u)
                used.update(q.id for q in u)
                need -= len(u)
    if need > 0:  # nothing adds up exactly: take the best fitting remaining sets
        for pool in pools:
            for u in sorted(pool, key=len):
                if need > 0 and len(u) <= need and not any(q.id in used for q in u):
                    chosen.append(u)
                    used.update(q.id for q in u)
                    need -= len(u)
    return [q for u in chosen for q in u], need


def shuffled(xs):
    xs = list(xs)
    rng.shuffle(xs)
    # newer papers first, with a random order among equals
    return sorted(xs, key=lambda u: -year(u[0] if isinstance(u, list) else u))


plans = {}

# ---------------- DILR: 22 questions of whole sets
DILR_THEMES = [
    ('sec-DILR-1', {'dilr::logical-reasoning::arrangements-linear-circular-', 'dilr::logical-reasoning::selections-distributions',
                    'dilr::logical-reasoning::binary-logic-truth-lie-'}),
    ('sec-DILR-2', {'dilr::data-interpretation::tables-caselets'}),
    ('sec-DILR-3', {'dilr::logical-reasoning::games-tournaments'}),
    ('sec-DILR-4', {'dilr::data-interpretation::bar-line-graphs', 'dilr::data-interpretation::pie-charts',
                    'dilr::data-interpretation::venn-diagrams'}),
    ('sec-DILR-5', {'dilr::logical-reasoning::networks-routes'}),
    ('sec-DILR-6', {'dilr::logical-reasoning::quant-based-lr'}),
    ('sec-DILR-8', {'dilr::logical-reasoning::scheduling-calendars'}),
    ('sec-DILR-7', set()),  # mixed: last, from what is left
]
sets, _ = units_of('DILR')
used = set()
for mock, theme in DILR_THEMES:
    pref = shuffled([u for u in sets if topics_of(u).most_common(1)[0][0] in theme]) if theme else []
    rest = shuffled([u for u in sets if u not in pref])
    picked, short = pick_sets(22, pref, rest, used)
    plans[mock] = picked

# ---------------- VARC: RC passages + verbal ability, 24 questions
ECON = re.compile(r'\b(econom|market|trade|inflation|capital|growth|fiscal|monetary|wage|labou?r|price|tax|bank|financ)', re.I)
PHIL = re.compile(r'\b(philosoph|moral|ethic|conscious|metaphys|truth|existen|kant|aristotle|plato|virtue|reason|meaning|knowledge|mind)', re.I)
SCI = re.compile(r'\b(scien|species|brain|evolution|climate|technolog|research|physic|cell|gene|biolog|ecolog|neuro|algorithm|data|digital|planet)', re.I)


def subject(unit):
    t = topics_of(unit)
    if t['varc::reading-comprehension::economics-passages']: return 'econ'
    if t['varc::reading-comprehension::philosophy-passages']: return 'phil'
    if t['varc::reading-comprehension::science-tech-passages']: return 'sci'
    text = unit[0].passage
    scores = {'econ': len(ECON.findall(text)), 'phil': len(PHIL.findall(text)), 'sci': len(SCI.findall(text))}
    best = max(scores, key=scores.get)
    return best if scores[best] >= 4 else 'other'


VA = {'pj': 'varc::verbal-ability::para-jumbles', 'ps': 'varc::verbal-ability::para-summary',
      'odd': 'varc::verbal-ability::odd-sentence-out', 'place': 'varc::verbal-ability::sentence-placement'}
VARC_THEMES = [  # mock, RC subject preference, RC target, verbal-ability topic mix
    ('sec-VARC-2', 'phil', 16, ['pj', 'ps', 'odd', 'place']),
    ('sec-VARC-4', 'econ', 16, ['pj', 'ps', 'odd', 'place']),
    ('sec-VARC-6', 'sci', 16, ['pj', 'ps', 'odd', 'place']),
    ('sec-VARC-3', None, 8, ['pj', 'ps']),
    ('sec-VARC-8', None, 8, ['odd', 'place']),
    ('sec-VARC-5', None, 8, ['pj', 'ps', 'odd', 'place']),
    ('sec-VARC-1', None, 16, ['pj', 'ps', 'odd', 'place']),
    ('sec-VARC-7', None, 16, ['pj', 'ps', 'odd', 'place']),
]
passages, singles = units_of('VARC')
va_by_topic = {k: shuffled([q for q in singles if q.topic == t]) for k, t in VA.items()}
used = set()
for mock, subj, rc_target, mix in VARC_THEMES:
    pref = shuffled([u for u in passages if subj and subject(u) == subj])
    rest = shuffled([u for u in passages if u not in pref])
    rc, _ = pick_sets(rc_target, pref, rest, used)
    va_need, va = 24 - len(rc), []
    i = 0
    while len(va) < va_need and any(any(q.id not in used for q in va_by_topic[k]) for k in mix):
        k = mix[i % len(mix)]
        nxt = next((q for q in va_by_topic[k] if q.id not in used), None)
        if nxt:
            va.append(nxt)
            used.add(nxt.id)
        i += 1
    plans[mock] = rc + va

# ---------------- QA: 22 standalone questions, about 30% TITA
_, qa = units_of('QA')
QA_THEMES = [  # mock, chapter quotas
    ('sec-QA-3', {'Number System': 22}),
    ('sec-QA-4', {'Geometry & Mensuration': 22}),
    ('sec-QA-6', {'Modern Maths': 22}),
    ('sec-QA-1', {'Arithmetic': 22}),
    ('sec-QA-5', {'Arithmetic': 22}),
    ('sec-QA-2', {'Algebra': 22}),
    ('sec-QA-7', {'Algebra': 22}),
    ('sec-QA-8', {'Arithmetic': 8, 'Algebra': 5, 'Number System': 3, 'Geometry & Mensuration': 4, 'Modern Maths': 2}),
]
used = set()
for mock, quotas in QA_THEMES:
    picked = []
    for chapter, n in quotas.items():
        pool = [q for q in shuffled(qa) if q.chapter == chapter and q.id not in used]
        by_topic = collections.defaultdict(list)
        for q in pool:
            by_topic[q.topic].append(q)
        tita_goal = round(n * 0.3)
        got, tita = [], 0
        while len(got) < n and any(by_topic.values()):
            for t in list(by_topic):  # round-robin over the chapter's topics
                if len(got) >= n or not by_topic[t]:
                    continue
                want_tita = tita < tita_goal and (n - len(got)) <= (tita_goal - tita) * 3
                cand = next((q for q in by_topic[t] if (q.type == 'TITA') == want_tita), by_topic[t][0])
                by_topic[t].remove(cand)
                got.append(cand)
                used.add(cand.id)
                tita += cand.type == 'TITA'
        picked += got
    plans[mock] = picked

# ---------------- write the migration
lines = ["-- Real question sets for the 24 sectional mocks, generated by backend/scripts/build_sectionals.py.",
         "-- Whole passage sets only, keyed questions only, no question shared by two sectionals of one section.", ""]
report = []
for mock in sorted(plans, key=lambda m: (m.split('-')[1], int(m.split('-')[2]))):
    ids = [q.id for q in plans[mock]]
    section = mock.split('-')[1]
    counts = {section: len(ids)}
    lines.append(f"UPDATE mocks SET question_ids = '{json.dumps(ids)}'::jsonb, question_count = {len(ids)}, "
                 f"section_question_counts = '{json.dumps(counts)}'::jsonb, difficulty = 'CAT-level' WHERE id = '{mock}';")
    tita = sum(q.type == 'TITA' for q in plans[mock])
    nsets = len({(q.paper, q.phash) for q in plans[mock] if q.passage.strip()})
    top = collections.Counter(q.topic_name for q in plans[mock]).most_common(2)
    report.append(f"{mock:12} {len(ids):2} qs  sets {nsets}  TITA {tita:2}  top: {top}")
open(OUT, 'w', encoding='utf-8').write('\n'.join(lines) + '\n')
print('\n'.join(report))
