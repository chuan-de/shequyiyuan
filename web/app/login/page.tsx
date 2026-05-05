'use client';

import { FormEvent, useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { login } from '@/lib/api';
import { writeToken, readToken } from '@/lib/token-storage';
import { AuthLayout } from '@/components/layout/auth-layout';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';

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
    setLoading(true);
    setError('');

    try {
      const result = await login({ username, password });
      writeToken({ accessToken: result.accessToken, tokenType: result.tokenType });
      router.push('/dashboard');
    } catch (submitError) {
      const message = submitError instanceof Error ? submitError.message : '登录失败';
      setError(message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthLayout>
      <div className="space-y-2">
        <h1 className="text-2xl font-bold tracking-tight">登录</h1>
        <p className="hint">使用后端 /api/v1/auth/login 接口获取 JWT。</p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="label" htmlFor="username">用户名</label>
          <Input id="username" value={username} onChange={event => setUsername(event.target.value)} required />
        </div>

        <div>
          <label className="label" htmlFor="password">密码</label>
          <Input id="password" type="password" value={password} onChange={event => setPassword(event.target.value)} required />
        </div>

        {error && <p className="error">{error}</p>}

        <Button className="w-full" type="submit" disabled={loading}>
          {loading ? '登录中...' : '登录'}
        </Button>
      </form>

      <p className="hint">
        还没有账号？
        <Link className="ml-2 text-brand hover:underline" href="/register">去注册</Link>
      </p>
    </AuthLayout>
  );
}
