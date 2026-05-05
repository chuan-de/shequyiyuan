import Link from 'next/link';
import { healthCheck } from '@/lib/api';

export default async function HomePage() {
  const health = await healthCheck().catch(() => null);

  return (
    <main className="page-wrap page-center">
      <section className="card space-y-4">
        <h1 className="text-2xl font-bold tracking-tight">Shequyiyuan Web</h1>
        <p className="hint">Frontend is connected to Spring Boot 3 JWT authentication APIs.</p>

        <div className="flex flex-wrap gap-3">
          <Link className="btn" href="/login">
            Sign In
          </Link>
          <Link className="btn-secondary" href="/register">
            Register
          </Link>
          <Link className="btn-secondary" href="/dashboard">
            Dashboard
          </Link>
        </div>

        <div className="rounded-xl border border-slate-200 bg-slate-50 p-4">
          <h2 className="mb-2 text-sm font-semibold text-slate-700">Backend Health Check</h2>
          {health ? (
            <pre className="overflow-auto rounded-lg bg-slate-900 p-3 text-xs text-slate-100">
              {JSON.stringify(health, null, 2)}
            </pre>
          ) : (
            <p className="error">Cannot reach backend health endpoint. Check NEXT_PUBLIC_API_BASE_URL.</p>
          )}
        </div>
      </section>
    </main>
  );
}
