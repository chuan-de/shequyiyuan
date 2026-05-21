'use client';
import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { AppShell } from '@/components/layout/app-shell';
import { CurrentUserResponse, currentUser } from '@/lib/api';
import { readToken } from '@/lib/token-storage';

export default function DashboardPage() {
  const router = useRouter();
  const [user, setUser] = useState<CurrentUserResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = readToken();
    if (!token) { router.replace('/login'); return; }
    currentUser(token.accessToken)
      .then(setUser)
      .catch(() => router.replace('/login'))
      .finally(() => setLoading(false));
  }, [router]);

  return (
    <AppShell title="首页">
      {loading ? (
        <div className="table-wrap p-12 text-center hint">加载中…</div>
      ) : (
        <div className="space-y-6">
          <div className="table-wrap p-8">
            <p className="text-xl font-bold text-slate-900 mb-1">
              欢迎回来，{user?.username} 👋
            </p>
            <p className="hint">社区医院管理系统 · 请从左侧菜单选择功能模块</p>
          </div>
          <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
            <div className="table-wrap p-5">
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-400 mb-1">用户名</p>
              <p className="text-lg font-bold text-slate-800">{user?.username}</p>
            </div>
            <div className="table-wrap p-5">
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-400 mb-1">状态</p>
              <span className="badge badge-green">{user?.enabled ? '启用' : '禁用'}</span>
            </div>
            <div className="table-wrap p-5">
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-400 mb-1">角色</p>
              <p className="text-sm font-medium text-slate-800">{user?.roles?.join(', ') || '-'}</p>
            </div>
            <div className="table-wrap p-5">
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-400 mb-1">权限数</p>
              <p className="text-lg font-bold text-slate-800">{user?.permissions?.length ?? 0}</p>
            </div>
          </div>
        </div>
      )}
    </AppShell>
  );
}
