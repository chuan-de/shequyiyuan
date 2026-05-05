'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { AppShell } from '@/components/layout/app-shell';
import { Button, LinkButton } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { DictionaryItemResponse, DictionaryResponse, listDictionaries, listDictionaryItems } from '@/lib/api';

export default function DictionariesPage() {
  const router = useRouter();
  const [token, setToken] = useState('');
  const [dictionaries, setDictionaries] = useState<DictionaryResponse[]>([]);
  const [selectedDictCode, setSelectedDictCode] = useState('');
  const [items, setItems] = useState<DictionaryItemResponse[]>([]);
  const [loadingDicts, setLoadingDicts] = useState(true);
  const [loadingItems, setLoadingItems] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const currentToken = localStorage.getItem('access_token');
    if (!currentToken) {
      router.replace('/login');
      return;
    }

    setToken(currentToken);
    setLoadingDicts(true);

    listDictionaries(currentToken)
      .then((result) => {
        setDictionaries(result);
        if (result.length > 0) {
          setSelectedDictCode(result[0].code);
        }
      })
      .catch(() => {
        setError('获取字典列表失败，请重新登录后重试');
        localStorage.removeItem('access_token');
        localStorage.removeItem('token_type');
        router.replace('/login');
      })
      .finally(() => {
        setLoadingDicts(false);
      });
  }, [router]);

  useEffect(() => {
    if (!token || !selectedDictCode) {
      setItems([]);
      return;
    }

    setLoadingItems(true);
    setError('');

    listDictionaryItems(token, selectedDictCode)
      .then(setItems)
      .catch(() => {
        setError('获取字典项失败，请稍后重试');
        setItems([]);
      })
      .finally(() => {
        setLoadingItems(false);
      });
  }, [selectedDictCode, token]);

  return (
    <AppShell title="字典管理" description="浏览系统字典分类及字典项。">
      <div className="grid gap-4 md:grid-cols-[280px_1fr]">
        <Card className="space-y-3">
          <h2 className="text-lg font-semibold">字典分类</h2>
          {loadingDicts ? <p className="hint">加载中...</p> : null}
          {!loadingDicts && dictionaries.length === 0 ? <p className="hint">暂无字典分类</p> : null}
          <div className="space-y-2">
            {dictionaries.map((dict) => (
              <Button
                key={dict.code}
                variant={selectedDictCode === dict.code ? 'primary' : 'secondary'}
                className="w-full justify-start"
                onClick={() => setSelectedDictCode(dict.code)}
              >
                {dict.name}（{dict.code}）
              </Button>
            ))}
          </div>
        </Card>

        <Card className="space-y-4 overflow-auto">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold">字典项</h2>
            <LinkButton href="/dashboard" variant="secondary">
              返回 Dashboard
            </LinkButton>
          </div>

          {error ? <p className="error">{error}</p> : null}
          {loadingItems ? <p className="hint">加载字典项中...</p> : null}

          {!loadingItems && items.length > 0 ? (
            <table className="w-full min-w-[560px] table-auto border-collapse text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-left text-slate-600">
                  <th className="px-2 py-2">名称</th>
                  <th className="px-2 py-2">编码</th>
                  <th className="px-2 py-2">排序</th>
                  <th className="px-2 py-2">启用状态</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <tr key={item.id} className="border-b border-slate-100">
                    <td className="px-2 py-2">{item.name}</td>
                    <td className="px-2 py-2 font-mono text-xs">{item.value}</td>
                    <td className="px-2 py-2">{item.sortOrder}</td>
                    <td className="px-2 py-2">{item.enabled ? '启用' : '停用'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : null}

          {!loadingItems && selectedDictCode && items.length === 0 ? <p className="hint">该字典暂无字典项</p> : null}
        </Card>
      </div>
    </AppShell>
  );
}
