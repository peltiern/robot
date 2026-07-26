import type { Animation, SaveResponse, ValidationWarning } from '../types/animation'

const BASE = `${import.meta.env.VITE_API_URL ?? 'http://localhost:8080'}/api/animations`

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...init,
  })
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  if (res.status === 204) return undefined as T
  return res.json()
}

export const animationApi = {
  listNames: (): Promise<string[]> =>
    request('/'),

  get: (name: string): Promise<Animation> =>
    request(`/${encodeURIComponent(name)}`),

  create: (animation: Animation): Promise<SaveResponse> =>
    request('/', { method: 'POST', body: JSON.stringify(animation) }),

  update: (name: string, animation: Animation): Promise<SaveResponse> =>
    request(`/${encodeURIComponent(name)}`, { method: 'PUT', body: JSON.stringify(animation) }),

  delete: (name: string): Promise<void> =>
    request(`/${encodeURIComponent(name)}`, { method: 'DELETE' }),

  play: (name: string): Promise<void> =>
    request(`/${encodeURIComponent(name)}/play`, { method: 'POST' }),

  preview: (animation: Animation): Promise<ValidationWarning[]> =>
    request('/preview', { method: 'POST', body: JSON.stringify(animation) }),
}
