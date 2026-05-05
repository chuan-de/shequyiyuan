const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';
const TRACE_ID_HEADER = 'X-Trace-Id';

function generateTraceId() {
  return typeof crypto !== 'undefined' && 'randomUUID' in crypto ? crypto.randomUUID() : `${Date.now()}-${Math.random()}`;
}

export type LoginPayload = { username: string; password: string };
export type RegisterPayload = { username: string; password: string };
export type AuthResponse = { accessToken: string; tokenType: string; expiresInSeconds: number };
export type CurrentUserResponse = { username: string; enabled: boolean; roles: string[] };
export type DictionaryResponse = { code: string; name: string };
export type DictionaryItemResponse = { id: number; name: string; value: string; sortOrder: number; enabled: boolean };

type ApiResponse<T> = { success: boolean; message?: string; data: T };
type ApiErrorResponse = { message?: string; errorCode?: string; details?: string[]; traceId?: string };

const errorCodeMessages: Record<string, string> = {
  VALIDATION_ERROR: '请求参数不合法', CONFLICT: '请求冲突，请检查数据状态', INTERNAL_ERROR: '系统开小差了，请稍后再试',
  UNAUTHORIZED: '用户名或密码错误', FORBIDDEN: '权限不足，无法执行该操作', NOT_FOUND: '请求资源不存在'
};

async function parseError(response: Response): Promise<Error> {
  const text = await response.text();
  try {
    const parsed = JSON.parse(text) as Partial<ApiErrorResponse>;
    if (parsed) {
      const human = parsed.errorCode ? errorCodeMessages[parsed.errorCode] ?? parsed.message ?? '请求失败' : parsed.message ?? '请求失败';
      const details = Array.isArray(parsed.details) && parsed.details.length > 0 ? `：${parsed.details.join('，')}` : '';
      const traceId = parsed.traceId ? `（追踪ID: ${parsed.traceId}）` : '';
      return new Error(`${human}${details}${traceId}`);
    }
  } catch {}
  return new Error(text || `Request failed with status ${response.status}`);
}

async function unwrapResponse<T>(response: Response): Promise<T> { if (!response.ok) throw await parseError(response); return (await response.json() as ApiResponse<T>).data; }

async function apiFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const traceId = generateTraceId();
  const headers = new Headers(init.headers);
  headers.set(TRACE_ID_HEADER, traceId);
  return fetch(`${API_BASE_URL}${path}`, { ...init, headers });
}

export async function login(payload: LoginPayload): Promise<AuthResponse> { return unwrapResponse(await apiFetch('/api/v1/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)})); }
export async function register(payload: RegisterPayload): Promise<void> { await unwrapResponse<Record<string,unknown>>(await apiFetch('/api/v1/auth/register',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)})); }
export async function healthCheck(): Promise<{ status: string; service: string; timestamp: string }> { return unwrapResponse(await apiFetch('/api/v1/health',{cache:'no-store'})); }
export async function currentUser(token: string): Promise<CurrentUserResponse> { return unwrapResponse(await apiFetch('/api/v1/auth/me',{headers:{Authorization:`Bearer ${token}`},cache:'no-store'})); }
export async function listDictionaries(token: string): Promise<DictionaryResponse[]> { const r=await apiFetch('/api/v1/dictionaries',{headers:{Authorization:`Bearer ${token}`},cache:'no-store'}); if(!r.ok) throw await parseError(r); return r.json(); }
export async function listDictionaryItems(token: string, dictCode: string): Promise<DictionaryItemResponse[]> { const r=await apiFetch(`/api/v1/dictionaries/${dictCode}/items`,{headers:{Authorization:`Bearer ${token}`},cache:'no-store'}); if(!r.ok) throw await parseError(r); return r.json(); }
