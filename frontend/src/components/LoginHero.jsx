import { useEffect, useState } from 'react';

/** CAT is held on the last Sunday on or before 30 November (same rule as the server's CatExamCalendar). */
function examDate(year) {
  const d = new Date(Date.UTC(year, 10, 30));
  d.setUTCDate(30 - d.getUTCDay());
  return d;
}

/** Days until the next CAT; rolls over to next year once exam day has passed (dates compared in IST). */
function countdown(now) {
  const istToday = new Date(now.getTime() + 5.5 * 3600e3);
  const today = Date.UTC(istToday.getUTCFullYear(), istToday.getUTCMonth(), istToday.getUTCDate());
  let exam = examDate(istToday.getUTCFullYear());
  if (exam.getTime() < today) exam = examDate(istToday.getUTCFullYear() + 1);
  return { year: exam.getUTCFullYear(), days: Math.round((exam.getTime() - today) / 864e5) };
}

function ExamCountdown() {
  const [now, setNow] = useState(() => new Date());
  useEffect(() => { const t = setInterval(() => setNow(new Date()), 60_000); return () => clearInterval(t); }, []);
  const { year, days } = countdown(now);
  return (
    <div className="hero-count">
      <i aria-hidden="true" />
      CAT {year} · {days === 0 ? 'today' : `${days} ${days === 1 ? 'day' : 'days'} to go`}
    </div>
  );
}

export default function LoginHero() {
  return (
    <section className="auth-hero">
      <img className="hero-photo" src="/brand/hero-photo.jpg" alt="" aria-hidden="true" />
      <div className="brand" style={{ padding: 0 }}><img className="brand-logo" src="/brand/99ile-on-dark.png" alt="99ile" /></div>

      <div className="hero-main">
        <ExamCountdown />
        <h1>Just <span className="hero-mark">do it.</span></h1>
        <p>
          Full-length mocks in the exam's own interface, revision that brings back the questions you got wrong,
          and a syllabus map that shows exactly what's left.
        </p>
      </div>

      <ul className="hero-facts">
        <li><b className="num">68</b>questions</li>
        <li><b className="num">3</b>sections · 40 min each</li>
        <li><b className="num">+3 / −1</b>marking</li>
      </ul>
    </section>
  );
}
