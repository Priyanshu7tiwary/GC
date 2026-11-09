import { useEffect, useState } from 'react';

/** A thin bar across the top of the screen while the app waits on the server. It appears only if a wait lasts
 *  over a third of a second, so quick requests and background refreshes never flash it. */
export default function TopBar() {
  const [show, setShow] = useState(false);
  useEffect(() => {
    let timer = null;
    const onBusy = (e) => {
      if (e.detail > 0) {
        if (!timer) timer = setTimeout(() => setShow(true), 350);
      } else {
        clearTimeout(timer);
        timer = null;
        setShow(false);
      }
    };
    window.addEventListener('api-busy', onBusy);
    return () => { window.removeEventListener('api-busy', onBusy); clearTimeout(timer); };
  }, []);
  return <div className={`loadbar ${show ? "on" : ""}`} role="progressbar" aria-label="Loading" aria-hidden={!show}><span /></div>;
}
