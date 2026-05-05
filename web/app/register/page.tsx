'use client';

import { FormEvent, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { register } from '@/lib/api';
import { AuthLayout } from '@/components/layout/AuthLayout';
import { Input } from '@/components/ui/Input';
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
      setError('两次密码不一致');
      return;
    }

    setLoading(true);
    setError('');

    try {
      await register({ username, password });
      router.push('/login');
    } catch (submitError) {
      const message = submitError instanceof Error ? submitError.message : '注册失败';
      setError(message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthLayout>
      <div className="space-y-2">
        <h1 className="text-2xl font-bold tracking-tight">注册</h1>
        <p className="hint">创建账号后可使用登录页获取 JWT。</p>
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

        <div>
          <label className="label" htmlFor="confirm-password">确认密码</label>
          <Input id="confirm-password" type="password" value={confirmPassword} onChange={event => setConfirmPassword(event.target.value)} required />
        </div>

        {error && <p className="error">{error}</p>}

        <Button className="w-full" type="submit" disabled={loading}>
          {loading ? '注册中...' : '注册'}
        </Button>
      </form>

      <p className="hint">
        已有账号？
        <Link className="ml-2 text-brand hover:underline" href="/login">去登录</Link>
      </p>
    </AuthLayout>
  );
}
