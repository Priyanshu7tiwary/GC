"""Align 2IIM keyed questions to the extracted official-paper questions and emit a verified answer-key CSV."""
import sys, re, json, csv, difflib, collections, itertools

ext = [json.loads(l) for l in open(sys.argv[1])]
# page furniture that pdftotext glued onto the last option ("Option 3 Options Type in the answer: ___")
for _e in ext:
    _e['options'] = [re.sub(r'\s*\bOptions\s*(?:Type in the answer:?\s*_*)?\s*$', '', o) for o in _e['options']]
src = [json.loads(l) for l in open(sys.argv[2])]
out_csv, report = sys.argv[3], sys.argv[4]

def norm(t):
    t = re.sub(r'\\\(|\\\)|\$\$|\\frac|\\times|\\sqrt|[{}^_\\]', ' ', t or '')
    return re.sub(r'[^a-z0-9]+', ' ', t.lower()).strip()

def toks(t): return norm(t).split()

def stem(q):
    """The extracted question's own sentence: text after the last Q.n marker, minus TITA boilerplate."""
    t = q['question_text']
    t = re.split(r'Type in the answer', t)[0]
    parts = re.split(r'Q\.\s*\d+\s', t)
    return parts[-1]

def ask(text, n=25):
    """The closing words of a question: its actual ask, after any shared passage or set text."""
    text = re.split(r'Type in the answer', text or '')[0]
    return [w for w in toks(text) if len(w) > 1][-n:]

def prompt_sim(e, s):
    a, b = ask(e['question_text']), ask(s['prompt'])
    if not a or not b: return 0
    n = min(len(a), len(b))
    return difflib.SequenceMatcher(None, a[-n:], b[-n:], autojunk=False).ratio()

def opt_sim(e, s):
    if not (e['options'] and s['options']): return None
    oa = [norm(o) for o in e['options']]; ob = [norm(o) for o in s['options']]
    return sum(max(difflib.SequenceMatcher(None, x, y).ratio() for y in ob) for x in oa) / len(oa)

def sim(e, s):
    p, o = prompt_sim(e, s), opt_sim(e, s)
    return p if o is None else 0.4 * p + 0.6 * o

groups = collections.defaultdict(list)
for s in src: groups[(s['year'], s['slot'], s['section'])].append(s)

def normalize_tita(v):
    """Reduce a keyed TITA answer to what a candidate would type: strip units, map A-E sentence labels to 1-5."""
    v = v.replace(',', '').strip()
    if re.fullmatch(r'[A-E]{1,5}', v):
        return ''.join(str('ABCDE'.index(c) + 1) for c in v)
    m = re.match(r'^(?:Rs\.?\s*)?(-?\d+(?:\.\d+)?)(?:\s+[A-Za-z]+\.?)*$', v)
    return m[1] if m else None

def map_mcq_key(official, source, k):
    """Carry the source's keyed option over to the official option list, or refuse when that isn't certain.

    Options are paired one-to-one by text. The pairing must be strong (every pair >= 0.8 similar), clearly better
    than any other pairing, and neither list may contain near-duplicate options: once maths is stripped from the
    text, options such as "sqrt(x), sqrt(y), sqrt(z) in AP" and "x, y, z in AP" become indistinguishable.
    Returns (answer, status, note, option_map) where option_map[official index] = source index.
    """
    r = lambda x, y: difflib.SequenceMatcher(None, norm(x), norm(y)).ratio()
    if len(source) != 4 or not 0 <= k < 4:
        return None, 'option_text_mismatch', '', None
    # exact pairing first: identical option texts, each unique within its list
    no, ns = [norm(o) for o in official], [norm(o) for o in source]
    if len(set(no)) == 4 and set(no) == set(ns):
        best = [ns.index(x) for x in no]
        note = '' if best == [0, 1, 2, 3] else f'option order differs on source (source idx {k})'
        return str(best.index(k)), 'verified_match', note, best
    for opts in (official, source):
        if any(r(opts[x], opts[y]) >= 0.9 for x in range(4) for y in range(x + 1, 4)):
            return None, 'ambiguous_options', 'near-identical options once maths is stripped', None
    if min(max(r(x, y) for y in source) for x in official) < 0.75:
        return None, 'option_set_mismatch', '', None
    sim = [[r(official[o], source[q]) for q in range(4)] for o in range(4)]
    perms = sorted(itertools.permutations(range(4)), key=lambda pm: -sum(sim[o][pm[o]] for o in range(4)))
    best, second = perms[0], perms[1]
    margin = sum(sim[o][best[o]] for o in range(4)) - sum(sim[o][second[o]] for o in range(4))
    if min(sim[o][best[o]] for o in range(4)) < 0.8 or margin < 0.15:
        return None, 'option_text_mismatch', '', None
    official_k = list(best).index(k)
    note = '' if list(best) == [0, 1, 2, 3] else f'option order differs on source (source idx {k})'
    return str(official_k), 'verified_match', note, list(best)


def accept(e, s, same_pos):
    p, o = prompt_sim(e, s), opt_sim(e, s)
    if o is not None:
        return o >= 0.85 or (o >= 0.6 and p >= 0.6) or (same_pos and o >= 0.5 and p >= 0.5)
    return p >= 0.75 or (same_pos and p >= 0.5)

rows, stats = [], collections.Counter()
for key in sorted({(e['year'], e['slot'], e['section']) for e in ext}):
    es = sorted([e for e in ext if (e['year'], e['slot'], e['section']) == key], key=lambda e: e['question_number'])
    ss = groups.get(key, [])
    # score every pair; prefer the same position when scores tie
    pairs = sorted(((sim(e, s) + (0.02 if i == j else 0), i, j) for i, e in enumerate(es) for j, s in enumerate(ss)), reverse=True)
    used_e, used_s, assign = set(), set(), {}
    for score, i, j in pairs:
        if i in used_e or j in used_s: continue
        used_e.add(i); used_s.add(j); assign[i] = (j, score)
    for i, e in enumerate(es):
        j, score = assign.get(i, (None, 0))
        s = ss[j] if j is not None else None
        answer, status, note, option_map = None, 'unmatched', '', None
        paired = s is not None and accept(e, s, i == j)
        if paired:
            if e['type'] == 'MCQ':
                if 'answer_choice' in s and len(e['options']) == 4:
                    answer, status, note, option_map = map_mcq_key(e['options'], s['options'], s['answer_choice'])
                elif 'answer_literal' in s and re.fullmatch(r'[1-4A-Da-d]', s['answer_literal'].strip()):
                    v = s['answer_literal'].strip().upper(); answer = str('ABCD'.index(v) if v in 'ABCD' else int(v) - 1); status = 'verified_match'
                elif s.get('answer_literal') and [k for k, o in enumerate(e['options']) if norm(o) == norm(s['answer_literal'])]:
                    answer, status = str([k for k, o in enumerate(e['options']) if norm(o) == norm(s['answer_literal'])][0]), 'verified_match'
                    note = 'source gave answer as option text'
                else:
                    status = 'type_mismatch'
            else:
                lit = s.get('answer_literal') or s.get('answer_text')
                if lit is None and 'answer_choice' in s and s['options']:
                    lit = s['options'][s['answer_choice']]
                lit = normalize_tita(lit) if lit else None
                if lit:
                    answer, status = lit, 'verified_match'
                else:
                    status = 'no_literal'
        stats[status] += 1
        rows.append(dict(id=e['id'], year=e['year'], slot=e['slot'], section=e['section'], question_number=e['question_number'],
                         type=e['type'], answer=answer, status=status, match_score=round(score, 3), prompt_sim=round(prompt_sim(e, s), 2) if s else 0, opt_sim=(round(opt_sim(e, s), 2) if s and opt_sim(e, s) is not None else ''), same_pos=(i == j),
                         source=f"https://online.2iim.com/CAT-question-paper/{s['file'][:-5]}/" if s else '', note=note,
                         paired=paired, source_index=s['index'] if s else '', option_map=json.dumps(option_map) if option_map else ''))

with open(out_csv, 'w', newline='') as f:
    w = csv.DictWriter(f, fieldnames=list(rows[0])); w.writeheader(); w.writerows(rows)
json.dump(dict(stats), open(report, 'w'))
print(stats)
low = [r for r in rows if r['status'] != 'verified_match']
print(len(low), 'low/unverified'); [print(r['id'], r['type'], r['status'], r['prompt_sim'], r['opt_sim'], r['same_pos']) for r in low]
print('verified but moved position:', sum(1 for r in rows if r['status']=='verified_match' and not r['same_pos']))
