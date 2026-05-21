import { ReactNode } from 'react';
import { Card } from '@/components/ui/card';

export function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <main className="page-wrap page-center">
      <Card className="w-full max-w-md space-y-6">{children}</Card>
    </main>
  );
}
