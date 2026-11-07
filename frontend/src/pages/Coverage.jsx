import { useState } from 'react';
import { useStore, coverageStats } from '../lib/store.jsx';
import { SYLLABUS, SECTIONS, ALL_TOPICS, topicId } from '../data/syllabus.js';
import { Ring, SegBar } from '../components/Charts.jsx';
import { IconCheck } from '../components/Icons.jsx';

const STATUS = [
  { label: 'Not started', cls: '' },
  { label: 'In progress', cls: 'warn' },
  { label: 'Completed', cls: 'good' },
];

export default function Coverage() {
  const { coverage, setCoverage } = useStore();
  const [active, setActive] = useState('QA');
  const [statusFilter, setStatusFilter] = useState('all');
  const [open, setOpen] = useState({});
  const overall = coverageStats(coverage);
  const totalQs = Object.values(coverage).reduce((a, c) => a + (c.practiced || 0), 0);
  const sec = SYLLABUS.find((s) => s.section === active);

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>Syllabus Coverage</h1>
          <p>Track every CAT topic from first read to completion. Completed topics enter your revision schedule automatically.</p>
        </div>
      </div>

      <div className="grid g-4" style={{ marginBottom: 18 }}>
        <div className="card" style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <Ring value={overall.pct} />
          <div><div className="stat-label">Overall</div><div className="stat-sub">{overall.done} of {overall.total} topics done</div><div className="stat-sub">{totalQs.toLocaleString('en-IN')} questions practised</div></div>
        </div>
        {Object.values(SECTIONS).map((s) => {
          const st = coverageStats(coverage, ALL_TOPICS.filter((t) => t.section === s.id));
          return (
            <button key={s.id} className="card" onClick={() => setActive(s.id)}
              style={{ textAlign: 'left', display: 'flex', alignItems: 'center', gap: 16, outline: active === s.id ? `2px solid ${s.color}` : 'none', outlineOffset: -1 }}>
              <Ring value={st.pct} size={64} stroke={7} color={s.color} />
              <div style={{ minWidth: 0 }}>
                <div style={{ fontWeight: 600 }}>{s.short}</div>
                <div className="small faint">{st.done} done · {st.prog} in progress</div>
                <div className="small faint">{st.total - st.done - st.prog} not started</div>
              </div>
            </button>
          );
        })}
      </div>

      <div className="card">
        <div className="card-head" style={{ flexWrap: 'wrap' }}>
          <div>
            <h2 style={{ display: 'flex', alignItems: 'center', gap: 8 }}><i className="dot" style={{ background: SECTIONS[active].color }} />{SECTIONS[active].name}</h2>
            <p>Click a status to cycle it · log practised questions per topic</p>
          </div>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            <div className="tabs">
              {Object.keys(SECTIONS).map((s) => <button key={s} className={`tab ${active === s ? 'active' : ''}`} onClick={() => setActive(s)}>{s}</button>)}
            </div>
            <div className="tabs">
              {[['all', 'All'], ['0', 'Not started'], ['1', 'In progress'], ['2', 'Done']].map(([k, l]) => (
                <button key={k} className={`tab ${statusFilter === k ? 'active' : ''}`} onClick={() => setStatusFilter(k)}>{l}</button>
              ))}
            </div>
          </div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {sec.chapters.map((ch) => {
            const topics = ch.topics.map((t) => ({ id: topicId(active, ch.name, t), name: t }));
            const st = coverageStats(coverage, topics);
            const visible = topics.filter((t) => statusFilter === 'all' || String(coverage[t.id]?.status ?? 0) === statusFilter);
            const key = `${active}-${ch.name}`;
            const isOpen = open[key] ?? true;
            return (
              <div key={ch.name} style={{ border: '1px solid var(--border)', borderRadius: 12, overflow: 'hidden' }}>
                <button onClick={() => setOpen({ ...open, [key]: !isOpen })}
                  style={{ width: '100%', display: 'flex', alignItems: 'center', gap: 14, padding: '14px 16px', background: 'var(--surface-2)', border: 0, textAlign: 'left' }}>
                  <span style={{ transform: isOpen ? 'rotate(90deg)' : 'none', transition: '.15s', color: 'var(--text-3)' }}>▶</span>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontWeight: 600 }}>{ch.name}</div>
                    <div className="small faint">{st.done}/{st.total} complete</div>
                  </div>
                  <div style={{ width: 160 }} className="hide-sm">
                    <SegBar parts={[{ label: 'Done', value: st.done, color: SECTIONS[active].color }, { label: 'In progress', value: st.prog, color: 'var(--text-3)' }, { label: 'Not started', value: st.total - st.done - st.prog, color: 'transparent' }]} />
                  </div>
                  <b className="num" style={{ width: 44, textAlign: 'right' }}>{st.pct}%</b>
                </button>
                {isOpen && (
                  <div style={{ padding: '0 16px' }}>
                    <div className="list">
                      {visible.map((t) => {
                        const c = coverage[t.id] || { status: 0, practiced: 0 };
                        const S = STATUS[c.status];
                        return (
                          <div className="row" key={t.id} style={{ flexWrap: 'wrap' }}>
                            <button aria-label="Toggle complete" onClick={() => setCoverage(t.id, { status: c.status === 2 ? 0 : 2 })}
                              style={{ width: 22, height: 22, borderRadius: 6, border: `1.5px solid ${c.status === 2 ? 'var(--good)' : 'var(--border)'}`, background: c.status === 2 ? 'var(--good)' : 'transparent', color: '#fff', display: 'grid', placeItems: 'center', padding: 0, flex: 'none' }}>
                              {c.status === 2 && <IconCheck width="14" />}
                            </button>
                            <div className="grow" style={{ minWidth: 160 }}>
                              <div style={{ fontWeight: 500, textDecoration: c.status === 2 ? 'none' : 'none' }}>{t.name}</div>
                              <div className="small faint">{c.practiced} questions practised</div>
                            </div>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                              <button className="btn sm ghost" onClick={() => setCoverage(t.id, { practiced: Math.max(0, c.practiced - 10) })} aria-label="Remove 10">−10</button>
                              <button className="btn sm" onClick={() => setCoverage(t.id, { practiced: c.practiced + 10, status: Math.max(c.status, 1) })}>+10 Qs</button>
                            </div>
                            <button className={`chip ${S.cls}`} style={{ border: 0, minWidth: 96, justifyContent: 'center' }}
                              onClick={() => setCoverage(t.id, { status: (c.status + 1) % 3 })} title="Click to change status">
                              {S.label}
                            </button>
                          </div>
                        );
                      })}
                      {!visible.length && <p className="small faint" style={{ padding: '12px 0', margin: 0 }}>No topics with this status.</p>}
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
