// Reads a JSON array of strings on stdin; prints the indices of those whose maths renders with KaTeX.
// Run from frontend/ so the app's own katex package is used: node ../backend/scripts/pyq/check_tex.mjs
import { createRequire } from 'node:module';

// resolve katex from the working directory (frontend/), not from this script's folder
const katex = createRequire(`${process.cwd()}/`)('katex');

const input = JSON.parse(await new Promise((resolve) => {
  let s = '';
  process.stdin.on('data', (d) => { s += d; });
  process.stdin.on('end', () => resolve(s));
}));

const MATH = /\\\((.+?)\\\)|\$\$(.+?)\$\$|\\\[(.+?)\\\]/gs;

function ok(text) {
  let rest = '';
  let last = 0;
  for (const m of text.matchAll(MATH)) {
    rest += text.slice(last, m.index);
    last = m.index + m[0].length;
    try {
      katex.renderToString(m[1] ?? m[2] ?? m[3], { throwOnError: true, strict: 'ignore' });
    } catch {
      return false;
    }
  }
  rest += text.slice(last);
  // TeX left outside the delimiters would show up raw
  return !/\\[a-zA-Z]+|\\\(|\\\)|\$\$/.test(rest);
}

process.stdout.write(JSON.stringify(input.map((t, i) => (ok(t) ? i : -1)).filter((i) => i >= 0)));
