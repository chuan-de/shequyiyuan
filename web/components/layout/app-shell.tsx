import { ReactNode } from 'react';

type AppShellProps = {
  title: string;
  description?: string;
  children: ReactNode;
};

export function AppShell({ title, description, children }: AppShellProps) {
  return (
    <main className="page-wrap">
      <div className="mx-auto w-full max-w-6xl space-y-4">
        <header className="space-y-1">
          <h1 className="text-2xl font-bold tracking-tight">{title}</h1>
          {description ? <p className="hint">{description}</p> : null}
        </header>
        {children}
      </div>
    </main>
  );
}
