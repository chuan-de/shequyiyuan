import { ReactNode } from 'react';
import { Sidebar } from './sidebar';

type AppShellProps = {
  title: string;
  description?: string;
  children: ReactNode;
};

export function AppShell({ title, description, children }: AppShellProps) {
  return (
    <div className="flex min-h-screen bg-slate-50">
      <Sidebar />
      <main className="flex-1 overflow-auto">
        <div className="p-6 lg:p-8">
          <header className="mb-6">
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">{title}</h1>
            {description ? <p className="hint mt-1">{description}</p> : null}
          </header>
          {children}
        </div>
      </main>
    </div>
  );
}
