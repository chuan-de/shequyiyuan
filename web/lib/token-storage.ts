export type StoredToken = {
  accessToken: string;
  tokenType: string;
};

const ACCESS_TOKEN_KEY = 'access_token';
const TOKEN_TYPE_KEY = 'token_type';

export function readToken(): StoredToken | null {
  if (typeof window === 'undefined') {
    return null;
  }

  const accessToken = localStorage.getItem(ACCESS_TOKEN_KEY);
  const tokenType = localStorage.getItem(TOKEN_TYPE_KEY);

  if (!accessToken || !tokenType) {
    return null;
  }

  return { accessToken, tokenType };
}

export function writeToken(token: StoredToken): void {
  if (typeof window === 'undefined') {
    return;
  }

  localStorage.setItem(ACCESS_TOKEN_KEY, token.accessToken);
  localStorage.setItem(TOKEN_TYPE_KEY, token.tokenType);
}

export function clearToken(): void {
  if (typeof window === 'undefined') {
    return;
  }

  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(TOKEN_TYPE_KEY);
}
