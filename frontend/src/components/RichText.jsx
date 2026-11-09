import { Fragment, useMemo } from 'react';
import katex from 'katex';
import 'katex/dist/katex.min.css';

// Question text can carry three things plain text can't:
//   - inline maths between \( and \), or display maths between \[ \] / $$ $$ (rendered with KaTeX)
//   - figures as [[img:/pyq/img/<file>]] markers (only local paper figures are allowed)
//   - tables kept from the paper's layout: runs of lines with wide gaps, shown monospaced so columns line up
const MATH = /\\\((.+?)\\\)|\\\[(.+?)\\\]|\$\$(.+?)\$\$/gs;
const IMG = /^\[\[img:(\/pyq\/img\/[\w.-]+)\]\]$/;
const TABLE_ROW = /\S {2,}\S.* {2,}\S/;
const HAS_MATH = /\\\(|\\\[|\$\$/;

function renderMath(tex, display) {
  // KaTeX escapes its input and, with trust off, renders no links or raw HTML.
  return katex.renderToString(tex, { displayMode: display, throwOnError: false, strict: 'ignore', output: 'html' });
}

function Inline({ text }) {
  const parts = [];
  let last = 0;
  for (const m of text.matchAll(MATH)) {
    if (m.index > last) parts.push(text.slice(last, m.index));
    const display = m[1] === undefined;
    parts.push(
      <span key={m.index} className={display ? 'rt-math-block' : 'rt-math'}
        dangerouslySetInnerHTML={{ __html: renderMath(m[1] ?? m[2] ?? m[3], display) }} />,
    );
    last = m.index + m[0].length;
  }
  if (last < text.length) parts.push(text.slice(last));
  return parts.map((p, i) => (typeof p === 'string' ? <Fragment key={`t${i}`}>{p}</Fragment> : p));
}

/** Splits text into paragraphs, monospaced table blocks and figures. */
function blocks(text) {
  const out = [];
  for (const line of text.split('\n')) {
    const img = line.trim().match(IMG);
    const kind = img ? 'img' : TABLE_ROW.test(line) && !HAS_MATH.test(line) ? 'table' : 'text';
    const prev = out[out.length - 1];
    if (img) out.push({ kind, src: img[1] });
    else if (prev && prev.kind === kind) prev.lines.push(line);
    else out.push({ kind, lines: [line] });
  }
  return out;
}

export default function RichText({ text, as: Tag = 'div', className = '' }) {
  const parts = useMemo(() => blocks(text || ''), [text]);
  if (!text) return null;
  // plain one-liners (most options) need no block structure
  if (parts.length === 1 && parts[0].kind === 'text') {
    return <Tag className={className}><Inline text={text} /></Tag>;
  }
  return (
    <Tag className={className}>
      {parts.map((b, i) => {
        if (b.kind === 'img') return <img key={i} className="rt-figure" src={b.src} alt="Figure from the question paper" loading="lazy" />;
        if (b.kind === 'table') return <pre key={i} className="rt-table">{b.lines.join('\n')}</pre>;
        return <Fragment key={i}><Inline text={b.lines.join('\n')} />{i < parts.length - 1 ? '\n' : ''}</Fragment>;
      })}
    </Tag>
  );
}
