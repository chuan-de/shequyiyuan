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
    <aside className="relative flex w-60 flex-shrink-0 flex-col overflow-hidden bg-gradient-to-b from-blue-700 via-blue-600 to-sky-600">
      {/* 装饰圆 — 与登录页品牌区呼应 */}
      <div className="pointer-events-none absolute -left-20 -top-20 h-56 w-56 rounded-full bg-white/10" />
      <div className="pointer-events-none absolute -bottom-24 -right-16 h-64 w-64 rounded-full bg-white/10" />

      <div className="relative flex items-center gap-2.5 px-5 py-5">
        <span className="grid h-9 w-9 place-items-center rounded-xl bg-white/15 text-lg shadow-inner backdrop-blur">
          🏥
        </span>
        <div>
          <div className="text-sm font-bold tracking-wide text-white">社区医院管理系统</div>
          <div className="text-[10px] text-blue-100/70">Community Hospital</div>
        </div>
      </div>

      <nav className="relative flex-1 space-y-0.5 overflow-y-auto p-3">
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

      <div className="relative border-t border-white/15 p-4">
        <div className="flex items-center gap-2.5 rounded-xl bg-white/10 p-2.5 backdrop-blur">
          <div className="grid h-8 w-8 shrink-0 place-items-center rounded-full bg-white text-sm font-bold text-blue-700">
            {username.charAt(0).toUpperCase()}
          </div>
          <span className="flex-1 truncate text-sm font-medium text-white">{username}</span>
          <button
            onClick={handleLogout}
            title="退出登录"
            aria-label="退出登录"
            className="grid h-7 w-7 place-items-center rounded-lg text-blue-100/80 transition hover:bg-white/15 hover:text-white"
          >
            <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15m3 0l3-3m0 0l-3-3m3 3H9" />
            </svg>
          </button>
        </div>
      </div>
    </aside>
  );
}
