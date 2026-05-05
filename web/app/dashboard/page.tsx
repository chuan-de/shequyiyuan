'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { CurrentUserResponse, currentUser } from '@/lib/api';
import { LinkButton } from '@/components/ui/button';

export default function DashboardPage() {
  const { user, tokenPreview, loading } = useRequireAuth();

  if (loading) {
    return (
      <AppShell user={null}>
        <Card>加载中...</Card>
      </AppShell>
    );
  }

  return (
    <AppShell user={user}>
      <Card className="space-y-4">
        <h1 className="text-2xl font-bold tracking-tight">Dashboard</h1>
        <p className="hint">已完成前后端认证联调（login/register/me）。</p>

        {user && (
          <div className="rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-700">
            <p>
              当前用户：<strong>{user.username}</strong>
            </p>
            <p>状态：{user.enabled ? '启用' : '停用'}</p>
            <p>角色：{user.roles.join(', ') || '无'}</p>
          </div>
        )}

        <p className="text-sm text-slate-700">当前 Token（前 40 字符）：</p>
        <code className="block overflow-auto rounded-lg bg-slate-900 p-3 text-xs text-slate-100">
          {tokenPreview || '未登录或未获取 token'}
        </code>

        {error && <p className="error">{error}</p>}

        <div className="flex flex-wrap gap-3">
          <LinkButton variant="secondary" href="/login">
            去登录页
          </LinkButton>
          <LinkButton variant="secondary" href="/dictionaries">
            字典管理
          </LinkButton>
          <button
            className="btn"
            onClick={() => {
              localStorage.removeItem('access_token');
              localStorage.removeItem('token_type');
              router.replace('/login');
            }}
          >
            退出登录
          </button>
        </div>
      </section>
    </main>
  );
}
