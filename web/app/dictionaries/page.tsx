'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { AppShell } from '@/components/layout/app-shell';
import Link from 'next/link';
import { Button, buttonVariantClass } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { DictionaryItemResponse, DictionaryResponse, currentUser, listDictionaries, queryDictionaryItems } from '@/lib/api';
import { hasPermission } from '@/lib/permissions';
import { DataTable, SortState, TableColumn, TablePagination } from '@/components/ui/data-table';
import { Input } from '@/components/ui/input';

export default function DictionariesPage() {
  const router = useRouter();
  const [token, setToken] = useState('');
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
        setError('Current account does not have dictionary:read permission');
        router.replace('/dashboard');
      }
    }).catch(() => {});

    listDictionaries(currentToken)
      .then((result) => {
        setDictionaries(result);
        if (result.length > 0) setSelectedDictionaryCode(result[0].code);
      })
      .catch(() => {
        setError('Failed to load dictionaries. Please sign in again.');
        localStorage.removeItem('access_token');
        localStorage.removeItem('token_type');
        router.replace('/login');
      })
      .finally(() => setLoadingDictionaries(false));
  }, [router]);

  useEffect(() => {
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
        setError('Failed to load dictionary items. Please try again later.');
        setItems([]);
      })
      .finally(() => setLoadingItems(false));
  }, [selectedDictionaryCode, token, itemName, page, size, sort]);

  const columns: TableColumn<DictionaryItemResponse>[] = [
    { key: 'name', title: 'Name', sortable: true, render: (item) => item.name },
    { key: 'value', title: 'Code', sortable: true, render: (item) => <span className="font-mono text-xs">{item.value}</span> },
    { key: 'sortOrder', title: 'Sort Order', sortable: true, render: (item) => item.sortOrder },
    { key: 'enabled', title: 'Status', sortable: true, render: (item) => item.enabled ? 'Enabled' : 'Disabled' }
  ];

  function onSort(field: string) {
    setSort((previous) => previous?.field === field ? { field, direction: previous.direction === 'asc' ? 'desc' : 'asc' } : { field, direction: 'asc' });
    setPage(1);
  }

  return (
    <AppShell title="Dictionary Management" description="Browse dictionary categories and dictionary items.">
      <div className="grid gap-4 md:grid-cols-[280px_1fr]">
        <Card className="space-y-3">
          <h2 className="text-lg font-semibold">Dictionary Categories</h2>
          {loadingDictionaries ? <p className="hint">Loading...</p> : null}
          {!loadingDictionaries && dictionaries.length === 0 ? <p className="hint">No dictionary categories</p> : null}
          <div className="space-y-2">
            {dictionaries.map((dictionary) => (
              <Button
                key={dictionary.code}
                variant={selectedDictionaryCode === dictionary.code ? 'primary' : 'secondary'}
                className="w-full justify-start"
                onClick={() => setSelectedDictionaryCode(dictionary.code)}
              >
                {dictionary.name} ({dictionary.code})
              </Button>
            ))}
          </div>
        </Card>

        <Card className="space-y-4 overflow-auto">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold">Dictionary Items</h2>
            <Input placeholder="Filter by item name" value={itemName} onChange={(event) => { setItemName(event.target.value); setPage(1); }} className="max-w-[280px]" />
            <Link href="/dashboard" className={buttonVariantClass('secondary')}>
              Back to Dashboard
            </Link>
          </div>

          {error ? <p className="error">{error}</p> : null}
          {loadingItems ? <p className="hint">Loading dictionary items...</p> : null}

          {!loadingItems && items.length > 0 ? (
            <DataTable columns={columns} rows={items} sort={sort} onSort={onSort} />
          ) : null}
          <TablePagination page={page} size={size} total={total} onChange={setPage} />

          {!loadingItems && selectedDictionaryCode && items.length === 0 ? <p className="hint">No items in this dictionary</p> : null}
        </Card>
      </div>
    </AppShell>
  );
}
