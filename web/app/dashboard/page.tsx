'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { AppShell } from '@/components/layout/app-shell';
import { Card } from '@/components/ui/card';
import { LinkButton, Button } from '@/components/ui/button';
import { CurrentUserResponse, currentUser } from '@/lib/api';
import { readToken, clearToken } from '@/lib/token-storage';
import { hasPermission } from '@/lib/permissions';

export default function DashboardPage() {
  const router = useRouter();
  const [user, setUser] = useState<CurrentUserResponse | null>(null);
  const [tokenPreview, setTokenPreview] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const token = readToken();
    if (!token) {
      router.replace('/login');
      return;
    }

    setTokenPreview(token.accessToken.slice(0, 40));

    currentUser(token.accessToken)
      .then(setUser)
      .catch(() => {
        setError('Failed to load user profile. Please sign in again.');
        clearToken();
        router.replace('/login');
      })
      .finally(() => setLoading(false));
  }, [router]);

  if (loading) {
    return (
      <AppShell title="Dashboard" description="System status and account information.">
        <Card>Loading...</Card>
      </AppShell>
    );
  }

  return (
    <AppShell title="Dashboard" description="Frontend and backend auth integration is ready (login/register/me).">
      <Card className="space-y-4">
        {user && (
          <div className="rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-700">
            <p>
              Current User:<strong>{user.username}</strong>
            </p>
            <p>Status:{user.enabled ? 'Enabled' : 'Disabled'}</p>
            <p>Roles:{user.roles.join(', ') || 'None'}</p>
            <p>Permissions:{user.permissions.join(', ') || 'None'}</p>
          </div>
        )}

        <p className="text-sm text-slate-700">Current Token (first 40 chars):</p>
        <code className="block overflow-auto rounded-lg bg-slate-900 p-3 text-xs text-slate-100">
          {tokenPreview || 'Not signed in or token unavailable'}
        </code>

        {error && <p className="error">{error}</p>}

        <div className="flex flex-wrap gap-3">
          <LinkButton variant="secondary" href="/login">
            Go to Login
          </LinkButton>
          {hasPermission(user, 'dictionary:read') ? (
            <LinkButton variant="secondary" href="/dictionaries">
              Dictionary Management
            </LinkButton>
          ) : null}
          <Button
            onClick={() => {
              clearToken();
              router.replace('/login');
            }}
          >
            Sign Out
          </Button>
        </div>
      </Card>
    </AppShell>
  );
}
