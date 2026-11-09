"""Download 2IIM's public CAT previous-year paper pages (one per slot and section) for answer-key extraction.

Usage: python3 fetch_2iim.py <out_dir>
Requests are sequential with a pause between them; pages already on disk are skipped.
"""
import pathlib, re, sys, time, urllib.request

INDEX = 'https://online.2iim.com/CAT-question-paper/'
UA = {'User-Agent': 'Mozilla/5.0 (CATalyst answer-key import)'}

def get(url):
    with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=60) as r:
        return r.read().decode('utf-8', 'replace')

def main(out):
    out = pathlib.Path(out); out.mkdir(parents=True, exist_ok=True)
    urls = sorted(set(re.findall(r'https://online\.2iim\.com/CAT-question-paper/CAT-20\d\d-Question-Paper-Slot-\d-(?:DILR|Quant|VARC)/', get(INDEX))))
    for u in urls:
        f = out / (u.rstrip('/').rsplit('/', 1)[1] + '.html')
        if f.exists() and f.stat().st_size:
            continue
        f.write_text(get(u)); print('fetched', f.name); time.sleep(1.5)
    print(len(urls), 'pages in', out)

if __name__ == '__main__':
    main(sys.argv[1])
