// Central API configuration
// Exports a resolved API base URL and a helper to build API endpoints.

const DEFAULT_API_HOST = 'http://localhost:8080';

// Prefer Vite env var `VITE_API_BASE_URL` when provided. Use a safe `any` cast for import.meta.
const env = (import.meta as any)?.env ?? {};
export const API_BASE_URL: string = (env.VITE_API_BASE_URL && String(env.VITE_API_BASE_URL)) || DEFAULT_API_HOST;

export function apiUrl(path: string) {
  const base = API_BASE_URL.replace(/\/$/, '');
  if (!path) return base;
  return base + (path.startsWith('/') ? path : '/' + path);
}

export default {
  API_BASE_URL,
  apiUrl,
};
