'use client';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { clearToken, readToken } from '@/lib/token-storage';
import { currentUser } from '@/lib/api';
import { useEffect, useState } from 'react';

const NAV_ITEMS = [
  { href: '/dashboard', label: '首页', icon: '⊞', permission: null },
  { href: '/medications', label: '药品管理', icon: '💊', permission: 'medications:read' },
  { href: '/doctors', label: '医生管理', icon: '👨‍⚕️', permission: 'doctors:read' },
  { href: '/visits', label: '就诊记录', icon: '📋', permission: 'visits:read' },
  { href: '/family-doctors', label: '家庭医生', icon: '🏠', permission: 'family-doctors:read' },
  { href: '/medical-records', label: '病历管理', icon: '🗂️', permission: 'medical-records:read' },
  { href: '/health-records', label: '健康档案', icon: '❤️', permission: 'health-records:read' },
  { href: '/configs', label: '系统配置', icon: '⚙️', permission: 'configs:read' },
  { href: '/dictionaries', label: '数据字典', icon: '📖', permission: 'dictionary:read' },
  { href: '/departments', label: '科室管理', icon: '🏥', permission: 'departments:read' },
  { href: '/patients', label: '患者管理', icon: '🧑‍⚕️', permission: 'patients:read' },
  { href: '/receptions', label: '前台管理', icon: '🖥️', permission: 'receptions:read' },
  { href: '/ai-consult', label: '社区 AI 问诊', icon: '🤖', permission: 'ai:consult' },
];

export function Sidebar() {
  const pathname = usePathname();
  const router = useRouter();
  const [username, setUsername] = useState('');
  const [permissions, setPermissions] = useState<string[] | null>(null);

  useEffect(() => {
    const token = readToken();
    if (!token) return;

    const raw = token.accessToken.split('.')[1];
    try {
      const decoded = JSON.parse(atob(raw));
      setUsername(decoded.sub ?? '用户');
    } catch {
      setUsername('用户');
    }

    currentUser(token.accessToken)
      .then(u => setPermissions(u.permissions))
      .catch(() => setPermissions([]));
  }, []);

  const visibleItems = NAV_ITEMS.filter(item =>
    item.permission === null || permissions === null || permissions.includes(item.permission)
  );

  function handleLogout() {
    clearToken();
    router.replace('/login');
  }

  return (
    <aside className="flex w-60 flex-shrink-0 flex-col bg-slate-900">
      <div className="flex items-center gap-2 border-b border-slate-700 px-5 py-4">
        <span className="text-lg font-bold text-white">社区医院</span>
        <span className="rounded bg-brand px-1.5 py-0.5 text-xs font-semibold text-white">管理</span>
      </div>

      <nav className="flex-1 overflow-y-auto p-3 space-y-0.5">
        {visibleItems.map((item) => (
          <Link
            key={item.href}
            href={item.href}
            className={`nav-link ${pathname === item.href ? 'nav-link-active' : ''}`}
          >
            <span className="text-base leading-none">{item.icon}</span>
            <span>{item.label}</span>
          </Link>
        ))}
      </nav>

      <div className="border-t border-slate-700 p-4">
        <div className="mb-3 flex items-center gap-2">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-brand text-sm font-bold text-white">
            {username.charAt(0).toUpperCase()}
          </div>
          <span className="text-sm font-medium text-slate-300 truncate">{username}</span>
        </div>
        <button
          onClick={handleLogout}
          className="w-full rounded-lg px-3 py-2 text-left text-sm text-slate-400 transition hover:bg-slate-800 hover:text-rose-400"
        >
          退出登录
        </button>
      </div>
    </aside>
  );
}
