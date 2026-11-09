import { useEffect, useRef, useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../lib/auth.jsx';
import LoginHero from '../components/LoginHero.jsx';

const GIS_SRC = 'https://accounts.google.com/gsi/client';

/** Loads Google Identity Services once and renders the official "Sign in with Google" button. */
function GoogleButton({ clientId, onCredential, onError }) {
  const ref = useRef(null);
  useEffect(() => {
    if (!clientId) return undefined;
    let cancelled = false;
    const render = () => {
      if (cancelled || !window.google?.accounts?.id || !ref.current) return;
      window.google.accounts.id.initialize({
        client_id: clientId,
        callback: (resp) => (resp?.credential ? onCredential(resp.credential) : onError('Google sign-in was cancelled.')),
        ux_mode: 'popup',
        use_fedcm_for_prompt: true,
      });
      window.google.accounts.id.renderButton(ref.current, { theme: 'outline', size: 'large', text: 'continue_with', width: 336 });
    };
    let script = document.querySelector(`script[src="${GIS_SRC}"]`);
    if (!script) {
      script = document.createElement('script');
      script.src = GIS_SRC;
      script.async = true;
      script.onerror = () => onError("Couldn't load Google sign-in. Check your connection.");
      document.head.appendChild(script);
    }
    if (window.google?.accounts?.id) render();
    else script.addEventListener('load', render);
    return () => { cancelled = true; script.removeEventListener('load', render); };
  }, [clientId, onCredential, onError]);

  if (!clientId) {
    return (
      <button type="button" className="btn" disabled title="Set GOOGLE_CLIENT_ID on the server to enable this">
        Google sign-in isn’t set up on this server
      </button>
    );
  }
  return <div ref={ref} style={{ display: 'flex', justifyContent: 'center', minHeight: 44 }} />;
}

function OtpStep({ challenge, onVerify, onResend, onBack }) {
  const [code, setCode] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [info, setInfo] = useState('');
  const [now, setNow] = useState(Date.now());
  const inputRef = useRef(null);

  useEffect(() => { inputRef.current?.focus(); }, []);
  useEffect(() => { const t = setInterval(() => setNow(Date.now()), 1000); return () => clearInterval(t); }, []);
  const wait = Math.max(0, Math.ceil((new Date(challenge.resendAvailableAt).getTime() - now) / 1000));

  const submit = async (e) => {
    e.preventDefault();
    if (!/^\d{6}$/.test(code)) return setError('Enter the 6-digit code from the email.');
    setBusy(true); setError('');
    try { await onVerify(code); } catch (err) { setError(err.message); setCode(''); } finally { setBusy(false); }
  };
  const resend = async () => {
    setError(''); setInfo('');
    try { await onResend(); setInfo('We sent a new code.'); setCode(''); } catch (err) { setError(err.message); }
  };

  return (
    <form className="auth-form" onSubmit={submit} noValidate>
      <div>
        <h1>Check your email</h1>
        <p className="muted" style={{ margin: '6px 0 0' }}>
          We sent a 6-digit {challenge.purpose === 'SIGN_UP' ? 'verification' : 'sign-in'} code to <b>{challenge.emailHint}</b>. It expires in 10 minutes.
        </p>
      </div>
      <label className="field" htmlFor="otp-code">Code
        <input id="otp-code" ref={inputRef} className="input num otp-input" inputMode="numeric" autoComplete="one-time-code"
          maxLength={6} value={code} onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))} placeholder="••••••" />
      </label>
      {error && <div className="chip bad" style={{ borderRadius: 4, padding: '8px 10px', whiteSpace: 'normal' }} role="alert">{error}</div>}
      {info && <div className="chip good" style={{ borderRadius: 4, padding: '8px 10px' }}>{info}</div>}
      <button className="btn primary" type="submit" disabled={busy || code.length !== 6} style={{ padding: '11px 16px' }}>
        {busy ? 'Verifying…' : challenge.purpose === 'SIGN_UP' ? 'Verify email and continue' : 'Verify and sign in'}
      </button>
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
        <button type="button" className="btn ghost sm" onClick={onBack}>← Use a different email</button>
        <button type="button" className="btn ghost sm" onClick={resend} disabled={wait > 0}>
          {wait > 0 ? `Resend code in ${wait}s` : 'Resend code'}
        </button>
      </div>
    </form>
  );
}

const Alert = ({ kind = 'bad', children }) => (
  <div className={`chip ${kind}`} style={{ borderRadius: 4, padding: '8px 10px', whiteSpace: 'normal' }} role={kind === 'bad' ? 'alert' : 'status'}>
    {children}
  </div>
);

/** Forgot password: step 1 asks for the email, step 2 takes the emailed code and a new password. */
function ForgotPassword({ initialEmail, onDone, onBack }) {
  const { forgotPassword, resetPassword, resendOtp } = useAuth();
  const [email, setEmail] = useState(initialEmail || '');
  const [challenge, setChallenge] = useState(null);
  const [code, setCode] = useState('');
  const [pw, setPw] = useState('');
  const [pw2, setPw2] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [info, setInfo] = useState('');
  const [now, setNow] = useState(Date.now());
  useEffect(() => { const t = setInterval(() => setNow(Date.now()), 1000); return () => clearInterval(t); }, []);
  const wait = challenge ? Math.max(0, Math.ceil((new Date(challenge.resendAvailableAt).getTime() - now) / 1000)) : 0;

  const requestCode = async (e) => {
    e.preventDefault();
    if (!/^\S+@\S+\.\S+$/.test(email)) return setError('Enter a valid email address.');
    setBusy(true); setError('');
    try { setChallenge(await forgotPassword(email.trim())); } catch (err) { setError(err.message); } finally { setBusy(false); }
  };

  const reset = async (e) => {
    e.preventDefault();
    if (!/^\d{6}$/.test(code)) return setError('Enter the 6-digit code from the email.');
    if (pw.length < 8) return setError('Use at least 8 characters for your new password.');
    if (pw !== pw2) return setError("The two passwords don't match.");
    setBusy(true); setError(''); setInfo('');
    try { await resetPassword(challenge.challengeId, code, pw); onDone(); }
    catch (err) { setError(err.message); setCode(''); }
    finally { setBusy(false); }
  };

  const resend = async () => {
    setError(''); setInfo('');
    try { setChallenge(await resendOtp(challenge.challengeId)); setInfo('We sent a new code.'); setCode(''); }
    catch (err) { setError(err.message); }
  };

  if (!challenge) {
    return (
      <form className="auth-form" onSubmit={requestCode} noValidate>
        <div>
          <h1>Reset your password</h1>
          <p className="muted" style={{ margin: '6px 0 0' }}>Enter your account's email and we'll send you a code to set a new password.</p>
        </div>
        <label className="field" htmlFor="reset-email">Email
          <input id="reset-email" className="input" type="email" value={email} onChange={(e) => setEmail(e.target.value)}
            placeholder="you@example.com" autoComplete="email" maxLength={254} autoFocus />
        </label>
        {error && <Alert>{error}</Alert>}
        <button className="btn primary" type="submit" disabled={busy} style={{ padding: '11px 16px' }}>
          {busy ? 'Sending code…' : 'Send reset code'}
        </button>
        <button type="button" className="btn ghost sm" onClick={() => onBack(email.trim())}>← Back to log in</button>
      </form>
    );
  }

  return (
    <form className="auth-form" onSubmit={reset} noValidate>
      <div>
        <h1>Set a new password</h1>
        <p className="muted" style={{ margin: '6px 0 0' }}>
          If an account exists for <b>{challenge.emailHint}</b>, we've sent it a 6-digit code. It expires in 10 minutes.
        </p>
      </div>
      <label className="field" htmlFor="reset-code">Code
        <input id="reset-code" className="input num otp-input" inputMode="numeric" autoComplete="one-time-code" autoFocus
          maxLength={6} value={code} onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))} placeholder="••••••" />
      </label>
      <label className="field" htmlFor="reset-pw">New password
        <input id="reset-pw" className="input" type="password" value={pw} onChange={(e) => setPw(e.target.value)}
          placeholder="At least 8 characters" autoComplete="new-password" maxLength={72} />
      </label>
      <label className="field" htmlFor="reset-pw2">Confirm new password
        <input id="reset-pw2" className="input" type="password" value={pw2} onChange={(e) => setPw2(e.target.value)}
          aria-invalid={pw2.length > 0 && pw2 !== pw} autoComplete="new-password" maxLength={72} />
      </label>
      {error && <Alert>{error}</Alert>}
      {info && <Alert kind="good">{info}</Alert>}
      <button className="btn primary" type="submit" disabled={busy || code.length !== 6} style={{ padding: '11px 16px' }}>
        {busy ? 'Saving…' : 'Save password and log in'}
      </button>
      <p className="small faint" style={{ margin: 0, textAlign: 'center' }}>This signs you out on all other devices.</p>
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
        <button type="button" className="btn ghost sm" onClick={() => { setChallenge(null); setError(''); setInfo(''); }}>← Different email</button>
        <button type="button" className="btn ghost sm" onClick={resend} disabled={wait > 0}>
          {wait > 0 ? `Resend code in ${wait}s` : 'Resend code'}
        </button>
      </div>
    </form>
  );
}

export default function Login() {
  const { status, googleClientId, login, register, verifyOtp, resendOtp, loginWithGoogle } = useAuth();
  const nav = useNavigate();
  const loc = useLocation();
  const [mode, setMode] = useState('login');
  const [form, setForm] = useState({ name: '', email: '', password: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [challenge, setChallenge] = useState(null);
  const [badCredentials, setBadCredentials] = useState(false);
  const passwordRef = useRef(null);

  // only ever return to a path on this site (never "//evil.com" or "/\\evil.com")
  const from = typeof loc.state?.from === 'string' && /^\/(?![\/\\])/.test(loc.state.from) ? loc.state.from : '/';
  const goNext = () => nav(from, { replace: true });
  // stable callbacks so the Google button isn't re-initialised on every render
  const googleRef = useRef({});
  googleRef.current = { loginWithGoogle, goNext };
  const [onCredential] = useState(() => async (credential) => {
    setError('');
    try { await googleRef.current.loginWithGoogle(credential); googleRef.current.goNext(); } catch (err) { setError(err.message); }
  });
  const [onGoogleError] = useState(() => (msg) => setError(msg));

  if (status === 'authenticated') return <Navigate to={from} replace />;

  const set = (k) => (e) => { setForm({ ...form, [k]: e.target.value }); setBadCredentials(false); };
  const switchMode = (m) => { setMode(m); setError(''); setBadCredentials(false); };

  const submit = async (e) => {
    e.preventDefault();
    if (mode === 'signup' && !form.name.trim()) return setError('Tell us your name.');
    if (!/^\S+@\S+\.\S+$/.test(form.email)) return setError('Enter a valid email address.');
    if (mode === 'signup' && form.password.length < 8) return setError('Use at least 8 characters for your password.');
    if (!form.password) return setError('Enter your password.');
    setBusy(true); setError(''); setBadCredentials(false);
    try {
      const c = mode === 'signup'
        ? await register(form.name.trim(), form.email.trim(), form.password)
        : await login(form.email.trim(), form.password);
      setForm((f) => ({ ...f, password: '' })); // don't keep the password in memory longer than needed
      if (c) setChallenge(c); // a new or unverified account confirms its email first
      else goNext();          // verified account: signed in already
    } catch (err) {
      if (mode === 'login' && err.status === 401) {
        // wrong email or password: say so plainly, mark both fields and let them retype the password
        setBadCredentials(true);
        setError('Incorrect email or password. Check both and try again, or reset your password.');
        setForm((f) => ({ ...f, password: '' }));
        setTimeout(() => passwordRef.current?.focus(), 0);
      } else {
        setError(err.message);
      }
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="auth">
      <LoginHero />

      <section className="auth-form-wrap">
        {status === 'loading' ? (
          <div className="auth-form"><p className="muted" style={{ margin: 0 }}>Checking your session…</p></div>
        ) : challenge ? (
          <OtpStep
            challenge={challenge}
            onVerify={async (code) => { await verifyOtp(challenge.challengeId, code); goNext(); }}
            onResend={async () => setChallenge(await resendOtp(challenge.challengeId))}
            onBack={() => { setChallenge(null); setError(''); }}
          />
        ) : mode === 'forgot' ? (
          <ForgotPassword initialEmail={form.email} onDone={goNext} onBack={(email) => { if (email) setForm((f) => ({ ...f, email })); switchMode('login'); }} />
        ) : (
          <form className="auth-form" onSubmit={submit} noValidate>
            <div>
              <h1>{mode === 'login' ? 'Welcome back' : 'Create your account'}</h1>
              <p className="muted" style={{ margin: '6px 0 0' }}>
                {mode === 'login' ? 'Log in to continue your prep.' : 'Start tracking your CAT preparation.'}
              </p>
            </div>

            <GoogleButton clientId={googleClientId} onCredential={onCredential} onError={onGoogleError} />
            <div className="divider">or with email</div>

            {mode === 'signup' && (
              <label className="field" htmlFor="name">Full name
                <input id="name" className="input" value={form.name} onChange={set('name')} placeholder="Riya Sharma" autoComplete="name" maxLength={120} />
              </label>
            )}
            <label className="field" htmlFor="email">Email
              <input id="email" className="input" type="email" aria-invalid={badCredentials} value={form.email} onChange={set('email')} placeholder="you@example.com" autoComplete="email" maxLength={254} />
            </label>
            <div style={{ position: 'relative' }}>
              <label className="field" htmlFor="password">Password
                <input id="password" ref={passwordRef} className="input" type="password" aria-invalid={badCredentials} value={form.password} onChange={set('password')}
                  placeholder={mode === 'signup' ? 'At least 8 characters' : '••••••••'} maxLength={72}
                  autoComplete={mode === 'login' ? 'current-password' : 'new-password'} />
              </label>
              {mode === 'login' && (
                <button type="button" className="link-btn" style={{ position: 'absolute', top: 0, right: 0 }}
                  onClick={() => switchMode('forgot')}>Forgot password?</button>
              )}
            </div>

            {error && (
              <Alert>
                <span>
                  {error}
                  {badCredentials && (
                    <> <button type="button" className="link-btn" style={{ color: 'inherit', fontSize: 'inherit', display: 'inline' }}
                      onClick={() => switchMode('forgot')}>Reset password</button></>
                  )}
                </span>
              </Alert>
            )}

            <button className="btn primary" type="submit" disabled={busy} style={{ padding: '11px 16px' }}>
              {busy ? (mode === 'login' ? 'Logging in…' : 'Creating account…') : mode === 'login' ? 'Log in' : 'Create account'}
            </button>
            {mode === 'signup' && (
              <p className="small faint" style={{ margin: 0, textAlign: 'center' }}>We’ll email you a code to verify your address.</p>
            )}

            <p className="small muted" style={{ textAlign: 'center', margin: 0 }}>
              {mode === 'login' ? "Don't have an account? " : 'Already registered? '}
              <button type="button" className="btn ghost sm" style={{ padding: '2px 4px' }}
                onClick={() => switchMode(mode === 'login' ? 'signup' : 'login')}>
                {mode === 'login' ? 'Sign up' : 'Log in'}
              </button>
            </p>
          </form>
        )}
      </section>
    </div>
  );
}
