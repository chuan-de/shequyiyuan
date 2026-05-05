'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { AppShell } from '@/components/layout/app-shell';
import Link from 'next/link';
import { Button, buttonVariantClass } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { DictionaryItemResponse, DictionaryResponse, currentUser, listDictionaries, listDictionaryItems } from '@/lib/api';
import { hasPermission } from '@/lib/permissions';

export default function DictionariesPage() {
  const router = useRouter();
  const [token, setToken] = useState('');
  const [dictionaries, setDictionaries] = useState<DictionaryResponse[]>([]);
  const [selectedDictionaryCode, setSelectedDictionaryCode] = useState('');
  const [items, setItems] = useState<DictionaryItemResponse[]>([]);
  const [loadingDictionaries, setLoadingDictionaries] = useState(true);
  const [loadingItems, setLoadingItems] = useState(false);
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

    listDictionaryItems(token, selectedDictionaryCode)
      .then(setItems)
      .catch(() => {
        setError('Failed to load dictionary items. Please try again later.');
        setItems([]);
      })
      .finally(() => setLoadingItems(false));
  }, [selectedDictionaryCode, token]);

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
            <Link href="/dashboard" className={buttonVariantClass('secondary')}>
              Back to Dashboard
            </Link>
          </div>

          {error ? <p className="error">{error}</p> : null}
          {loadingItems ? <p className="hint">Loading dictionary items...</p> : null}

          {!loadingItems && items.length > 0 ? (
            <table className="w-full min-w-[560px] table-auto border-collapse text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-left text-slate-600">
                  <th className="px-2 py-2">Name</th>
                  <th className="px-2 py-2">Code</th>
                  <th className="px-2 py-2">Sort Order</th>
                  <th className="px-2 py-2">Status</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <tr key={item.id} className="border-b border-slate-100">
                    <td className="px-2 py-2">{item.name}</td>
                    <td className="px-2 py-2 font-mono text-xs">{item.value}</td>
                    <td className="px-2 py-2">{item.sortOrder}</td>
                    <td className="px-2 py-2">{item.enabled ? 'Enabled' : 'Disabled'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : null}

          {!loadingItems && selectedDictionaryCode && items.length === 0 ? <p className="hint">No items in this dictionary</p> : null}
        </Card>
      </div>
    </AppShell>
  );
}
