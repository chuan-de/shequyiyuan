import { ReactNode } from 'react';

const FEATURES = [
  { icon: '🩺', title: '一体化诊疗管理', desc: '就诊、病历、健康档案全流程数字化' },
  { icon: '🤖', title: 'AI 智能辅助', desc: '病历识别 / 患者智能问询 / 社区 AI 问诊' },
  { icon: '🔐', title: '细粒度权限', desc: '患者、医生、前台、管理员分角色管控' },
];

export function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <main className="flex min-h-screen bg-slate-50">
      {/* 左侧品牌区 — 移动端隐藏 */}
      <aside className="relative hidden w-[46%] flex-col justify-between overflow-hidden bg-gradient-to-br from-blue-700 via-blue-600 to-sky-500 p-12 text-white lg:flex">
        {/* 装饰圆 */}
        <div className="pointer-events-none absolute -left-24 -top-24 h-72 w-72 rounded-full bg-white/10" />
        <div className="pointer-events-none absolute -bottom-32 -right-20 h-96 w-96 rounded-full bg-white/10" />
        <div className="pointer-events-none absolute right-16 top-24 h-24 w-24 rounded-full bg-white/10" />

        <div className="relative flex items-center gap-3">
          <span className="grid h-11 w-11 place-items-center rounded-2xl bg-white/15 text-2xl shadow-inner backdrop-blur">
            🏥
          </span>
          <div>
            <div className="text-lg font-bold tracking-wide">社区医院管理系统</div>
            <div className="text-xs text-blue-100/80">Community Hospital Management</div>
          </div>
        </div>

        <div className="relative space-y-8">
          <h2 className="text-3xl font-bold leading-snug">
            让社区医疗服务
            <br />
            更智能、更高效
          </h2>
          <ul className="space-y-5">
            {FEATURES.map((f) => (
              <li key={f.title} className="flex items-start gap-4">
                <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-white/15 text-lg backdrop-blur">
                  {f.icon}
                </span>
                <div>
                  <div className="text-sm font-semibold">{f.title}</div>
                  <div className="mt-0.5 text-xs leading-relaxed text-blue-100/80">{f.desc}</div>
                </div>
              </li>
            ))}
          </ul>
        </div>

        <p className="relative text-xs text-blue-100/60">
          © {new Date().getFullYear()} 社区医院管理系统 · 仅供内部使用
        </p>
      </aside>

      {/* 右侧表单区 */}
      <section className="flex flex-1 items-center justify-center px-4 py-10 md:px-8">
        <div className="w-full max-w-md">
          {/* 移动端顶部 logo */}
          <div className="mb-8 flex items-center justify-center gap-2.5 lg:hidden">
            <span className="grid h-10 w-10 place-items-center rounded-xl bg-blue-600 text-xl text-white">🏥</span>
            <span className="text-lg font-bold text-slate-900">社区医院管理系统</span>
          </div>

          <div className="space-y-6 rounded-2xl bg-white p-8 shadow-soft ring-1 ring-slate-200/60 md:p-10">
            {children}
          </div>
        </div>
      </section>
    </main>
  );
}
