'use client';

import { useCallback, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { AppShell } from '@/components/layout/app-shell';
import Link from 'next/link';
import { Button, buttonVariantClass } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import {
  DictionaryItemResponse, DictionaryResponse, changeDictionaryItemStatus,
  createDictionaryItem, currentUser, listDictionaries, queryDictionaryItems, updateDictionaryItem,
} from '@/lib/api';
import { invalidateDictCache } from '@/lib/dictionaries';
import { hasPermission } from '@/lib/permissions';
import { DataTable, SortState, TableColumn, TablePagination } from '@/components/ui/data-table';
import { Input } from '@/components/ui/input';

type ItemFormState = {
  id: number | null;          // null = 新建
  dictCode: string;
  dictName: string;
  itemCode: string;
  itemName: string;
  sortOrder: string;
};

export default function DictionariesPage() {
  const router = useRouter();
  const [token, setToken] = useState('');
  const [canWrite, setCanWrite] = useState(false);
  const [dictionaries, setDictionaries] = useState<DictionaryResponse[]>([]);
  const [selectedDictionaryCode, setSelectedDictionaryCode] = useState('');
  const [items, setItems] = useState<DictionaryItemResponse[]>([]);
  const [loadingDictionaries, setLoadingDictionaries] = useState(true);
  const [loadingItems, setLoadingItems] = useState(false);
  const [itemName, setItemName] = useState('');
  const [page, setPage] = useState(1);
  const [size] = useState(10);
  const [total, setTotal] = useState(0);
  const [sort, setSort] = useState<SortState>({ field: 'sortOrder', direction: 'asc' });
  const [error, setError] = useState('');
  const [form, setForm] = useState<ItemFormState | null>(null);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const selectedDictionary = dictionaries.find((d) => d.code === selectedDictionaryCode) ?? null;

  const reloadDictionaries = useCallback((t: string) => {
    return listDictionaries(t).then((result) => {
      setDictionaries(result);
      return result;
    });
  }, []);

  useEffect(() => {
    const currentToken = localStorage.getItem('access_token');
    if (!currentToken) {
      router.replace('/login');
      return;
    }

    setToken(currentToken);
    setLoadingDictionaries(true);

    currentUser(currentToken).then((user) => {
      if (!hasPermission(user, 'dictionary:read')) {
        setError('当前账号没有字典查看权限');
        router.replace('/dashboard');
        return;
      }
      setCanWrite(hasPermission(user, 'dictionary:write'));
    }).catch(() => {});

    reloadDictionaries(currentToken)
      .then((result) => {
        if (result.length > 0) setSelectedDictionaryCode(result[0].code);
      })
      .catch(() => {
        setError('加载字典失败，请重新登录');
        localStorage.removeItem('access_token');
        localStorage.removeItem('token_type');
        router.replace('/login');
      })
      .finally(() => setLoadingDictionaries(false));
  }, [router, reloadDictionaries]);

  const loadItems = useCallback(() => {
    if (!token || !selectedDictionaryCode) {
      setItems([]);
      return;
    }
    setLoadingItems(true);
    setError('');
    queryDictionaryItems(token, { dictCode: selectedDictionaryCode, itemName, page, size, sortBy: sort?.field, sortDir: sort?.direction })
      .then((res) => {
        setItems(res.records);
        setTotal(res.total);
      })
      .catch(() => {
        setError('加载字典项失败，请稍后重试');
        setItems([]);
      })
      .finally(() => setLoadingItems(false));
  }, [selectedDictionaryCode, token, itemName, page, size, sort]);

  useEffect(() => { loadItems(); }, [loadItems]);

  function openCreate() {
    setFormError('');
    setForm({
      id: null,
      dictCode: selectedDictionary?.code ?? '',
      dictName: selectedDictionary?.name ?? '',
      itemCode: '',
      itemName: '',
      sortOrder: String(total + 1),
    });
  }

  function openEdit(item: DictionaryItemResponse) {
    setFormError('');
    setForm({
      id: item.id,
      dictCode: selectedDictionary?.code ?? '',
      dictName: selectedDictionary?.name ?? '',
      itemCode: item.value,
      itemName: item.name,
      sortOrder: String(item.sortOrder),
    });
  }

  async function saveForm() {
    if (!form) return;
    if (!form.dictCode.trim() || !form.dictName.trim() || !form.itemCode.trim() || !form.itemName.trim()) {
      setFormError('字典编码 / 字典名称 / 项编码 / 项名称均为必填');
      return;
    }
    setSaving(true);
    setFormError('');
    const payload = {
      dictCode: form.dictCode.trim(),
      dictName: form.dictName.trim(),
      itemCode: form.itemCode.trim(),
      itemName: form.itemName.trim(),
      sortOrder: form.sortOrder ? Number(form.sortOrder) : 0,
    };
    try {
      if (form.id === null) {
        await createDictionaryItem(token, payload);
      } else {
        await updateDictionaryItem(token, form.id, payload);
      }
      invalidateDictCache(payload.dictCode);
      setForm(null);
      await reloadDictionaries(token);
      if (payload.dictCode !== selectedDictionaryCode) {
        setSelectedDictionaryCode(payload.dictCode);
      } else {
        loadItems();
      }
    } catch (e) {
      setFormError((e as Error).message || '保存失败');
    } finally {
      setSaving(false);
    }
  }

  async function toggleEnabled(item: DictionaryItemResponse) {
    try {
      await changeDictionaryItemStatus(token, item.id, !item.enabled);
      invalidateDictCache(selectedDictionaryCode);
      loadItems();
    } catch (e) {
      setError((e as Error).message || '状态切换失败');
    }
  }

  const columns: TableColumn<DictionaryItemResponse>[] = [
    { key: 'name', title: '名称', sortable: true, render: (item) => item.name },
    { key: 'value', title: '编码', sortable: true, render: (item) => <span className="font-mono text-xs">{item.value}</span> },
    { key: 'sortOrder', title: '排序', sortable: true, render: (item) => item.sortOrder },
    {
      key: 'enabled', title: '状态', sortable: true,
      render: (item) => <span className={`badge ${item.enabled ? 'badge-green' : 'badge-red'}`}>{item.enabled ? '启用' : '禁用'}</span>,
    },
    ...(canWrite ? [{
      key: 'actions', title: '操作',
      render: (item: DictionaryItemResponse) => (
        <div className="flex gap-1">
          <button className="btn-ghost" onClick={() => openEdit(item)}>编辑</button>
          <button className={`btn-ghost ${item.enabled ? 'text-rose-500' : 'text-emerald-600'}`} onClick={() => toggleEnabled(item)}>
            {item.enabled ? '停用' : '启用'}
          </button>
        </div>
      ),
    } satisfies TableColumn<DictionaryItemResponse>] : []),
  ];

  function onSort(field: string) {
    setSort((previous) => previous?.field === field ? { field, direction: previous.direction === 'asc' ? 'desc' : 'asc' } : { field, direction: 'asc' });
    setPage(1);
  }

  return (
    <AppShell title="数据字典" description="维护业务表单使用的枚举选项（性别 / 就诊科室 / 档案类型 / 科室类别等）">
      <div className="grid gap-4 md:grid-cols-[280px_1fr]">
        <Card className="space-y-3">
          <h2 className="text-lg font-semibold">字典分类</h2>
          {loadingDictionaries ? <p className="hint">加载中…</p> : null}
          {!loadingDictionaries && dictionaries.length === 0 ? <p className="hint">暂无字典分类</p> : null}
          <div className="space-y-2">
            {dictionaries.map((dictionary) => (
              <Button
                key={dictionary.code}
                variant={selectedDictionaryCode === dictionary.code ? 'primary' : 'secondary'}
                className="w-full justify-start"
                onClick={() => { setSelectedDictionaryCode(dictionary.code); setPage(1); }}
              >
                {dictionary.name} ({dictionary.code})
              </Button>
            ))}
          </div>
          <p className="hint text-xs leading-relaxed">
            字典项被业务页面实时消费：在这里新增 / 停用某项后，相应表单下拉与列表展示会同步变化。
          </p>
        </Card>

        <Card className="space-y-4 overflow-auto">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h2 className="text-lg font-semibold">字典项</h2>
            <div className="flex items-center gap-2">
              <Input placeholder="按名称搜索" value={itemName} onChange={(event) => { setItemName(event.target.value); setPage(1); }} className="max-w-[220px]" />
              {canWrite && <Button onClick={openCreate}>+ 新建字典项</Button>}
              <Link href="/dashboard" className={buttonVariantClass('secondary')}>
                返回工作台
              </Link>
            </div>
          </div>

          {error ? <p className="error">{error}</p> : null}
          {loadingItems ? <p className="hint">加载字典项中…</p> : null}

          {!loadingItems && items.length > 0 ? (
            <DataTable columns={columns} rows={items} sort={sort} onSort={onSort} />
          ) : null}
          <TablePagination page={page} size={size} total={total} onChange={setPage} />

          {!loadingItems && selectedDictionaryCode && items.length === 0 ? <p className="hint">当前字典暂无数据</p> : null}
        </Card>
      </div>

      {/* 新建 / 编辑字典项 */}
      {form && (
        <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && setForm(null)}>
          <div className="modal-box">
            <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4">
              <h2 className="text-base font-semibold text-slate-900">{form.id === null ? '新建字典项' : '编辑字典项'}</h2>
              <button onClick={() => setForm(null)} className="rounded-lg p-1 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600">
                <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            <div className="flex-1 space-y-4 overflow-y-auto px-6 py-5">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="label">字典编码 *</label>
                  <Input value={form.dictCode} placeholder="如 sex_types" disabled={form.id !== null}
                         onChange={(e) => setForm((f) => f && { ...f, dictCode: e.target.value })} />
                </div>
                <div>
                  <label className="label">字典名称 *</label>
                  <Input value={form.dictName} placeholder="如 性别类型"
                         onChange={(e) => setForm((f) => f && { ...f, dictName: e.target.value })} />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="label">项编码 *</label>
                  <Input value={form.itemCode} placeholder="业务存储的码值，如 13"
                         onChange={(e) => setForm((f) => f && { ...f, itemCode: e.target.value })} />
                </div>
                <div>
                  <label className="label">项名称 *</label>
                  <Input value={form.itemName} placeholder="展示标签，如 骨科"
                         onChange={(e) => setForm((f) => f && { ...f, itemName: e.target.value })} />
                </div>
              </div>
              <div className="max-w-[160px]">
                <label className="label">排序</label>
                <Input type="number" value={form.sortOrder}
                       onChange={(e) => setForm((f) => f && { ...f, sortOrder: e.target.value })} />
              </div>
              <p className="hint text-xs">业务表存的是「项编码」；下拉框与列表展示的是「项名称」。数字型字段（性别 / 科室等）请使用数字编码。</p>
              {formError && <p className="error">{formError}</p>}
            </div>
            <div className="flex justify-end gap-3 border-t border-slate-100 px-6 py-4">
              <Button variant="secondary" onClick={() => setForm(null)}>取消</Button>
              <Button onClick={saveForm} disabled={saving}>{saving ? '保存中…' : '保存'}</Button>
            </div>
          </div>
        </div>
      )}
    </AppShell>
  );
}
