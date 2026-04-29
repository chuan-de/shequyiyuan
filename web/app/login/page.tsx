'use client';

import { FormEvent, useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { login } from '@/lib/api';

export default function LoginPage() {
  const router = useRouter();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const token = localStorage.getItem('access_token');
    if (token) {
      router.replace('/dashboard');
    }
  }, [router]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError('');

    try {
      const result = await login({ username, password });
      localStorage.setItem('access_token', result.accessToken);
      localStorage.setItem('token_type', result.tokenType);
      router.push('/dashboard');
    } catch (submitError) {
      const message = submitError instanceof Error ? submitError.message : '登录失败';
      setError(message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="page-wrap page-center">
      <section className="card space-y-6">
        <div className="space-y-2">
          <h1 className="text-2xl font-bold tracking-tight">登录</h1>
          <p className="hint">使用后端 /api/v1/auth/login 接口获取 JWT。</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="label" htmlFor="username">
              用户名
            </label>
            <input
              id="username"
              className="input"
              value={username}
              onChange={event => setUsername(event.target.value)}
              required
            />
          </div>

          <div>
            <label className="label" htmlFor="password">
              密码
            </label>
            <input
              id="password"
              className="input"
              type="password"
              value={password}
              onChange={event => setPassword(event.target.value)}
              required
            />
          </div>

          {error && <p className="error">{error}</p>}

          <button className="btn w-full" type="submit" disabled={loading}>
            {loading ? '登录中...' : '登录'}
          </button>
        </form>

        <p className="hint">
          还没有账号？
          <Link className="ml-2 text-brand hover:underline" href="/register">
            去注册
          </Link>
        </p>
      </section>
    </main>
  );
}
