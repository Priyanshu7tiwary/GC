"""Parse downloaded 2IIM CAT paper pages into question blocks with answer keys.

Each question on a 2IIM paper page ends in a `<div class="group section">` block holding the choices and the
answer, which is shown either as a tooltip ("Choice C" / literal), a `.Cans` choice, or a `.Sans` paragraph.
The prompt is whatever text sits between the previous block and this one.
"""
import sys, re, html, json, pathlib

def clean(fragment):
    t = re.sub(r'<(script|style|noscript)\b.*?</\1>', ' ', fragment, flags=re.S | re.I)
    t = re.sub(r'<!--.*?-->', ' ', t, flags=re.S)
    t = re.sub(r'<br\s*/?>|</p>', '\n', t, flags=re.I)
    t = re.sub(r'<[^>]+>', ' ', t)
    t = html.unescape(t)
    t = re.sub(r'[ \t\r\f\v]+', ' ', t)
    return re.sub(r'\n\s*', '\n', t).strip()

CHROME_IMG = re.compile(r'joey|loading|piverb|logo|icon|whatsapp|banner|scholar|course|result|rajesh|wizako|/touch/', re.I)


def rich(fragment, page_url):
    """Like clean(), but keeps the maths: TeX stays as-is, <sup>/<sub> become TeX, and figures become [[img:URL]]."""
    t = re.sub(r'<(script|style|noscript)\b.*?</\1>', ' ', fragment, flags=re.S | re.I)
    t = re.sub(r'<!--.*?-->', ' ', t, flags=re.S)

    def img(m):
        src = m.group(1)
        if CHROME_IMG.search(src) or src.startswith(('http:', 'https:', '/')):
            return ' '
        return '\n[[img:' + page_url + src.lstrip('./') + ']]\n'
    t = re.sub(r'<img\b[^>]*?(?:data-src|src)=["\']?([^"\' >]+)[^>]*>', img, t, flags=re.I)
    for tag, op in (('sup', '^'), ('sub', '_')):
        t = re.sub(rf'<{tag}[^>]*>(.*?)</{tag}>',
                   lambda m: '\\({}' + op + '{' + html.unescape(re.sub(r'<[^>]+>', '', m.group(1))).strip() + '}\\)', t, flags=re.S | re.I)
    return clean(t)


BLOCK = re.compile(r'<div class=["\']?(?:group section|section group)["\']?\s*>')

def parse(path):
    s = pathlib.Path(path).read_text(errors='replace')
    m = re.match(r'CAT-(\d{4})-Question-Paper-Slot-(\d)-(DILR|Quant|VARC)', pathlib.Path(path).stem)
    year, slot, sec = int(m[1]), int(m[2]), {'Quant': 'QA', 'DILR': 'DILR', 'VARC': 'VARC'}[m[3]]
    page_url = f'https://online.2iim.com/CAT-question-paper/{pathlib.Path(path).stem}/'
    start = s.find('class="ques')
    start = s.find('>', start) + 1  # begin after the <ol class="ques ..."> tag itself
    starts = [b.start() for b in BLOCK.finditer(s) if b.start() > start]
    out, prev = [], start
    for i, b in enumerate(starts):
        nxt = starts[i + 1] if i + 1 < len(starts) else len(s)
        block = s[b:nxt]
        end_m = re.search(r'<hr\s*/?>|</li>\s*<li>\s*<h4|<h4', block)
        block = block[:end_m.start()] if end_m else block[:6000]
        lead = s[prev:b]
        prev = b + len(block)
        headings = [clean(h) for h in re.findall(r'<h4[^>]*>(.*?)</h4>', lead, flags=re.S)]
        heading = headings[-1] if headings else ''
        lead = re.sub(r'<h4[^>]*>.*?</h4>', ' ', lead, flags=re.S)
        prompt = clean(lead)
        choice_m = re.search(r'<ol class=["\']?choice[^>]*>(.*?)</ol>', block, re.S)
        lis = re.findall(r'<li([^>]*)>(.*?)</li>', choice_m[1], re.S) if choice_m else []
        options = [clean(t) for _, t in lis]
        rec = dict(year=year, slot=slot, section=sec, prompt=prompt, options=options, file=pathlib.Path(path).name,
                   index=len(out), heading=heading, rich_prompt=rich(lead, page_url), rich_options=[rich(t, page_url) for _, t in lis])
        tip = re.search(r'Correct\s*Answer\s*</button>\s*<span\s+class=["\']?tooltiptext["\']?\s*>(.*?)</span>', block, re.S | re.I)
        cans = [k for k, (attrs, _) in enumerate(lis) if re.search(r'class=["\']?Cans', attrs)]
        sans = re.search(r'class=["\']?Sans[^>]*>(.*?)</p>', block, re.S)
        if tip:
            a = clean(tip[1])
            ch = re.match(r'Choice\s*([A-Da-d1-5])\b\s*(.*)', a, re.S)
            if ch:
                c = ch[1].upper()
                rec['answer_choice'] = 'ABCDE'.index(c) if c in 'ABCDE' else int(c) - 1
                rec['answer_text'] = ch[2].strip()
            else:
                rec['answer_literal'] = a.split('\n')[0].strip()
        elif len(cans) == 1:
            rec['answer_choice'] = cans[0]
            rec['answer_text'] = options[cans[0]]
        elif sans:
            rec['answer_literal'] = re.sub(r'^Correct\s*Answer\s*:?\s*', '', clean(sans[1]), flags=re.I).strip()
        else:
            continue
        out.append(rec)
    return out

if __name__ == '__main__':
    allq = []
    for f in sorted(sys.argv[2:]):
        allq += parse(f)
    pathlib.Path(sys.argv[1]).write_text('\n'.join(json.dumps(r, ensure_ascii=False) for r in allq))
    print(len(allq))
