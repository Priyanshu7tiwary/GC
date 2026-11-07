// Thin fetch wrapper for the CATalyst API.
// - The session lives in an HttpOnly cookie the browser sends automatically; this code never sees it.
// - Every state-changing request echoes the XSRF-TOKEN cookie in the X-XSRF-TOKEN header (CSRF protection).

function readCookie(name) {
  const match = document.cookie.split('; ').find((c) => c.startsWith(`${name}=`));
  return match ? decodeURIComponent(match.slice(name.length + 1)) : null;
}

export class ApiError extends Error {
  constructor(status, message, fieldErrors) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors || {};
  }
}

export async function api(path, { method = 'GET', body } = {}) {
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (method !== 'GET') {
    const xsrf = readCookie('XSRF-TOKEN');
    if (xsrf) headers['X-XSRF-TOKEN'] = xsrf;
  }

  let res;
  try {
    res = await fetch(path, {
      method,
      headers,
      credentials: 'same-origin',
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, "Can't reach the server. Check your connection and try again.");
  }

  if (res.status === 204) return null;
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    const fields = data?.errors || {};
    const first = Object.values(fields)[0];
    const message = res.status === 403
      ? 'Your session expired. Refresh the page and try again.'
      : first || data?.detail || `Request failed (${res.status}).`;
    throw new ApiError(res.status, message, fields);
  }
  return data;
}
