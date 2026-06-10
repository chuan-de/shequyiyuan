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
          <header className="mb-6 flex items-center gap-3">
            <span className="h-7 w-1.5 rounded-full bg-gradient-to-b from-blue-600 to-sky-400" />
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-slate-900">{title}</h1>
              {description ? <p className="hint mt-0.5">{description}</p> : null}
            </div>
          </header>
          {children}
        </div>
      </main>
    </div>
  );
}
