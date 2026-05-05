'use client';

import { FormEvent, useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { login } from '@/lib/api';
import { writeToken, readToken } from '@/lib/token-storage';
import { AuthLayout } from '@/components/layout/auth-layout';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { minLength, required, validateForm } from '@/components/ui/form-validator';

export default function LoginPage() {
  const router = useRouter();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const token = readToken();
    if (token) {
      router.replace('/dashboard');
    }
  }, [router]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const errors = validateForm({ username, password }, { username: [required('Username')], password: [required('Password'), minLength('Password', 6)] });
    if (Object.keys(errors).length > 0) {
      setError(Object.values(errors)[0]);
      return;
    }
    setLoading(true);
    setError('');

    try {
      const result = await login({ username, password });
      writeToken({ accessToken: result.accessToken, tokenType: result.tokenType });
      router.push('/dashboard');
    } catch (submitError) {
      const message = submitError instanceof Error ? submitError.message : 'Sign in failed';
      setError(message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthLayout>
      <div className="space-y-2">
        <h1 className="text-2xl font-bold tracking-tight">Sign In</h1>
        <p className="hint">Use backend REST endpoint /api/v1/auth/login to get a JWT.</p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="label" htmlFor="username">Username</label>
          <Input id="username" value={username} onChange={event => setUsername(event.target.value)} required />
        </div>

        <div>
          <label className="label" htmlFor="password">Password</label>
          <Input id="password" type="password" value={password} onChange={event => setPassword(event.target.value)} required />
        </div>

        {error && <p className="error">{error}</p>}

        <Button className="w-full" type="submit" disabled={loading}>
          {loading ? 'Signing in...' : 'Sign In'}
        </Button>
      </form>

      <p className="hint">
        No account yet?
        <Link className="ml-2 text-brand hover:underline" href="/register">Create one</Link>
      </p>
    </AuthLayout>
  );
}
