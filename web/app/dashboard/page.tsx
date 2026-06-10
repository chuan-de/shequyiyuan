'use client';
import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { AppShell } from '@/components/layout/app-shell';
import { CurrentUserResponse, currentUser } from '@/lib/api';
import { useEffectiveConfigs } from '@/lib/system-config';
import { clearToken, readToken } from '@/lib/token-storage';

const ROLE_LABELS: Record<string, string> = {
  ADMIN: '管理员',
  USER: '普通用户',
  PATIENT: '患者',
  RECEPTION: '前台',
  DOCTOR: '医生',
  FAMILY_DOCTOR: '家庭医生',
};

export default function DashboardPage() {
  const router = useRouter();
  const [user, setUser] = useState<CurrentUserResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [token, setToken] = useState('');
  // 系统公告来自「系统配置」模块的 system.announcement，管理员改完刷新即见。
  const configs = useEffectiveConfigs(token);
  const announcement = configs?.['system.announcement']?.trim();

  useEffect(() => {
    const token = readToken();
    if (!token) { router.replace('/login'); return; }
    setToken(token.accessToken);
    currentUser(token.accessToken)
      .then(setUser)
      .catch(() => { clearToken(); router.replace('/login'); })
      .finally(() => setLoading(false));
  }, [router]);

  const stats = [
    {
      icon: '👤',
      label: '当前账号',
      value: user?.username ?? '-',
    },
    {
      icon: '🟢',
      label: '账号状态',
      value: user?.enabled ? '启用' : '禁用',
    },
    {
      icon: '🎖️',
      label: '角色',
      value: user?.roles?.map(r => ROLE_LABELS[r] ?? r).join('、') || '-',
    },
    {
      icon: '🔑',
      label: '权限数',
      value: String(user?.permissions?.length ?? 0),
    },
  ];

  return (
    <AppShell title="首页">
      {loading ? (
        <div className="table-wrap p-12 text-center hint">加载中…</div>
      ) : (
        <div className="space-y-6">
          {/* 系统公告（系统配置 system.announcement，空则不显示） */}
          {announcement && (
            <div className="flex items-start gap-3 rounded-2xl bg-amber-50 px-5 py-4 text-sm text-amber-800 ring-1 ring-amber-200">
              <span className="text-base leading-none">📢</span>
              <p className="whitespace-pre-wrap leading-relaxed">{announcement}</p>
            </div>
          )}

          {/* 渐变欢迎卡 — 与登录页品牌区同款 */}
          <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-blue-700 via-blue-600 to-sky-500 p-8 text-white shadow-soft">
            <div className="pointer-events-none absolute -right-16 -top-20 h-56 w-56 rounded-full bg-white/10" />
            <div className="pointer-events-none absolute -bottom-24 right-32 h-48 w-48 rounded-full bg-white/10" />
            <p className="relative text-2xl font-bold">
              欢迎回来，{user?.username} 👋
            </p>
            <p className="relative mt-1.5 text-sm text-blue-100/90">
              社区医院管理系统 · 请从左侧菜单选择功能模块
            </p>
          </div>

          <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
            {stats.map((s) => (
              <div key={s.label} className="card flex items-center gap-4 !p-5">
                <span className="grid h-11 w-11 shrink-0 place-items-center rounded-xl bg-blue-50 text-xl">
                  {s.icon}
                </span>
                <div className="min-w-0">
                  <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">{s.label}</p>
                  <p className="truncate text-base font-bold text-slate-800">{s.value}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </AppShell>
  );
}
