'use client';

import Link from 'next/link';
import { useMemo } from 'react';

export default function DashboardPage() {
  const token = useMemo(() => {
    if (typeof window === 'undefined') {
      return '';
    }
    return localStorage.getItem('access_token') ?? '';
  }, []);

  return (
    <main className="page-wrap page-center">
      <section className="card space-y-4">
        <h1 className="text-2xl font-bold tracking-tight">Dashboard</h1>
        <p className="hint">已完成前后端认证联调第一步。</p>
        <p className="text-sm text-slate-700">当前 Token（前 40 字符）：</p>
        <code className="block overflow-auto rounded-lg bg-slate-900 p-3 text-xs text-slate-100">
          {token ? `${token.slice(0, 40)}...` : '未登录或未获取 token'}
        </code>

        <div className="flex flex-wrap gap-3">
          <Link className="btn-secondary" href="/login">
            去登录页
          </Link>
          <button
            className="btn"
            onClick={() => {
              localStorage.removeItem('access_token');
              localStorage.removeItem('token_type');
              location.href = '/login';
            }}
          >
            退出登录
          </button>
        </div>
      </section>
    </main>
  );
}
