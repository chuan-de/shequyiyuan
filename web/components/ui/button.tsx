import Link from 'next/link';
import { ButtonHTMLAttributes, ReactNode } from 'react';

export type ButtonVariant = 'primary' | 'secondary';

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
  children: ReactNode;
};

type LinkButtonProps = {
  href: string;
  variant?: ButtonVariant;
  children: ReactNode;
  className?: string;
};

export function buttonVariantClass(variant: ButtonVariant) {
  return variant === 'secondary' ? 'btn-secondary' : 'btn';
}

/**
 * Use `Button` for actions within the current page (submit, save, delete, etc.).
 */
export function Button({ variant = 'primary', children, className = '', ...props }: ButtonProps) {
  return (
    <button className={`${buttonVariantClass(variant)} ${className}`.trim()} {...props}>
      {children}
    </button>
  );
}

/**
 * Use `LinkButton` only when a styled navigation link is required.
 * Prefer `next/link` + `buttonVariantClass` for new code and gradual migration.
 */
export function LinkButton({ href, variant = 'primary', children, className = '' }: LinkButtonProps) {
  return (
    <Link href={href} className={`${buttonVariantClass(variant)} ${className}`.trim()}>
      {children}
    </Link>
  );
}
