"""Build the text overlay that repairs maths and figures lost when the official PDFs were converted to text.

pdftotext drops radicals, superscripts and stacked fractions (e.g. "(a + 3)2" for (a+3)², "log3 x" for log₃x,
"\\ue020" for √) and cannot carry figures. 2IIM's pages carry the same official questions with TeX maths and the
set figures, so for every question already paired with a 2IIM question (see match_answer_keys.py):

  QA    the prompt and options are taken from 2IIM's TeX. Options keep the official order through the verified
        option map. When no option map exists because the stripped options were ambiguous or garbled, 2IIM's
        options are used in 2IIM's order together with 2IIM's key, since the key belongs to that order.
  DILR  2IIM's figures for the set are attached to the set text.

Every formula is checked with KaTeX (scripts/pyq/check_tex.mjs); anything that doesn't render is dropped and the
extracted text is kept instead.

Usage: python3 build_source_overlay.py <2iim.jsonl> <img_dir>
  writes resources/cat_pyq_answer_keys/source_overlay.jsonl and downloads figures into <img_dir>.
"""
import csv
import hashlib
import json
import pathlib
import re
import subprocess
import sys
import time
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[3]
BANK = ROOT / 'papers/extracted/cat_pyq_questions.jsonl'
KEYS = ROOT / 'resources/cat_pyq_answer_keys/answer_keys.csv'
OUT = ROOT / 'resources/cat_pyq_answer_keys/source_overlay.jsonl'
CHECK = pathlib.Path(__file__).with_name('check_tex.mjs')
UA = {'User-Agent': 'Mozilla/5.0 (CATalyst figure import)'}


def wrap_bare_tex(t):
    """Older pages leave TeX such as \\frac{5x+2}{3x-5} outside \\( \\); wrap each such expression."""
    out, last = [], 0
    for m in re.finditer(r'\\\(.+?\\\)', t, flags=re.S):
        out.append(('text', t[last:m.start()])); out.append(('math', m.group(0))); last = m.end()
    out.append(('text', t[last:]))

    def wrap(text):
        res, i = '', 0
        while True:
            m = re.search(r'\\[a-zA-Z]+', text[i:])
            if not m:
                return res + text[i:]
            start = i + m.start()
            j = i + m.end()
            # swallow following {...} groups and ^/_ scripts, keeping braces balanced
            while j < len(text) and (text[j] in '{^_' or (text[j] == ' ' and j + 1 < len(text) and text[j + 1] in '{^_')):
                if text[j] == ' ':
                    j += 1; continue
                if text[j] in '^_':
                    j += 1
                    if j < len(text) and text[j] != '{':
                        j += 1; continue
                depth = 0
                while j < len(text):
                    depth += text[j] == '{'
                    depth -= text[j] == '}'
                    j += 1
                    if depth == 0:
                        break
            res += text[i:start] + '\\(' + text[start:j] + '\\)'
            i = j
    return ''.join(wrap(x) if k == 'text' else x for k, x in out)


def drop_orphan_delimiters(t):
    r"""Remove a \) with no \( before it, or a \( that is never closed."""
    out, open_at = [], None
    for tok in re.split(r'(\\\(|\\\))', t):
        if tok == '\\(':
            if open_at is not None:
                continue  # nested opener: keep the first
            open_at = len(out)
            out.append(tok)
        elif tok == '\\)':
            if open_at is None:
                continue  # orphan closer
            open_at = None
            out.append(tok)
        else:
            out.append(tok)
    if open_at is not None:
        del out[open_at]
    return ''.join(out)


def tidy(t):
    r"""Normalise 2IIM's TeX: collapse hard wraps, undo doubled backslashes, delimit stray TeX with \( \)."""
    t = re.sub(r'[ \t]*\n[ \t]*(?!\[\[img)', ' ', t).strip()
    # older pages double the backslash before commands and delimiters; "\\" + space stays (array row breaks)
    prev = None
    while prev != t:
        prev, t = t, re.sub(r'\\\\(?=[a-zA-Z()\[\]])', r'\\', t)
    t = re.sub(r'\\\(\s*\\\)', '', t)
    # older pages close fractions with a stray ")": "\frac{5}{2})" -> "\frac{5}{2}"
    while t.count(')') > t.count('(') and '})' in t:
        t = t.replace('})', '}', 1)
    t = drop_orphan_delimiters(t)
    t = wrap_bare_tex(t)
    # words inside maths ("x ≤ 5/2 or x ≥ 7/2") should read as words, not italic variables
    t = re.sub(r'\\\((.+?)\\\)', lambda m: '\\(' + re.sub(r'\s(or|and)\s', r'\\text{ \1 }', m.group(1)) + '\\)', t)
    return t


def katex_ok(strings):
    """Indices of strings whose maths KaTeX renders and that have no TeX outside the delimiters."""
    res = subprocess.run(['node', str(CHECK)], input=json.dumps(strings), capture_output=True, text=True,
                         cwd=ROOT / 'frontend', check=True)
    return json.loads(res.stdout)


def fetch_image(url, img_dir):
    ext = pathlib.Path(url).suffix.lower() or '.png'
    name = hashlib.sha1(url.encode()).hexdigest()[:16] + ext
    path = img_dir / name
    if not path.exists():
        with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=60) as r:
            data = r.read()
        if not data[:4] in (b'\x89PNG', b'GIF8') and data[:2] != b'\xff\xd8':
            raise ValueError(f'not an image: {url}')
        path.write_bytes(data)
        time.sleep(1.0)
    return '/pyq/img/' + name


def main(src_path, img_dir):
    img_dir = pathlib.Path(img_dir)
    img_dir.mkdir(parents=True, exist_ok=True)
    bank = {q['id']: q for q in map(json.loads, BANK.read_text().splitlines())}
    src = {(s['file'], s['index']): s for s in map(json.loads, pathlib.Path(src_path).read_text().splitlines())}
    rows = list(csv.DictReader(KEYS.open()))

    def words(t):
        t = re.split(r'Type in the answer', re.sub(r'\\[a-zA-Z]+|\\[()]', ' ', t))[0]
        return set(re.findall(r'[a-z]{3,}', t.lower()))

    def jaccard(a, b):
        return len(a & b) / max(1, len(a | b))

    # QA questions whose maths was too garbled to pass the key matcher are paired on their words alone:
    # the matcher's one-to-one candidate is kept when the non-maths words agree.
    own = {i: words(re.split(r'Q\.\s*\d+\s', q['question_text'])[-1]) for i, q in bank.items()}
    cands = {}
    for r in rows:
        if r['section'] == 'QA' and r['source_index']:
            s = src[(r['source'].rstrip('/').rsplit('/', 1)[1] + '.html', int(r['source_index']))]
            cands[r['id']] = (s, jaccard(own[r['id']], words(s['prompt'])))

    def best_by_words(r, s):
        """True when this question and this source are each other's closest match by words in the section."""
        peers = [x for x in rows if x['section'] == 'QA' and x['year'] == r['year'] and x['slot'] == r['slot']]
        sw = words(s['prompt'])
        mine = max(peers, key=lambda x: jaccard(own[x['id']], sw))['id']
        return mine == r['id']

    def same_option_numbers(a, b):
        """MCQ options carry the same numbers (e.g. 25/151, 1/2, 1/4, 111/55), in any order."""
        sig = lambda opts: sorted(tuple(re.findall(r'\d+', o)) for o in opts)
        return len(a) == len(b) == 4 and sig(a) == sig(b) and len({x for o in a for x in re.findall(r'\d+', o)}) >= 4

    entries = []
    for r in rows:
        if r['section'] == 'VARC':
            continue
        text_paired = False
        if r['paired'] != 'True':
            if r['id'] not in cands:
                continue
            s, j = cands[r['id']]
            if not (j >= 0.6 or (j >= 0.3 and best_by_words(r, s)) or same_option_numbers(bank[r['id']]['options'], s['options'])):
                continue
            text_paired = True
        s = src[(r['source'].rstrip('/').rsplit('/', 1)[1] + '.html', int(r['source_index']))]
        q = bank[r['id']]
        e = {'id': r['id']}
        images = re.findall(r'\[\[img:([^\]]+)\]\]', s['rich_prompt'])
        if images:
            e['images'] = images
        if r['section'] == 'QA':
            prompt = tidy(re.sub(r'\[\[img:[^\]]+\]\]', '', s['rich_prompt']))
            prompt = re.sub(r'\s*\(TITA\)\s*$', '', prompt)
            e['prompt'] = prompt
            if q['type'] == 'MCQ' and len(s['rich_options']) == 4:
                opts = [tidy(o) for o in s['rich_options']]
                if r['option_map']:
                    m = json.loads(r['option_map'])
                    e['options'] = [opts[m[o]] for o in range(4)]
                elif (text_paired or r['status'] in ('ambiguous_options', 'option_set_mismatch', 'option_text_mismatch')) and 'answer_choice' in s:
                    e['options'] = opts
                    e['answer'] = str(s['answer_choice'])
                    e['options_from_source'] = True
            if text_paired and q['type'] == 'TITA':
                lit = s.get('answer_literal') or s.get('answer_text')
                lit = re.sub(r'[,\s]', '', lit or '')
                if re.fullmatch(r'-?\d+(\.\d+)?', lit):
                    e['answer'] = lit
        if len(e) > 1:
            entries.append(e)

    # validate all maths in one KaTeX run; drop what doesn't render
    strings, where = [], []
    for i, e in enumerate(entries):
        for k in ('prompt',):
            if k in e:
                strings.append(e[k]); where.append((i, k, None))
        for j, o in enumerate(e.get('options', [])):
            strings.append(o); where.append((i, 'options', j))
    ok = set(katex_ok(strings))
    bad_prompt, bad_opts = set(), set()
    for n, (i, k, j) in enumerate(where):
        if n not in ok:
            (bad_prompt if k == 'prompt' else bad_opts).add(i)
    for i in bad_prompt:
        entries[i].pop('prompt', None)
    for i in bad_opts:
        for k in ('options', 'answer', 'options_from_source'):
            entries[i].pop(k, None)

    # figures: download once, reference locally
    failed = 0
    for e in entries:
        if 'images' in e:
            local = []
            for u in e['images']:
                try:
                    local.append(fetch_image(u, img_dir))
                except Exception as ex:  # a missing figure must not block the rest
                    print('image failed', u, ex, file=sys.stderr); failed += 1
            e['images'] = local
            if not local:
                del e['images']
    entries = [e for e in entries if len(e) > 1]
    OUT.write_text('\n'.join(json.dumps(e, ensure_ascii=False) for e in entries) + '\n')
    print(f"{len(entries)} overlay entries; prompts {sum('prompt' in e for e in entries)}, "
          f"options {sum('options' in e for e in entries)} (from source order: {sum(bool(e.get('options_from_source')) for e in entries)}), "
          f"with figures {sum('images' in e for e in entries)}; rejected by KaTeX: prompts {len(bad_prompt)}, options {len(bad_opts)}; "
          f"image failures {failed}")


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
