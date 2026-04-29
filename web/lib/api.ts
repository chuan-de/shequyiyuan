const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';

export type LoginPayload = {
  username: string;
  password: string;
};

export type AuthResponse = {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
};

export async function login(payload: LoginPayload): Promise<AuthResponse> {
  const response = await fetch(`${API_BASE_URL}/api/v1/auth/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || 'Login failed');
  }

  return response.json();
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
