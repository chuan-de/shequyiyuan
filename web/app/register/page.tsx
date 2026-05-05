'use client';

import { FormEvent, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { register } from '@/lib/api';
import { AuthLayout } from '@/components/layout/auth-layout';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';

export default function RegisterPage() {
  const router = useRouter();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (password !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    setLoading(true);
    setError('');

    try {
      await register({ username, password });
      router.push('/login');
    } catch (submitError) {
      const message = submitError instanceof Error ? submitError.message : 'Registration failed';
      setError(message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthLayout>
      <div className="space-y-2">
        <h1 className="text-2xl font-bold tracking-tight">Register</h1>
        <p className="hint">Create an account and then sign in to obtain a JWT.</p>
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

        <div>
          <label className="label" htmlFor="confirm-password">Confirm Password</label>
          <Input id="confirm-password" type="password" value={confirmPassword} onChange={event => setConfirmPassword(event.target.value)} required />
        </div>

        {error && <p className="error">{error}</p>}

        <Button className="w-full" type="submit" disabled={loading}>
          {loading ? 'Registering...' : 'Register'}
        </Button>
      </form>

      <p className="hint">
        Already have an account?
        <Link className="ml-2 text-brand hover:underline" href="/login">Go to Sign In</Link>
      </p>
    </AuthLayout>
  );
}
