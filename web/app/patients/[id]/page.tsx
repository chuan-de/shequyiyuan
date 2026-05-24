'use client';

import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { AppShell } from '@/components/layout/app-shell';
import { PatientAiAskPanel } from '@/components/business/patient-ai-ask-panel';
import { API_ROUTES } from '@/lib/api-contract';
import { currentUser, getEntity, type CurrentUserResponse, type EntityRecord } from '@/lib/api';
import { hasPermission } from '@/lib/permissions';
import { readToken } from '@/lib/token-storage';

/**
 * Doctor / admin view of a single patient with an AI 问询 sidebar.
 *
 * Kept intentionally minimal — full patient detail (history list, family
 * members, etc.) is out of scope for Phase 2; the page exists primarily as a
 * mount point for {@link PatientAiAskPanel}. When more patient detail is
 * needed, drop it into the left column without touching the AI panel.
 */
export default function PatientDetailPage() {
  const params = useParams<{ id: string }>();
  const patientId = params?.id;

  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<CurrentUserResponse | null>(null);
  const [patient, setPatient] = useState<EntityRecord | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!patientId) return;
    setLoading(true);
    setError(null);
    try {
      const stored = readToken();
      if (!stored) {
        setError('请先登录');
        return;
      }
      setToken(stored.accessToken);
      const [me, row] = await Promise.all([
        currentUser(stored.accessToken),
        getEntity(stored.accessToken, API_ROUTES.patients, Number(patientId)),
      ]);
      setUser(me);
      setPatient(row);
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载患者信息失败');
    } finally {
      setLoading(false);
    }
  }, [patientId]);

  useEffect(() => { void load(); }, [load]);

  const canAskAi = hasPermission(user, 'ai:patient-rag');
  const fullName = patient?.fullName as string | undefined;
  const username = patient?.username as string | undefined;
  const phone = patient?.phone as string | undefined;
  const idNumber = patient?.idNumber as string | undefined;
  const email = patient?.email as string | undefined;

  return (
    <AppShell title={fullName ? `患者：${fullName}` : '患者详情'} description="查看患者基本信息并发起 AI 智能问询">
      <div className="mb-4">
        <Link href="/patients" className="text-sm text-blue-600 hover:underline">← 返回患者列表</Link>
      </div>

      {loading && (
        <div className="rounded-xl border border-slate-200 bg-white px-4 py-6 text-sm text-slate-500">
          加载中…
        </div>
      )}
      {error && (
        <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          {error}
        </div>
      )}

      {!loading && !error && patient && (
        <div className="grid gap-4 lg:grid-cols-3">
          <section className="rounded-xl border border-slate-200 bg-white p-5 lg:col-span-2">
            <h2 className="mb-3 text-sm font-semibold text-slate-900">基本信息</h2>
            <dl className="grid grid-cols-2 gap-x-6 gap-y-3 text-sm">
              <div>
                <dt className="text-slate-500">账号</dt>
                <dd className="mt-0.5 text-slate-800">{username ?? '—'}</dd>
              </div>
              <div>
                <dt className="text-slate-500">姓名</dt>
                <dd className="mt-0.5 text-slate-800">{fullName ?? '—'}</dd>
              </div>
              <div>
                <dt className="text-slate-500">手机号</dt>
                <dd className="mt-0.5 text-slate-800">{phone ?? '—'}</dd>
              </div>
              <div>
                <dt className="text-slate-500">身份证号</dt>
                <dd className="mt-0.5 text-slate-800">{idNumber ?? '—'}</dd>
              </div>
              <div className="col-span-2">
                <dt className="text-slate-500">邮箱</dt>
                <dd className="mt-0.5 text-slate-800">{email ?? '—'}</dd>
              </div>
            </dl>
          </section>

          <aside className="h-[640px] lg:col-span-1">
            {canAskAi && token ? (
              <PatientAiAskPanel
                token={token}
                patientId={Number(patientId)}
                patientName={fullName}
              />
            ) : (
              <div className="flex h-full items-center justify-center rounded-xl border border-dashed border-slate-300 bg-slate-50 px-4 text-center text-xs text-slate-500">
                当前账号无 AI 问询权限（ai:patient-rag）
              </div>
            )}
          </aside>
        </div>
      )}
    </AppShell>
  );
}
