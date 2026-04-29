import Link from 'next/link';
import { healthCheck } from '@/lib/api';

export default async function HomePage() {
  const health = await healthCheck().catch(() => null);

  return (
    <main className="page-wrap page-center">
      <section className="card space-y-4">
        <h1 className="text-2xl font-bold tracking-tight">Shequyiyuan Web</h1>
        <p className="hint">前端已对接后端 Spring Boot 3 JWT 认证接口。</p>

        <div className="flex flex-wrap gap-3">
          <Link className="btn" href="/login">
            登录页面
          </Link>
          <Link className="btn-secondary" href="/dashboard">
            Dashboard
          </Link>
        </div>

        <div className="rounded-xl border border-slate-200 bg-slate-50 p-4">
          <h2 className="mb-2 text-sm font-semibold text-slate-700">后端健康检查</h2>
          {health ? (
            <pre className="overflow-auto rounded-lg bg-slate-900 p-3 text-xs text-slate-100">
              {JSON.stringify(health, null, 2)}
            </pre>
          ) : (
            <p className="error">无法连接后端健康检查接口，请确认 NEXT_PUBLIC_API_BASE_URL 配置。</p>
          )}
        </div>
      </section>
    </main>
  );
}
