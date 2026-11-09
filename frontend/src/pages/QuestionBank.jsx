import { useCallback, useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { api } from '../lib/api.js';
import { useAdminMode } from '../lib/adminMode.jsx';
import TopicBanks from '../components/TopicBanks.jsx';

// Admin page: what generated mocks can draw from, and a JSON import to add more questions.
const ORIGINS = [['PYQ', 'Official CAT papers'], ['BANK', 'Practice bank'], ['IMPORTED', 'Imported'], ['AI', 'AI (verified)']];
const SECTIONS = ['VARC', 'DILR', 'QA'];

const TEMPLATE = [
  {
    section: 'QA', type: 'MCQ', topic: 'Percentages', difficulty: 'EASY',
    prompt: 'A shirt marked at ₹800 is sold at a 15% discount. What is the selling price, in ₹?',
    options: ['640', '680', '700', '720'], answer: '1',
    explanation: '15% of 800 is 120, so the price is 800 − 120 = 680.',
  },
  {
    section: 'QA', type: 'TITA', topic: 'Quadratic equations', difficulty: 'MODERATE',
    prompt: 'If \\(x^{2} - 7x + 12 = 0\\), what is the sum of the squares of the roots?',
    answer: '25', explanation: 'The roots are 3 and 4, and 9 + 16 = 25.',
  },
  {
    section: 'DILR', type: 'TITA', topic: 'Arrangements (linear/circular)', difficulty: 'HARD',
    passage: 'Shared set text goes here. Questions with identical passage text stay together in generated mocks.',
    prompt: 'First question of the set …', answer: '3',
  },
];

function download(name, data) {
  const url = URL.createObjectURL(new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' }));
  const a = Object.assign(document.createElement('a'), { href: url, download: name });
  a.click();
  URL.revokeObjectURL(url);
}

export default function QuestionBank() {
  const { isAdmin } = useAdminMode();
  const [stats, setStats] = useState(null);
  const [text, setText] = useState('');
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const refresh = useCallback(() => api('/api/admin/questions/stats').then(setStats, (e) => setError(e.message)), []);
  useEffect(() => { if (isAdmin) refresh(); }, [isAdmin, refresh]);
  if (!isAdmin) return <Navigate to="/" replace />;

  const submit = async () => {
    setError(''); setResult(null);
    let rows;
    try {
      rows = JSON.parse(text);
      if (!Array.isArray(rows)) rows = [rows];
    } catch {
      setError('That isn’t valid JSON. Paste an array of questions (download the template to see the format).');
      return;
    }
    setBusy(true);
    try {
      const r = await api('/api/admin/questions/import', { method: 'POST', body: rows });
      setResult(r);
      if (r.added) { setText(''); refresh(); }
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  };

  const onFile = async (e) => {
    const file = e.target.files?.[0];
    if (file) setText(await file.text());
    e.target.value = '';
  };

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>Question bank</h1>
          <p>Every keyed question here can be drawn into an AI-generated mock (22 per section).</p>
        </div>
      </div>

      <div className="card" style={{ marginBottom: 18 }}>
        <div className="card-head"><div><h2>Available to the generator</h2><p>{stats ? `${stats.total.toLocaleString('en-IN')} keyed questions` : 'Loading…'}</p></div></div>
        {stats && (
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>Section</th>{ORIGINS.map(([k, l]) => <th key={k}>{l}</th>)}<th>Total</th></tr></thead>
              <tbody>
                {SECTIONS.map((s) => {
                  const row = stats.keyedBySectionAndOrigin[s] || {};
                  return (
                    <tr key={s}>
                      <td style={{ fontWeight: 600 }}>{s}</td>
                      {ORIGINS.map(([k]) => <td key={k} className="num">{row[k] || 0}</td>)}
                      <td className="num" style={{ fontWeight: 700 }}>{Object.values(row).reduce((a, b) => a + b, 0)}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="card" style={{ marginBottom: 18 }}>
        <div className="card-head">
          <div><h2>Banks by topic</h2><p>One bank per syllabus topic. Every question is labelled with its section, chapter and topic; move any that are misfiled.</p></div>
        </div>
        <TopicBanks refreshKey={stats?.total} />
      </div>

      <div className="card">
        <div className="card-head">
          <div><h2>Add questions</h2><p>Paste a JSON array or load a .json file. Every row is checked before anything is saved; exact duplicates are skipped.</p></div>
          <button className="btn sm" onClick={() => download('99ile-questions-template.json', TEMPLATE)}>Download template</button>
        </div>
        <ul className="small muted" style={{ marginTop: 0 }}>
          <li><b>section</b>: VARC, DILR or QA · <b>topic</b>: a syllabus topic name from that section, as listed under Banks by topic (e.g. "Percentages") · <b>type</b>: MCQ or TITA · <b>difficulty</b>: EASY, MODERATE or HARD (optional)</li>
          <li><b>MCQ</b>: exactly 4 <b>options</b>; <b>answer</b> is the option index, "0" to "3".</li>
          <li><b>TITA</b>: no options; <b>answer</b> is a number such as "42", "3.5" or a sequence like "2413".</li>
          <li><b>passage</b> (optional): shared RC or DILR set text. Maths can be written as TeX between \( and \).</li>
        </ul>
        <textarea className="input" rows={12} style={{ width: '100%', fontFamily: 'var(--f-mono, monospace)', fontSize: 12.5 }}
          placeholder='[{"section": "QA", "type": "TITA", "topic": "Averages", "prompt": "…", "answer": "12"}]'
          value={text} onChange={(e) => setText(e.target.value)} />
        <div style={{ display: 'flex', gap: 10, alignItems: 'center', marginTop: 12, flexWrap: 'wrap' }}>
          <button className="btn primary" disabled={busy || !text.trim()} onClick={submit}>{busy ? 'Importing…' : 'Validate & import'}</button>
          <label className="btn">Load .json file<input type="file" accept="application/json,.json" hidden onChange={onFile} /></label>
          {result && !result.errors.length && <span className="chip good">{result.added} added · {result.skippedDuplicates} duplicates skipped</span>}
        </div>
        {error && <p className="small" style={{ color: 'var(--bad)' }}>{error}</p>}
        {result?.errors?.length > 0 && (
          <div className="small" style={{ color: 'var(--bad)', marginTop: 10 }}>
            Nothing was saved. Fix these rows and try again:
            <ul>{result.errors.slice(0, 50).map((e) => <li key={e}>{e}</li>)}</ul>
          </div>
        )}
      </div>
    </div>
  );
}
