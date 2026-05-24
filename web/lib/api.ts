import { API_ROUTES, AI_CONSENT_REQUIRED_CODE, type AiConsultSessionDetail, type AiConsultSessionSummary, type AiConsultStreamEvent, type AiVisionRequest, type AiVisionResponse, type ApiErrorResponse, type ApiResponse, type AskPatientAiResponse, type AuthResponse, type CurrentUserResponse, type DictionaryItemResponse, type DictionaryResponse, type EntityRecord, errorCodeMessages, type HealthResponse, type LoginPayload, type RegisterPayload, type StatusChangeRequest, type StatusManagedRoute } from './api-contract';
export type { EntityRecord, CurrentUserResponse, DictionaryItemResponse, DictionaryResponse, AiVisionResponse, MedicalRecordFields, AskPatientAiResponse, AiPatientCitation, AiConsultMessage, AiConsultSessionDetail, AiConsultSessionSummary, AiConsultStreamEvent } from './api-contract';
export { AI_CONSENT_REQUIRED_CODE } from './api-contract';

/**
 * Thrown by {@link askPatientAi} when the backend returns HTTP 412 with
 * {@code code: 'AI_CONSENT_REQUIRED'}. The patient detail page catches this
 * specifically and pops the consent modal instead of showing a generic error.
 */
export class AiConsentRequiredError extends Error {
  readonly code = AI_CONSENT_REQUIRED_CODE;
  constructor(message = '患者尚未授权 AI 智能辅助服务') {
    super(message);
    this.name = 'AiConsentRequiredError';
  }
}

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';
const TRACE_ID_HEADER = 'X-Trace-Id';

function generateTraceId() { return typeof crypto !== 'undefined' && 'randomUUID' in crypto ? crypto.randomUUID() : `${Date.now()}-${Math.random()}`; }
export type PageResponse<T> = { records: T[]; total: number; page: number; size: number };
export type ListQuery = { page?: number; size?: number; sortBy?: string; sortDir?: 'asc' | 'desc'; keyword?: string; itemName?: string; enabled?: boolean };

async function parseError(response: Response): Promise<Error> {
  const text = await response.text();
  try {
    const parsed = JSON.parse(text) as Partial<ApiErrorResponse>;
    const human = parsed.errorCode ? errorCodeMessages[parsed.errorCode] ?? parsed.message ?? 'Request failed' : parsed.message ?? 'Request failed';
    return new Error(human);
  } catch { return new Error(text || `Request failed with status ${response.status}`); }
}
async function unwrapResponse<T>(response: Response): Promise<T> { if (!response.ok) throw await parseError(response); return (await response.json() as ApiResponse<T>).data; }

export function applyAuthResponse(response: Response, requestUrl: string): void {
  if (typeof window === 'undefined') return;
  const refreshed = response.headers.get('X-Refreshed-Token');
  if (refreshed) localStorage.setItem('access_token', refreshed);
  if (response.status === 401 && !requestUrl.includes('/auth/login') && !requestUrl.includes('/auth/register')) {
    localStorage.removeItem('access_token');
    localStorage.removeItem('token_type');
    if (!window.location.pathname.startsWith('/login')) {
      window.location.assign('/login');
    }
  }
}

async function apiFetch(route: string, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers);
  headers.set(TRACE_ID_HEADER, generateTraceId());
  const url = `${API_BASE_URL}${route}`;
  const response = await fetch(url, { ...init, headers });
  applyAuthResponse(response, url);
  return response;
}
const authHeader = (token: string) => ({ Authorization: `Bearer ${token}` });
// Doctor + FamilyDoctor backends switched to {enabled} (app_user.enabled toggle), so they
// fall back to the default {enabled} payload below. Only routes that still use targetStatus
// enums remain here.
const statusChangeByRoute: Partial<{ [R in StatusManagedRoute]: (enabled: boolean) => StatusChangeRequest<R> }> = {
  [API_ROUTES.visits]: (enabled) => ({ targetStatus: enabled ? 'COMPLETED' : 'CANCELLED' }),
  [API_ROUTES.medications]: (enabled) => ({ targetStatus: enabled ? 'ENABLED' : 'DISABLED' }),
  [API_ROUTES.configs]: (enabled) => ({ targetStatus: enabled ? 'ENABLED' : 'DISABLED' }),
  [API_ROUTES.medicalRecords]: (enabled) => ({ targetStatus: enabled ? 'ACTIVE' : 'ARCHIVED' }),
  [API_ROUTES.healthRecords]: (enabled) => ({ targetStatus: enabled ? 'ACTIVE' : 'ARCHIVED' })
};

export async function login(payload: LoginPayload): Promise<AuthResponse> { const r = await apiFetch(API_ROUTES.authLogin,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)}); if (!r.ok) throw await parseError(r); return r.json() as Promise<AuthResponse>; }
export async function register(payload: RegisterPayload): Promise<void> { const r = await apiFetch(API_ROUTES.authRegister,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)}); if (!r.ok) throw await parseError(r); }
export async function healthCheck(): Promise<HealthResponse> { const r = await apiFetch(API_ROUTES.health,{cache:'no-store'}); if (!r.ok) throw new Error('Health check failed'); return r.json() as Promise<HealthResponse>; }
export async function currentUser(token: string): Promise<CurrentUserResponse> { return unwrapResponse(await apiFetch(API_ROUTES.authCurrentUser,{headers:authHeader(token),cache:'no-store'})); }
export async function listDictionaries(token: string): Promise<DictionaryResponse[]> {
  const data = await unwrapResponse<DictionaryResponse[] | PageResponse<DictionaryResponse>>(await apiFetch(API_ROUTES.dictionaries,{headers:authHeader(token),cache:'no-store'}));
  if (Array.isArray(data)) return data;
  return Array.isArray(data?.records) ? data.records : [];
}
export async function queryDictionaryItems(token: string, query: ListQuery & { dictCode: string }): Promise<PageResponse<DictionaryItemResponse>> {
  const p = new URLSearchParams();
  ['page','size','itemName','sortBy','sortDir'].forEach((k)=>{
    const v = query[k as keyof typeof query];
    if (v !== undefined) p.set(k, String(v));
  });
  return unwrapResponse(await apiFetch(`${API_ROUTES.dictionaryItems(query.dictCode)}?${p.toString()}`, { headers: authHeader(token), cache:'no-store' }));
}
export async function listEntities(
  token: string,
  route: string,
  params: { keyword?: string; page?: number; size?: number; [key: string]: string | number | undefined } = {}
): Promise<PageResponse<EntityRecord>> {
  const p = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => { if (v !== undefined) p.set(k, String(v)); });
  return unwrapResponse(await apiFetch(`${route}?${p.toString()}`, { headers: authHeader(token), cache: 'no-store' }));
}
export async function getEntity(token: string, route: string, id: number): Promise<EntityRecord> { return unwrapResponse(await apiFetch(`${route}/${id}`, { headers: authHeader(token), cache:'no-store' })); }
export async function createEntity(token: string, route: string, payload: Record<string, unknown>): Promise<void> { await unwrapResponse(await apiFetch(route,{method:'POST',headers:{...authHeader(token),'Content-Type':'application/json'},body:JSON.stringify(payload)})); }
export async function updateEntity(token: string, route: string, id: number, payload: Record<string, unknown>): Promise<void> { await unwrapResponse(await apiFetch(`${route}/${id}`,{method:'PUT',headers:{...authHeader(token),'Content-Type':'application/json'},body:JSON.stringify(payload)})); }
export async function deleteEntity(token: string, route: string, id: number | string): Promise<void> {
  await unwrapResponse(await apiFetch(`${route}/${id}`, { method: 'DELETE', headers: authHeader(token) }));
}
/**
 * Call the backend AI Vision OCR endpoint. Maps friendly Chinese messages to
 * the common HTTP error shapes so the UI doesn't have to repeat the table.
 * Accepts either a {photoId} or an inline {base64, contentType} payload —
 * the controller validates and returns 400 if neither is present.
 */
export async function parseMedicalRecordPhoto(token: string, payload: AiVisionRequest): Promise<AiVisionResponse> {
  const response = await apiFetch(API_ROUTES.aiVisionParseMedicalRecord, {
    method: 'POST',
    headers: { ...authHeader(token), 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  if (!response.ok) {
    if (response.status === 401) throw new Error('登录已过期，请重新登录');
    if (response.status === 403) throw new Error('当前账号无 AI 识别权限（ai:vision）');
    if (response.status === 429) {
      let detail = '请稍后重试';
      try {
        const body = await response.json();
        if (body?.reason === 'qpm') detail = '调用太频繁，请稍候再试';
        else if (body?.reason === 'daily-token-budget') detail = '今日 AI 配额已用尽，请明天再试';
      } catch { /* ignore */ }
      throw new Error('AI 调用频率超限：' + detail);
    }
    if (response.status === 400) throw new Error('AI 识别请求参数无效');
    if (response.status >= 500) throw new Error('AI 服务暂时不可用，请稍后再试');
    throw await parseError(response);
  }
  return (await response.json() as ApiResponse<AiVisionResponse>).data;
}

/**
 * POST /api/v1/ai/patient/{patientId}/ask — ask a natural-language question
 * about ONE patient's records. The server enforces:
 *
 * - ai:patient-rag permission (403 if missing),
 * - row-level access (doctor/admin OR the patient themselves; 403 otherwise),
 * - prior consent on patient_profile.ai_consent_at (412 + AI_CONSENT_REQUIRED
 *   when null — translated to {@link AiConsentRequiredError} below so the UI
 *   can pop the consent modal without parsing strings).
 *
 * 429 maps to a friendly message; everything else falls through to
 * {@link parseError} for the generic Chinese banner.
 */
export async function askPatientAi(
  token: string,
  patientId: number | string,
  question: string
): Promise<AskPatientAiResponse> {
  const response = await apiFetch(API_ROUTES.aiPatientAsk(patientId), {
    method: 'POST',
    headers: { ...authHeader(token), 'Content-Type': 'application/json' },
    body: JSON.stringify({ question }),
  });
  if (!response.ok) {
    if (response.status === 412) {
      // Distinguish "no consent yet" so the UI can pop the modal silently.
      let message = '患者尚未授权 AI 智能辅助服务';
      try {
        const body = await response.clone().json();
        if (body?.code === AI_CONSENT_REQUIRED_CODE) {
          if (body?.message) message = body.message;
          throw new AiConsentRequiredError(message);
        }
      } catch (err) {
        if (err instanceof AiConsentRequiredError) throw err;
        // Fallthrough to generic parsing if body isn't the expected shape.
      }
    }
    if (response.status === 401) throw new Error('登录已过期，请重新登录');
    if (response.status === 403) throw new Error('当前账号无 AI 问询权限（ai:patient-rag）');
    if (response.status === 404) throw new Error('患者不存在');
    if (response.status === 429) {
      let detail = '请稍后重试';
      try {
        const body = await response.json();
        if (body?.reason === 'qpm') detail = '调用太频繁，请稍候再试';
        else if (body?.reason === 'daily-token-budget') detail = '今日 AI 配额已用尽，请明天再试';
      } catch { /* ignore */ }
      throw new Error('AI 调用频率超限：' + detail);
    }
    if (response.status >= 500) throw new Error('AI 服务暂时不可用，请稍后再试');
    throw await parseError(response);
  }
  return (await response.json() as ApiResponse<AskPatientAiResponse>).data;
}

/**
 * POST /api/v1/ai/patient/{patientId}/consent — mark the patient as having
 * accepted AI processing. Only the patient themselves (matched on
 * patient_profile.user_id) or an admin can call this; the server returns 403
 * to anyone else even with ai:patient-rag.
 */
export async function grantAiConsent(
  token: string,
  patientId: number | string
): Promise<{ consentedAt: string }> {
  const response = await apiFetch(API_ROUTES.aiPatientConsent(patientId), {
    method: 'POST',
    headers: { ...authHeader(token), 'Content-Type': 'application/json' },
  });
  if (!response.ok) {
    if (response.status === 401) throw new Error('登录已过期，请重新登录');
    if (response.status === 403) throw new Error('仅患者本人或管理员可签署该授权');
    if (response.status === 404) throw new Error('患者不存在');
    throw await parseError(response);
  }
  return (await response.json() as ApiResponse<{ consentedAt: string }>).data;
}

// ---------------------------------------------------------------------------
// Community AI consult (Phase 3) — session CRUD + SSE streaming chat.
// ---------------------------------------------------------------------------

/** List the caller's consult sessions (paginated, most-recent first). */
export async function listConsultSessions(
  token: string,
  query: { page?: number; size?: number } = {}
): Promise<PageResponse<AiConsultSessionSummary>> {
  const p = new URLSearchParams();
  if (query.page !== undefined) p.set('page', String(query.page));
  if (query.size !== undefined) p.set('size', String(query.size));
  return unwrapResponse(await apiFetch(`${API_ROUTES.aiConsultSessions}?${p.toString()}`, {
    headers: authHeader(token), cache: 'no-store',
  }));
}

/** Create a new session. Title defaults server-side to "新对话". */
export async function createConsultSession(token: string, title?: string): Promise<AiConsultSessionSummary> {
  return unwrapResponse(await apiFetch(API_ROUTES.aiConsultSessions, {
    method: 'POST',
    headers: { ...authHeader(token), 'Content-Type': 'application/json' },
    body: JSON.stringify(title ? { title } : {}),
  }));
}

/** Fetch a single session with full message history (caller must own it). */
export async function getConsultSession(token: string, id: number): Promise<AiConsultSessionDetail> {
  return unwrapResponse(await apiFetch(API_ROUTES.aiConsultSession(id), {
    headers: authHeader(token), cache: 'no-store',
  }));
}

/** Rename a session. Server rejects empty / >200 chars with 400. */
export async function renameConsultSession(
  token: string, id: number, title: string
): Promise<AiConsultSessionSummary> {
  return unwrapResponse(await apiFetch(API_ROUTES.aiConsultSession(id), {
    method: 'PATCH',
    headers: { ...authHeader(token), 'Content-Type': 'application/json' },
    body: JSON.stringify({ title }),
  }));
}

/** Delete a session and all its messages (CASCADE in V43). */
export async function deleteConsultSession(token: string, id: number): Promise<void> {
  await unwrapResponse(await apiFetch(API_ROUTES.aiConsultSession(id), {
    method: 'DELETE', headers: authHeader(token),
  }));
}

/**
 * Stream an assistant reply for one new user message. The server returns
 * `text/event-stream`; we parse the raw bytes with TextDecoder rather than
 * EventSource so we can attach the Bearer token (EventSource has no header API)
 * and reuse our generic auth refresh logic.
 *
 * Callbacks:
 * - {@code onChunk(delta)}  — fires for every {"delta": "..."} frame.
 * - {@code onDone(meta)}    — fires once when the stream completes normally;
 *                              {@code meta.failed} / {@code meta.refused} flag
 *                              guardrail / upstream errors.
 * - {@code onError(err)}    — fires on network failure or HTTP non-2xx before
 *                              the stream began. NOT called for mid-stream
 *                              errors signalled via {@code onDone({failed:true})}.
 */
export async function streamConsultMessage(
  token: string,
  sessionId: number,
  content: string,
  callbacks: {
    onChunk: (delta: string) => void;
    onDone: (meta: { messageId: number; tokensIn: number; tokensOut: number; refused?: boolean; failed?: boolean }) => void;
    onError: (err: Error) => void;
    signal?: AbortSignal;
  }
): Promise<void> {
  const url = `${API_BASE_URL}${API_ROUTES.aiConsultSessionMessages(sessionId)}`;
  let response: Response;
  try {
    response = await fetch(url, {
      method: 'POST',
      headers: {
        ...authHeader(token),
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream',
        [TRACE_ID_HEADER]: generateTraceId(),
      },
      body: JSON.stringify({ content }),
      signal: callbacks.signal,
    });
  } catch (err) {
    callbacks.onError(err instanceof Error ? err : new Error(String(err)));
    return;
  }
  applyAuthResponse(response, url);

  if (!response.ok) {
    if (response.status === 401) { callbacks.onError(new Error('登录已过期，请重新登录')); return; }
    if (response.status === 403) { callbacks.onError(new Error('当前账号无社区 AI 问诊权限（ai:consult）')); return; }
    if (response.status === 404) { callbacks.onError(new Error('会话不存在或无权访问')); return; }
    if (response.status === 429) {
      let detail = '请稍后重试';
      try {
        const body = await response.json();
        if (body?.reason === 'qpm') detail = '调用太频繁，请稍候再试';
        else if (body?.reason === 'daily-token-budget') detail = '今日 AI 配额已用尽，请明天再试';
      } catch { /* ignore */ }
      callbacks.onError(new Error('AI 调用频率超限：' + detail));
      return;
    }
    if (response.status >= 500) { callbacks.onError(new Error('AI 服务暂时不可用，请稍后再试')); return; }
    callbacks.onError(await parseError(response));
    return;
  }

  const body = response.body;
  if (!body) { callbacks.onError(new Error('响应没有内容')); return; }

  const reader = body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';
  try {
    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });

      // SSE frames are separated by a blank line. Process every complete event
      // and leave the trailing partial in buffer for the next read.
      let sep = buffer.indexOf('\n\n');
      while (sep !== -1) {
        const rawEvent = buffer.slice(0, sep);
        buffer = buffer.slice(sep + 2);
        sep = buffer.indexOf('\n\n');

        for (const line of rawEvent.split('\n')) {
          const trimmed = line.startsWith('data:') ? line.slice(5).trim() : line.trim();
          if (!trimmed || trimmed.startsWith(':')) continue;
          if (trimmed === '[DONE]') { return; }
          let event: AiConsultStreamEvent;
          try { event = JSON.parse(trimmed) as AiConsultStreamEvent; }
          catch { continue; }
          if ('done' in event && event.done) {
            callbacks.onDone({
              messageId: event.messageId,
              tokensIn: event.tokensIn,
              tokensOut: event.tokensOut,
              refused: event.refused,
              failed: event.failed,
            });
          } else if ('error' in event) {
            callbacks.onChunk('\n[错误] ' + event.error);
          } else if ('delta' in event) {
            callbacks.onChunk(event.delta);
          }
        }
      }
    }
  } catch (err) {
    callbacks.onError(err instanceof Error ? err : new Error(String(err)));
  }
}

export async function changeEntityStatus(token: string, route: string, id: number, enabled: boolean): Promise<void> {
  const mapper = statusChangeByRoute[route as StatusManagedRoute];
  const payload = mapper ? mapper(enabled) : ({ enabled } as Record<string, unknown>);
  await unwrapResponse(await apiFetch(`${route}/${id}/status`,{method:'PATCH',headers:{...authHeader(token),'Content-Type':'application/json'},body:JSON.stringify(payload)}));
}
