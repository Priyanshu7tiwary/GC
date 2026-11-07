import { useStore } from '../lib/store.jsx';
import { SECTIONS } from '../data/syllabus.js';
import { LineChart } from '../components/Charts.jsx';

const fmtDate = (t) => new Date(t).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });

export default function Analytics() {
  const { attempts, targetPercentile, examDate, updateSettings, resetDemo } = useStore();
  const fulls = attempts.filter((a) => Object.keys(a.sections).length === 3);

  const pctTrend = fulls.map((a, i) => ({ label: `M${i + 1}`, tip: `${a.title} · ${fmtDate(a.date)}`, pct: Number((a.percentile || 0).toFixed(1)) }));
  const accTrend = fulls.map((a, i) => {
    const row = { label: `M${i + 1}`, tip: a.title };
    Object.entries(a.sections).forEach(([k, s]) => { row[k] = s.correct + s.wrong ? Math.round((s.correct / (s.correct + s.wrong)) * 100) : 0; });
    return row;
  });

  const secAvg = Object.keys(SECTIONS).map((k) => {
    const list = fulls.map((a) => a.sections[k]);
    const n = list.length || 1;
    const avg = (f) => list.reduce((t, s) => t + f(s), 0) / n;
    return {
      k,
      score: avg((s) => s.score),
      attempted: avg((s) => s.correct + s.wrong),
      acc: avg((s) => (s.correct + s.wrong ? s.correct / (s.correct + s.wrong) : 0)) * 100,
      neg: avg((s) => s.wrong),
    };
  });
  const weakest = [...secAvg].sort((a, b) => a.acc - b.acc)[0];

  return (
    <div className="page">
      <div className="page-head">
        <div><h1>Performance Analytics</h1><p>Trends across your {fulls.length} full-length mocks.</p></div>
      </div>

      <div className="grid g-2" style={{ marginBottom: 18 }}>
        <div className="card">
          <div className="card-head"><div><h2>Estimated percentile</h2><p>Target: {targetPercentile}+</p></div></div>
          <LineChart data={pctTrend} series={[{ key: 'pct', label: 'Percentile', color: 'var(--accent)' }]} yMax={100} />
        </div>
        <div className="card">
          <div className="card-head"><div><h2>Accuracy by section</h2><p>% of attempted questions answered correctly</p></div></div>
          <LineChart data={accTrend} series={Object.keys(SECTIONS).map((k) => ({ key: k, label: k, color: SECTIONS[k].color }))} yMax={100} format={(v) => `${v}%`} />
        </div>
      </div>

      <div className="grid g-main" style={{ marginBottom: 18 }}>
        <div className="card">
          <div className="card-head"><div><h2>Section averages</h2><p>Mean per full mock</p></div></div>
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>Section</th><th>Avg score</th><th>Avg attempted</th><th>Accuracy</th><th>Avg negatives</th></tr></thead>
              <tbody>
                {secAvg.map((s) => (
                  <tr key={s.k}>
                    <td style={{ fontWeight: 600 }}><i className="dot" style={{ background: SECTIONS[s.k].color, marginRight: 8 }} />{SECTIONS[s.k].name}</td>
                    <td className="num">{s.score.toFixed(1)}</td>
                    <td className="num">{s.attempted.toFixed(1)}</td>
                    <td className="num">{s.acc.toFixed(0)}%</td>
                    <td className="num">−{s.neg.toFixed(1)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {weakest && fulls.length > 0 && (
            <div className="chip warn" style={{ marginTop: 14, whiteSpace: 'normal', borderRadius: 10, padding: '10px 12px' }}>
              Insight: {weakest.k} has your lowest accuracy ({weakest.acc.toFixed(0)}%). Fewer, surer attempts there would cut ~{Math.round(weakest.neg)} negative marks per mock.
            </div>
          )}
        </div>

        <div className="card">
          <div className="card-head"><div><h2>Goals & settings</h2></div></div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            <label className="field">Exam date
              <input className="input" type="date" value={new Date(examDate - new Date(examDate).getTimezoneOffset() * 60000).toISOString().slice(0, 10)}
                onChange={(e) => e.target.value && updateSettings({ examDate: new Date(`${e.target.value}T00:00:00`).getTime(), targetYear: Number(e.target.value.slice(0, 4)) })} />
            </label>
            <label className="field">Target percentile
              <select className="input" value={targetPercentile} onChange={(e) => updateSettings({ targetPercentile: Number(e.target.value) })}>
                {[90, 95, 97, 98, 99, 99.5].map((p) => <option key={p} value={p}>{p}</option>)}
              </select>
            </label>
            <button className="btn" onClick={() => { if (confirm('Reset all progress to the demo data?')) resetDemo(); }}>Reset demo data</button>
          </div>
        </div>
      </div>
    </div>
  );
}
