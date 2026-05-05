'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { currentUser, CurrentUserResponse } from '@/lib/api';
import { clearToken, readToken } from '@/lib/token-storage';

export function useRequireAuth() {
  const router = useRouter();
  const [user, setUser] = useState<CurrentUserResponse | null>(null);
  const [tokenPreview, setTokenPreview] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = readToken();
    if (!token?.accessToken) {
      router.replace('/login');
      setLoading(false);
      return;
    }

    setTokenPreview(`${token.accessToken.slice(0, 40)}...`);

    currentUser(token.accessToken)
      .then(setUser)
      .catch(() => {
        clearToken();
        router.replace('/login');
      })
      .finally(() => setLoading(false));
  }, [router]);

  return { user, tokenPreview, loading };
}

export function logout(router: ReturnType<typeof useRouter>) {
  clearToken();
  router.replace('/login');
}
