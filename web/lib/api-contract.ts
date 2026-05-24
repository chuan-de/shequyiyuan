export const API_ROUTES = {
  authLogin: '/api/v1/auth/login',
  authRegister: '/api/v1/auth/register',
  authCurrentUser: '/api/v1/auth/me',
  health: '/api/v1/health',
  dictionaries: '/api/v1/dictionaries',
  dictionaryItems: (dictionaryCode: string) => `/api/v1/dictionaries/${dictionaryCode}/items`,
  medications: '/api/v1/medications',
  familyDoctors: '/api/v1/family-doctors',
  visits: '/api/v1/visits',
  medicalRecords: '/api/v1/medical-records',
  healthRecords: '/api/v1/health-records',
  doctors: '/api/v1/doctors',
  configs: '/api/v1/configs',
  patients: '/api/v1/patients',
  receptions: '/api/v1/receptions',
  departments: '/api/v1/departments',
  aiVisionParseMedicalRecord: '/api/v1/ai/vision/parse-medical-record',
  aiPatientAsk: (patientId: number | string) => `/api/v1/ai/patient/${patientId}/ask`,
  aiPatientConsent: (patientId: number | string) => `/api/v1/ai/patient/${patientId}/consent`,
  aiConsultSessions: '/api/v1/ai/consult/sessions',
  aiConsultSession: (id: number | string) => `/api/v1/ai/consult/sessions/${id}`,
  aiConsultSessionMessages: (id: number | string) => `/api/v1/ai/consult/sessions/${id}/messages`
} as const;

export type LoginPayload = { username: string; password: string };
export type RegisterPayload = { username: string; password: string };
export type AuthResponse = { accessToken: string; tokenType: string; expiresInSeconds: number };
export type CurrentUserResponse = { username: string; enabled: boolean; roles: string[]; permissions: string[] };
export type DictionaryResponse = { code: string; name: string };
export type DictionaryItemResponse = { id: number; name: string; value: string; sortOrder: number; enabled: boolean };
export type HealthResponse = { status: string; service: string; timestamp: string };
export type EntityRecord = { id: number; name?: string; enabled?: boolean; status?: string; [key: string]: unknown };
export type StatusManagedRoute =
  | typeof API_ROUTES.doctors
  | typeof API_ROUTES.familyDoctors
  | typeof API_ROUTES.visits
  | typeof API_ROUTES.medications
  | typeof API_ROUTES.configs
  | typeof API_ROUTES.medicalRecords
  | typeof API_ROUTES.healthRecords;

export type DoctorStatus = 'ACTIVE' | 'INACTIVE';
export type FamilyDoctorStatus = 'PENDING' | 'ACTIVE' | 'SUSPENDED' | 'TERMINATED';
export type VisitStatus = 'PENDING' | 'COMPLETED' | 'CANCELLED';
export type MedicationStatus = 'ENABLED' | 'DISABLED';
export type ConfigStatus = 'ENABLED' | 'DISABLED';
export type MedicalRecordStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED';
export type HealthRecordStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED';

export type EntityStatusByRoute = {
  [API_ROUTES.doctors]: DoctorStatus;
  [API_ROUTES.familyDoctors]: FamilyDoctorStatus;
  [API_ROUTES.visits]: VisitStatus;
  [API_ROUTES.medications]: MedicationStatus;
  [API_ROUTES.configs]: ConfigStatus;
  [API_ROUTES.medicalRecords]: MedicalRecordStatus;
  [API_ROUTES.healthRecords]: HealthRecordStatus;
};

export type StatusChangeRequest<R extends StatusManagedRoute = StatusManagedRoute> = {
  targetStatus: EntityStatusByRoute[R];
  reason?: string;
};

export type ApiResponse<T> = { success: boolean; message?: string; data: T };
export type ApiErrorResponse = { message?: string; errorCode?: string; details?: string[]; traceId?: string };

/**
 * Structured fields extracted from a medical-record image by the AI vision
 * service. Mirrors {@code com.hospital.ai.vision.MedicalRecordFields}. All
 * keys are nullable — the medic decides which suggestions to accept.
 */
export type MedicalRecordFields = {
  patientName: string | null;
  gender: string | null;
  age: number | null;
  visitDate: string | null;
  department: string | null;
  chiefComplaint: string | null;
  presentIllness: string | null;
  diagnosis: string | null;
  prescription: string | null;
  doctor: string | null;
};

/**
 * Response from POST /api/v1/ai/vision/parse-medical-record. {@code rawJson}
 * is the model's full payload (kept for debugging); UI primarily uses
 * {@code fields} and the token/latency stats so the medic sees the cost.
 */
export type AiVisionResponse = {
  rawJson: Record<string, unknown>;
  fields: MedicalRecordFields;
  confidence: number;
  tokensIn: number;
  tokensOut: number;
  latencyMs: number;
  extractionHistoryId: number;
};

export type AiVisionRequest =
  | { photoId: string; prompt?: string }
  | { base64: string; contentType: string; prompt?: string };

/**
 * One source-document slice the RAG retrieval surfaced as evidence for an
 * answer. UI renders these as footnotes ([#1] [#2] …) with a side drawer
 * that shows the snippet + a deep-link to the original record.
 */
export type AiPatientCitation = {
  chunkId: number;
  sourceType: 'medical_record' | 'health_record' | 'visit';
  sourceId: number;
  fieldKey: string;
  snippet: string;
  sourceDate: string | null;
  similarity: number | null;
};

/** Response shape from POST /api/v1/ai/patient/{id}/ask. */
export type AskPatientAiResponse = {
  answer: string;
  citations: AiPatientCitation[];
  tokensIn: number;
  tokensOut: number;
  latencyMs: number;
};

export type AskPatientAiRequest = { question: string };

/** Server-side error code when patient hasn't granted AI consent yet. */
export const AI_CONSENT_REQUIRED_CODE = 'AI_CONSENT_REQUIRED';

/**
 * Community AI consult session (Phase 3). One thread per row in the left rail
 * on /ai-consult. {@code messageCount} is provided by the list endpoint so we
 * can render the badge without a separate fetch.
 */
export type AiConsultSessionSummary = {
  id: number;
  title: string;
  updatedAt: string;
  createdAt: string;
  messageCount: number;
};

/** One turn in a consult thread. Role is one of system / user / assistant. */
export type AiConsultMessage = {
  id: number;
  role: 'system' | 'user' | 'assistant';
  content: string;
  tokensIn: number | null;
  tokensOut: number | null;
  model: string | null;
  status: 'completed' | 'failed' | 'refused_by_guardrail';
  createdAt: string;
};

export type AiConsultSessionDetail = {
  id: number;
  title: string;
  updatedAt: string;
  createdAt: string;
  messages: AiConsultMessage[];
};

/** One SSE frame emitted by POST /api/v1/ai/consult/sessions/{id}/messages. */
export type AiConsultStreamEvent =
  | { delta: string }
  | { error: string }
  | { done: true; messageId: number; tokensIn: number; tokensOut: number; refused?: boolean; failed?: boolean };

export const errorCodeMessages: Record<string, string> = {
  VALIDATION_ERROR: 'Invalid request payload',
  CONFLICT: 'Request conflict. Please verify current data state',
  INTERNAL_ERROR: 'Unexpected server error. Please try again later',
  UNAUTHORIZED: 'Invalid username or password',
  FORBIDDEN: 'Insufficient permission for this action',
  NOT_FOUND: 'Requested resource was not found'
};
