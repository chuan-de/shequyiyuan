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
  return new Error(text || `Request failed with status ${response.status}`);
}

export async function login(payload: LoginPayload): Promise<AuthResponse> {
  const response = await fetch(`${API_BASE_URL}/api/v1/auth/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw await parseError(response);
  }

  return response.json();
}

export async function register(payload: RegisterPayload): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/api/v1/auth/register`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw await parseError(response);
  }
}

export async function healthCheck(): Promise<{ status: string; service: string; timestamp: string }> {
  const response = await fetch(`${API_BASE_URL}/api/v1/health`, {
    cache: 'no-store',
  });

  if (!response.ok) {
    throw new Error('Backend health check failed');
  }

  return response.json();
}

export async function currentUser(token: string): Promise<CurrentUserResponse> {
  const response = await fetch(`${API_BASE_URL}/api/v1/auth/me`, {
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
