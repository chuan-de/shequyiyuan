'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { AppShell } from '@/components/layout/app-shell';
import { Card } from '@/components/ui/card';
import { LinkButton, Button } from '@/components/ui/button';
import { CurrentUserResponse, currentUser } from '@/lib/api';
import { readToken, clearToken } from '@/lib/token-storage';
import { hasPermission } from '@/lib/permissions';

export default function DashboardPage() {
  const router = useRouter();
  const [user, setUser] = useState<CurrentUserResponse | null>(null);
  const [tokenPreview, setTokenPreview] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const token = readToken();
    if (!token) {
      router.replace('/login');
      return;
    }

    setTokenPreview(token.accessToken.slice(0, 40));

    currentUser(token.accessToken)
      .then(setUser)
      .catch(() => {
        setError('用户信息获取失败，请重新登录');
        clearToken();
        router.replace('/login');
      })
      .finally(() => setLoading(false));
  }, [router]);

  if (loading) {
    return (
      <AppShell title="Dashboard" description="系统状态与账号信息。">
        <Card>加载中...</Card>
      </AppShell>
    );
  }

  return (
    <AppShell title="Dashboard" description="已完成前后端认证联调（login/register/me）。">
      <Card className="space-y-4">
        {user && (
          <div className="rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-700">
            <p>
              当前用户：<strong>{user.username}</strong>
            </p>
            <p>状态：{user.enabled ? '启用' : '停用'}</p>
            <p>角色：{user.roles.join(', ') || '无'}</p>
            <p>权限：{user.permissions.join(', ') || '无'}</p>
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
          {hasPermission(user, 'dictionary:read') ? (
            <LinkButton variant="secondary" href="/dictionaries">
              字典管理
            </LinkButton>
          ) : null}
          <Button
            onClick={() => {
              clearToken();
              router.replace('/login');
            }}
          >
            退出登录
          </Button>
        </div>
      </Card>
    </AppShell>
  );
}
