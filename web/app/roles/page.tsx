'use client';

import { useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import { AppShell } from '@/components/layout/app-shell';
import { Button } from '@/components/ui/button';
import {
  currentUser, fetchRbacPermissions, fetchRbacRoles, updateRolePermissions,
  type CurrentUserResponse, type RbacPermission, type RbacRole,
} from '@/lib/api';
import { hasPermission } from '@/lib/permissions';
import { readToken } from '@/lib/token-storage';

/** 权限码模块前缀 → 中文分组名（未列出的前缀原样展示）。 */
const MODULE_LABELS: Record<string, string> = {
  'medications': '药品管理',
  'doctors': '医生管理',
  'departments': '科室管理',
  'visits': '就诊记录',
  'family-doctors': '家庭医生',
  'family-doctor-contracts': '家医签约',
  'followups': '慢病随访',
  'medical-records': '病历管理',
  'patients': '患者管理',
  'receptions': '前台管理',
  'dictionary': '数据字典',
  'configs': '系统配置',
  'ai': 'AI 功能',
  'rbac': '角色权限',
  'bingli': '病历（兼容旧码）',
  'jiuankangdangan': '健康档案（已下线）',
};

/** 动作后缀 → 中文标签。 */
const ACTION_LABELS: Record<string, string> = {
  'read': '查看', 'write': '编辑', 'delete': '删除', 'status': '启停',
  'reset-password': '重置密码', 'inventory': '调库存',
  'vision': '病历识别', 'patient-rag': '患者问询', 'consult': '社区问诊', 'admin': '管理',
};

export default function RolePermissionsPage() {
  const router = useRouter();
  const [token, setToken] = useState('');
  const [user, setUser] = useState<CurrentUserResponse | null>(null);
  const [roles, setRoles] = useState<RbacRole[]>([]);
  const [permissions, setPermissions] = useState<RbacPermission[]>([]);
  const [selectedRoleId, setSelectedRoleId] = useState<number | null>(null);
  const [checked, setChecked] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState<{ msg: string; type: 'success' | 'error' } | null>(null);

  function showToast(msg: string, type: 'success' | 'error' = 'success') {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 3000);
  }

  useEffect(() => {
    const stored = readToken();
    if (!stored) { router.replace('/login'); return; }
    const t = stored.accessToken;
    setToken(t);
    Promise.all([currentUser(t), fetchRbacRoles(t), fetchRbacPermissions(t)])
      .then(([me, rs, ps]) => {
        setUser(me);
        setRoles(rs);
        setPermissions(ps);
        const firstEditable = rs.find(r => r.roleCode !== 'ADMIN') ?? rs[0];
        if (firstEditable) {
          setSelectedRoleId(firstEditable.id);
          setChecked(new Set(firstEditable.permissionCodes));
        }
      })
      .catch((e) => showToast(e instanceof Error ? e.message : '加载失败', 'error'))
      .finally(() => setLoading(false));
  }, [router]);

  const selectedRole = roles.find(r => r.id === selectedRoleId) ?? null;
  const isAdminRole = selectedRole?.roleCode === 'ADMIN';
  const canWrite = hasPermission(user, 'rbac:write');
  const dirty = useMemo(() => {
    if (!selectedRole) return false;
    const current = new Set(selectedRole.permissionCodes);
    if (current.size !== checked.size) return true;
    for (const c of checked) if (!current.has(c)) return true;
    return false;
  }, [selectedRole, checked]);

  // 按模块前缀分组（code 形如 module:action）。
  const grouped = useMemo(() => {
    const map = new Map<string, RbacPermission[]>();
    permissions.forEach(p => {
      const prefix = p.code.includes(':') ? p.code.slice(0, p.code.lastIndexOf(':')) : p.code;
      if (!map.has(prefix)) map.set(prefix, []);
      map.get(prefix)!.push(p);
    });
    return Array.from(map.entries()).sort((a, b) => a[0].localeCompare(b[0]));
  }, [permissions]);

  function selectRole(role: RbacRole) {
    if (dirty && !window.confirm('当前修改尚未保存，切换角色将丢弃改动，确定？')) return;
    setSelectedRoleId(role.id);
    setChecked(new Set(role.permissionCodes));
  }

  function toggle(code: string) {
    if (isAdminRole || !canWrite) return;
    setChecked(prev => {
      const next = new Set(prev);
      if (next.has(code)) next.delete(code); else next.add(code);
      return next;
    });
  }

  function toggleGroup(perms: RbacPermission[], allOn: boolean) {
    if (isAdminRole || !canWrite) return;
    setChecked(prev => {
      const next = new Set(prev);
      perms.forEach(p => { if (allOn) next.delete(p.code); else next.add(p.code); });
      return next;
    });
  }

  async function save() {
    if (!selectedRole || !token) return;
    setSaving(true);
    try {
      const updated = await updateRolePermissions(token, selectedRole.id, Array.from(checked));
      setRoles(prev => prev.map(r => r.id === updated.id ? updated : r));
      setChecked(new Set(updated.permissionCodes));
      showToast(`「${updated.roleName}」权限已保存，对在线用户即时生效`);
    } catch (e) {
      showToast(e instanceof Error ? e.message : '保存失败', 'error');
    } finally {
      setSaving(false);
    }
  }

  function actionLabel(code: string): string {
    const action = code.includes(':') ? code.slice(code.lastIndexOf(':') + 1) : code;
    return ACTION_LABELS[action] ?? action;
  }

  return (
    <AppShell title="角色权限" description="配置各角色可访问的模块与操作 — 保存后无需重新登录即时生效">
      {toast && (
        <div className={`fixed right-6 top-6 z-50 flex items-center gap-2 rounded-xl px-4 py-3 text-sm font-medium shadow-lg ${toast.type === 'error' ? 'bg-rose-50 text-rose-700 ring-1 ring-rose-200' : 'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200'}`}>
          <span>{toast.type === 'error' ? '✕' : '✓'}</span>
          <span>{toast.msg}</span>
        </div>
      )}

      {loading ? (
        <div className="table-wrap p-12 text-center hint">加载中…</div>
      ) : (
        <div className="grid gap-4 lg:grid-cols-4">
          {/* 角色列表 */}
          <aside className="space-y-2 lg:col-span-1">
            {roles.map(r => (
              <button
                key={r.id}
                onClick={() => selectRole(r)}
                className={`flex w-full items-center justify-between rounded-xl border px-4 py-3 text-left text-sm transition ${
                  r.id === selectedRoleId
                    ? 'border-blue-300 bg-blue-50 font-semibold text-blue-700'
                    : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50'
                }`}
              >
                <span>
                  {r.roleName}
                  <span className="ml-1.5 text-xs font-normal text-slate-400">{r.roleCode}</span>
                </span>
                <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-500">
                  {r.permissionCodes.length}
                </span>
              </button>
            ))}
          </aside>

          {/* 权限矩阵 */}
          <section className="lg:col-span-3">
            {selectedRole && (
              <div className="card !p-5">
                <div className="mb-4 flex items-center justify-between">
                  <h2 className="text-sm font-semibold text-slate-900">
                    {selectedRole.roleName} 的权限（已选 {checked.size} 项）
                  </h2>
                  {!isAdminRole && canWrite && (
                    <Button onClick={save} disabled={!dirty || saving}>
                      {saving ? '保存中…' : dirty ? '保存修改' : '无改动'}
                    </Button>
                  )}
                </div>

                {isAdminRole && (
                  <div className="mb-4 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-700">
                    🔒 管理员角色拥有全部权限且固定不可修改，以防误操作导致无人能进入本页恢复配置。
                  </div>
                )}
                {!canWrite && !isAdminRole && (
                  <div className="mb-4 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-500">
                    当前账号仅有查看权限（缺少 rbac:write）。
                  </div>
                )}

                <div className="space-y-4">
                  {grouped.map(([prefix, perms]) => {
                    const allOn = perms.every(p => checked.has(p.code));
                    return (
                      <div key={prefix} className="rounded-xl border border-slate-200 p-4">
                        <div className="mb-2 flex items-center justify-between">
                          <h3 className="text-sm font-medium text-slate-800">
                            {MODULE_LABELS[prefix] ?? prefix}
                            <span className="ml-1.5 text-xs font-normal text-slate-400">{prefix}</span>
                          </h3>
                          {!isAdminRole && canWrite && (
                            <button
                              onClick={() => toggleGroup(perms, allOn)}
                              className="text-xs text-blue-600 hover:underline"
                            >
                              {allOn ? '全部取消' : '全选'}
                            </button>
                          )}
                        </div>
                        <div className="flex flex-wrap gap-2">
                          {perms.map(p => {
                            const on = isAdminRole || checked.has(p.code);
                            return (
                              <button
                                key={p.code}
                                onClick={() => toggle(p.code)}
                                disabled={isAdminRole || !canWrite}
                                title={p.code}
                                className={`rounded-lg border px-3 py-1.5 text-sm transition ${
                                  on
                                    ? 'border-blue-300 bg-blue-50 text-blue-700'
                                    : 'border-slate-200 bg-white text-slate-500 hover:bg-slate-50'
                                } ${isAdminRole || !canWrite ? 'cursor-not-allowed opacity-80' : ''}`}
                              >
                                {on ? '✓ ' : ''}{p.name ?? actionLabel(p.code)}
                              </button>
                            );
                          })}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </section>
        </div>
      )}
    </AppShell>
  );
}
