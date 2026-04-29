import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'Shequyiyuan Web',
  description: 'Next.js frontend for hospital project',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="zh-CN">
      <body>{children}</body>
    </html>
  );
}
