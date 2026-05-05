const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';

export type LoginPayload = {
  username: string;
  password: string;
};

export type RegisterPayload = {
  username: string;
  password: string;
};

export type AuthResponse = {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
};

export type CurrentUserResponse = {
  username: string;
  enabled: boolean;
  roles: string[];
};

export type DictionaryResponse = {
  code: string;
  name: string;
};

export type DictionaryItemResponse = {
  id: number;
  name: string;
  value: string;
  sortOrder: number;
  enabled: boolean;
};

async function parseError(response: Response): Promise<Error> {
  const text = await response.text();

  try {
    const parsed = JSON.parse(text) as Partial<ApiErrorResponse>;
    if (parsed && typeof parsed.message === 'string') {
      const details = Array.isArray(parsed.details) && parsed.details.length > 0
        ? `: ${parsed.details.join(', ')}`
        : '';
      const code = parsed.errorCode ? `[${parsed.errorCode}] ` : '';
      return new Error(`${code}${parsed.message}${details}`);
    }
  } catch {
    // ignore json parse errors and fallback to plain text
  }

  return new Error(text || `Request failed with status ${response.status}`);
}

async function unwrapResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw await parseError(response);
  }

  const result = (await response.json()) as ApiResponse<T>;
  if (!result.success) {
    throw new Error(result.message || 'Request failed');
  }

  return result.data;
}

export async function login(payload: LoginPayload): Promise<AuthResponse> {
  const response = await fetch(`${API_BASE_URL}/api/v1/auth/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });

  return unwrapResponse<AuthResponse>(response);
}

export async function register(payload: RegisterPayload): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/api/v1/auth/register`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });

  await unwrapResponse<Record<string, unknown>>(response);
}

export async function healthCheck(): Promise<{ status: string; service: string; timestamp: string }> {
  const response = await fetch(`${API_BASE_URL}/api/v1/health`, {
    cache: 'no-store',
  });

  return unwrapResponse<{ status: string; service: string; timestamp: string }>(response);
}

export async function currentUser(token: string): Promise<CurrentUserResponse> {
  const response = await fetch(`${API_BASE_URL}/api/v1/auth/me`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
    cache: 'no-store',
  });

  return unwrapResponse<CurrentUserResponse>(response);
}

export async function listDictionaries(token: string): Promise<DictionaryResponse[]> {
  const response = await fetch(`${API_BASE_URL}/api/v1/dictionaries`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
    cache: 'no-store',
  });

  if (!response.ok) {
    throw await parseError(response);
  }

  return response.json();
}

export async function listDictionaryItems(token: string, dictCode: string): Promise<DictionaryItemResponse[]> {
  const response = await fetch(`${API_BASE_URL}/api/v1/dictionaries/${dictCode}/items`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
    cache: 'no-store',
  });

  if (!response.ok) {
    throw await parseError(response);
  }

  return response.json();
}
