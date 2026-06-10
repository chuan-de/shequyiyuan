'use client';

import { FormEvent, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { register } from '@/lib/api';
import { AuthLayout } from '@/components/layout/auth-layout';
import { minLength, required, sameAs, validateForm } from '@/components/ui/form-validator';

export default function RegisterPage() {
  const router = useRouter();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const errors = validateForm(
      { username, password, confirmPassword },
      {
        username: [required('账号')],
        password: [required('密码'), minLength('密码', 6)],
        confirmPassword: [required('确认密码'), sameAs('确认密码', 'password')],
      },
    );
    if (Object.keys(errors).length > 0) {
      setError(Object.values(errors)[0]);
      return;
    }

    setLoading(true);
    setError('');

    try {
      await register({ username, password });
      router.push('/login');
    } catch (submitError) {
      const message = submitError instanceof Error ? submitError.message : '注册失败，请稍后重试';
      setError(message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthLayout>
      <div className="space-y-1.5">
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">创建账号</h1>
        <p className="text-sm text-slate-500">注册完成后即可登录系统</p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-5">
        <div>
          <label className="label" htmlFor="username">账号</label>
          <input
            id="username"
            className="input"
            placeholder="请输入账号"
            autoComplete="username"
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            required
          />
        </div>

        <div>
          <label className="label" htmlFor="password">密码</label>
          <input
            id="password"
            className="input"
            type="password"
            placeholder="至少 6 位"
            autoComplete="new-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
        </div>

        <div>
          <label className="label" htmlFor="confirm-password">确认密码</label>
          <input
            id="confirm-password"
            className="input"
            type="password"
            placeholder="再次输入密码"
            autoComplete="new-password"
            value={confirmPassword}
            onChange={(event) => setConfirmPassword(event.target.value)}
            required
          />
        </div>

        {error && (
          <div className="flex items-center gap-2 rounded-xl bg-rose-50 px-3 py-2.5 text-sm text-rose-700 ring-1 ring-rose-200">
            <svg className="h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m0 3.75h.008v.008H12v-.008zM21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            {error}
          </div>
        )}

        <button
          type="submit"
          disabled={loading}
          className="btn w-full gap-2 py-3 text-base font-semibold shadow-lg shadow-brand/25 hover:shadow-brand/35"
        >
          {loading ? '注册中…' : '注 册'}
        </button>
      </form>

      <div className="flex items-center gap-3 text-xs text-slate-400">
        <span className="h-px flex-1 bg-slate-200" />
        已经有账号了？
        <span className="h-px flex-1 bg-slate-200" />
      </div>

      <Link
        href="/login"
        className="btn-secondary block w-full py-2.5 text-center text-sm font-medium"
      >
        返回登录
      </Link>
    </AuthLayout>
  );
}
