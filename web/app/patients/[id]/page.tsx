'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { AppShell } from '@/components/layout/app-shell';
import { PatientAiAskPanel } from '@/components/business/patient-ai-ask-panel';
import { API_ROUTES } from '@/lib/api-contract';
import { currentUser, getEntity, listEntities, type CurrentUserResponse, type EntityRecord } from '@/lib/api';
import { useDictionaries } from '@/lib/dictionaries';
import { hasPermission } from '@/lib/permissions';
import { readToken } from '@/lib/token-storage';

/**
 * 患者 360° 视图：基本信息 + 家医签约 + 健康指标趋势 + 就诊/健康档案时间线，
 * 右侧挂 AI 问询面板。各分区按权限独立加载，缺权限时整块隐藏。
 */
export default function PatientDetailPage() {
  const params = useParams<{ id: string }>();
  const patientId = params?.id;

  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<CurrentUserResponse | null>(null);
  const [patient, setPatient] = useState<EntityRecord | null>(null);
  const [contracts, setContracts] = useState<EntityRecord[]>([]);
  const [followups, setFollowups] = useState<EntityRecord[]>([]);
  const [visits, setVisits] = useState<EntityRecord[]>([]);
  const [healthRecords, setHealthRecords] = useState<EntityRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const dicts = useDictionaries(token ?? '', ['sex_types']);

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
      const t = stored.accessToken;
      setToken(t);
      const me = await currentUser(t);
      setUser(me);
      const pid = Number(patientId);

      // 各分区按权限拉取，单个失败不拖垮整页。
      const safe = <T,>(p: Promise<T>, fallback: T) => p.catch(() => fallback);
      const empty = { records: [] as EntityRecord[], total: 0, page: 1, size: 0 };
      const [row, contractRes, followupRes, visitRes, healthRes] = await Promise.all([
        getEntity(t, API_ROUTES.patients, pid),
        hasPermission(me, 'family-doctor-contracts:read')
          ? safe(listEntities(t, API_ROUTES.familyDoctorContracts, { patientId: pid, page: 1, size: 50 }), empty)
          : Promise.resolve(empty),
        hasPermission(me, 'followups:read')
          ? safe(listEntities(t, API_ROUTES.followups, { patientId: pid, page: 1, size: 200 }), empty)
          : Promise.resolve(empty),
        hasPermission(me, 'visits:read')
          ? safe(listEntities(t, API_ROUTES.visits, { page: 1, size: 500 }), empty)
          : Promise.resolve(empty),
        hasPermission(me, 'health-records:read')
          ? safe(listEntities(t, API_ROUTES.healthRecords, { page: 1, size: 500 }), empty)
          : Promise.resolve(empty),
      ]);
      setPatient(row);
      setContracts(contractRes.records);
      setFollowups(followupRes.records);
      // 就诊/健康档案接口暂无 patientId 过滤参数，前端按患者过滤。
      setVisits(visitRes.records.filter(v => Number(v.patientId) === pid));
      setHealthRecords(healthRes.records.filter(h => Number(h.patientId) === pid));
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载患者信息失败');
    } finally {
      setLoading(false);
    }
  }, [patientId]);

  useEffect(() => { void load(); }, [load]);

  const canAskAi = hasPermission(user, 'ai:patient-rag');

  const str = (k: string) => {
    const v = patient?.[k];
    return v == null || v === '' ? null : String(v);
  };
  const fullName = str('fullName');
  const sexLabel = patient?.sexTypes != null ? (dicts.labelOf('sex_types', patient.sexTypes) ?? String(patient.sexTypes)) : null;
  const age = useMemo(() => {
    const bd = str('birthDate');
    if (!bd) return null;
    const d = new Date(bd);
    if (isNaN(d.getTime())) return null;
    const now = new Date();
    let a = now.getFullYear() - d.getFullYear();
    if (now.getMonth() < d.getMonth() || (now.getMonth() === d.getMonth() && now.getDate() < d.getDate())) a--;
    return a >= 0 ? a : null;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [patient]);

  const activeContract = contracts.find(c => c.status === 'ACTIVE') ?? null;
  // followups 接口按测量时间倒序返回；趋势图需要时间正序。
  const trendData = useMemo(() => [...followups].reverse(), [followups]);
  const latestFollowup = followups[0] ?? null;

  const timeline = useMemo(() => {
    const items: { time: string; kind: '就诊' | '健康档案'; title: string; href: string }[] = [];
    visits.forEach(v => items.push({
      time: String(v.visitDate ?? v.createdAt ?? ''),
      kind: '就诊',
      title: `就诊号 ${v.visitNumber ?? '—'}${v.fee != null ? ` · 费用 ¥${Number(v.fee).toFixed(2)}` : ''}${v.registrationNotes ? ` · ${v.registrationNotes}` : ''}`,
      href: '/visits',
    }));
    healthRecords.forEach(h => items.push({
      time: String(h.recordedAt ?? h.createdAt ?? ''),
      kind: '健康档案',
      title: String(h.title ?? '—'),
      href: '/health-records',
    }));
    return items.filter(i => i.time).sort((a, b) => b.time.localeCompare(a.time)).slice(0, 10);
  }, [visits, healthRecords]);

  return (
    <AppShell title={fullName ? `患者：${fullName}` : '患者详情'} description="患者 360° 视图 — 档案、签约、健康趋势与 AI 问询">
      <div className="mb-4">
        <Link href="/patients" className="text-sm text-blue-600 hover:underline">← 返回患者列表</Link>
      </div>

      {loading && (
        <div className="rounded-xl border border-slate-200 bg-white px-4 py-6 text-sm text-slate-500">加载中…</div>
      )}
      {error && (
        <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">{error}</div>
      )}

      {!loading && !error && patient && (
        <div className="grid gap-4 lg:grid-cols-3">
          <div className="space-y-4 lg:col-span-2">
            {/* 基本信息 */}
            <section className="rounded-xl border border-slate-200 bg-white p-5">
              <div className="mb-4 flex items-center gap-4">
                {str('photoUrl') ? (
                  <img src={str('photoUrl')!} alt="" className="h-16 w-16 rounded-full object-cover ring-2 ring-blue-100" />
                ) : (
                  <div className="grid h-16 w-16 place-items-center rounded-full bg-blue-50 text-2xl">🧑</div>
                )}
                <div>
                  <div className="flex items-center gap-2">
                    <h2 className="text-lg font-semibold text-slate-900">{fullName ?? '—'}</h2>
                    {sexLabel && <span className="rounded-full bg-blue-50 px-2 py-0.5 text-xs text-blue-700">{sexLabel}</span>}
                    {age != null && <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-600">{age} 岁</span>}
                    {patient.enabled === false && <span className="rounded-full bg-rose-50 px-2 py-0.5 text-xs text-rose-600">已禁用</span>}
                  </div>
                  <p className="mt-0.5 text-xs text-slate-500">账号 {str('username') ?? '—'}</p>
                </div>
              </div>

              {str('allergies') && (
                <div className="mb-3 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
                  ⚠️ <span className="font-medium">过敏史：</span>{str('allergies')}
                </div>
              )}

              <dl className="grid grid-cols-2 gap-x-6 gap-y-3 text-sm">
                <div><dt className="text-slate-500">手机号</dt><dd className="mt-0.5 text-slate-800">{str('phone') ?? '—'}</dd></div>
                <div><dt className="text-slate-500">身份证号</dt><dd className="mt-0.5 text-slate-800">{str('idNumber') ?? '—'}</dd></div>
                <div><dt className="text-slate-500">出生日期</dt><dd className="mt-0.5 text-slate-800">{str('birthDate') ?? '—'}</dd></div>
                <div><dt className="text-slate-500">邮箱</dt><dd className="mt-0.5 text-slate-800">{str('email') ?? '—'}</dd></div>
                <div className="col-span-2"><dt className="text-slate-500">住址</dt><dd className="mt-0.5 text-slate-800">{str('address') ?? '—'}</dd></div>
                <div className="col-span-2"><dt className="text-slate-500">既往病史</dt><dd className="mt-0.5 whitespace-pre-wrap text-slate-800">{str('medicalHistory') ?? '—'}</dd></div>
                <div><dt className="text-slate-500">紧急联系人</dt><dd className="mt-0.5 text-slate-800">{str('emergencyContactName') ?? '—'}</dd></div>
                <div><dt className="text-slate-500">紧急联系人电话</dt><dd className="mt-0.5 text-slate-800">{str('emergencyContactPhone') ?? '—'}</dd></div>
              </dl>
            </section>

            {/* 家庭医生签约 */}
            {hasPermission(user, 'family-doctor-contracts:read') && (
              <section className="rounded-xl border border-slate-200 bg-white p-5">
                <div className="mb-3 flex items-center justify-between">
                  <h2 className="text-sm font-semibold text-slate-900">🤝 家庭医生签约</h2>
                  <Link href="/family-doctor-contracts" className="text-xs text-blue-600 hover:underline">管理签约 →</Link>
                </div>
                {activeContract ? (
                  <div className="flex flex-wrap items-center gap-x-8 gap-y-2 text-sm">
                    <div><span className="text-slate-500">家庭医生：</span><span className="font-medium text-slate-800">{String(activeContract.familyDoctorName ?? '—')}</span></div>
                    <div><span className="text-slate-500">服务包：</span><span className="text-slate-800">{String(activeContract.servicePackage ?? '—')}</span></div>
                    <div><span className="text-slate-500">签约日期:</span><span className="text-slate-800"> {String(activeContract.signedAt ?? '—')}</span></div>
                    <div><span className="text-slate-500">到期：</span><span className="text-slate-800">{String(activeContract.expiresAt ?? '长期')}</span></div>
                    <span className="rounded-full bg-emerald-50 px-2 py-0.5 text-xs font-medium text-emerald-700">生效中</span>
                  </div>
                ) : (
                  <p className="text-sm text-slate-400">暂无生效中的签约{contracts.length > 0 ? `（历史签约 ${contracts.length} 份）` : ''}</p>
                )}
              </section>
            )}

            {/* 健康指标趋势 */}
            {hasPermission(user, 'followups:read') && (
              <section className="rounded-xl border border-slate-200 bg-white p-5">
                <div className="mb-3 flex items-center justify-between">
                  <h2 className="text-sm font-semibold text-slate-900">🩺 健康指标（近 {followups.length} 次随访）</h2>
                  <Link href="/followups" className="text-xs text-blue-600 hover:underline">录入随访 →</Link>
                </div>
                {followups.length === 0 ? (
                  <p className="text-sm text-slate-400">暂无随访记录</p>
                ) : (
                  <div className="grid gap-4 sm:grid-cols-3">
                    <TrendCard
                      label="血压 (mmHg)"
                      value={latestFollowup?.systolic != null ? `${latestFollowup.systolic}/${latestFollowup.diastolic ?? '—'}` : '—'}
                      series={trendData.map(f => f.systolic == null ? null : Number(f.systolic))}
                      series2={trendData.map(f => f.diastolic == null ? null : Number(f.diastolic))}
                      warn={latestFollowup?.systolic != null && (Number(latestFollowup.systolic) >= 140 || Number(latestFollowup.diastolic ?? 0) >= 90)}
                    />
                    <TrendCard
                      label="血糖 (mmol/L)"
                      value={latestFollowup?.bloodSugar != null ? String(latestFollowup.bloodSugar) : '—'}
                      series={trendData.map(f => f.bloodSugar == null ? null : Number(f.bloodSugar))}
                      warn={latestFollowup?.bloodSugar != null && Number(latestFollowup.bloodSugar) >= 7}
                    />
                    <TrendCard
                      label={`体重 (kg)${latestFollowup?.bmi != null ? ` · BMI ${latestFollowup.bmi}` : ''}`}
                      value={latestFollowup?.weightKg != null ? String(latestFollowup.weightKg) : '—'}
                      series={trendData.map(f => f.weightKg == null ? null : Number(f.weightKg))}
                      warn={latestFollowup?.bmi != null && Number(latestFollowup.bmi) >= 28}
                    />
                  </div>
                )}
              </section>
            )}

            {/* 就诊 / 健康档案时间线 */}
            {(hasPermission(user, 'visits:read') || hasPermission(user, 'health-records:read')) && (
              <section className="rounded-xl border border-slate-200 bg-white p-5">
                <h2 className="mb-3 text-sm font-semibold text-slate-900">📋 就诊与档案时间线</h2>
                {timeline.length === 0 ? (
                  <p className="text-sm text-slate-400">暂无记录</p>
                ) : (
                  <ol className="space-y-3">
                    {timeline.map((item, i) => (
                      <li key={i} className="flex items-start gap-3 text-sm">
                        <span className={`mt-0.5 rounded-full px-2 py-0.5 text-xs font-medium ${item.kind === '就诊' ? 'bg-blue-50 text-blue-700' : 'bg-emerald-50 text-emerald-700'}`}>
                          {item.kind}
                        </span>
                        <div className="min-w-0 flex-1">
                          <Link href={item.href} className="block truncate text-slate-800 hover:text-blue-600">{item.title}</Link>
                          <span className="text-xs text-slate-400">{item.time.replace('T', ' ').slice(0, 16)}</span>
                        </div>
                      </li>
                    ))}
                  </ol>
                )}
              </section>
            )}
          </div>

          <aside className="h-[640px] lg:col-span-1">
            {canAskAi && token ? (
              <PatientAiAskPanel
                token={token}
                patientId={Number(patientId)}
                patientName={fullName ?? undefined}
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

/** 单指标卡：最新值 + SVG 折线趋势（无第三方图表依赖）。 */
function TrendCard({ label, value, series, series2, warn }: {
  label: string;
  value: string;
  series: (number | null)[];
  series2?: (number | null)[];
  warn?: boolean;
}) {
  return (
    <div className={`rounded-lg border p-3 ${warn ? 'border-amber-200 bg-amber-50/50' : 'border-slate-200 bg-slate-50/50'}`}>
      <div className="text-xs text-slate-500">{label}</div>
      <div className={`mt-0.5 text-xl font-semibold ${warn ? 'text-amber-600' : 'text-slate-900'}`}>
        {value}
        {warn && <span className="ml-1 align-middle text-xs font-normal">⚠️ 偏高</span>}
      </div>
      <Sparkline series={series} series2={series2} />
    </div>
  );
}

function Sparkline({ series, series2 }: { series: (number | null)[]; series2?: (number | null)[] }) {
  const W = 160; const H = 36;
  const all = [...series, ...(series2 ?? [])].filter((v): v is number => v != null);
  if (all.length < 2) return <div className="mt-2 h-9 text-xs leading-9 text-slate-300">数据不足，暂无趋势</div>;
  const min = Math.min(...all); const max = Math.max(...all);
  const span = max - min || 1;
  const toPoints = (s: (number | null)[]) => s
    .map((v, i) => v == null ? null : `${(i / Math.max(s.length - 1, 1)) * W},${H - ((v - min) / span) * (H - 6) - 3}`)
    .filter((p): p is string => p != null)
    .join(' ');
  return (
    <svg viewBox={`0 0 ${W} ${H}`} className="mt-2 h-9 w-full" preserveAspectRatio="none">
      <polyline points={toPoints(series)} fill="none" stroke="#2563eb" strokeWidth="1.5" />
      {series2 && <polyline points={toPoints(series2)} fill="none" stroke="#0ea5e9" strokeWidth="1.5" strokeDasharray="3 2" />}
    </svg>
  );
}
