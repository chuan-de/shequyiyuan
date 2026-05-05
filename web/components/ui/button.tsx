import Link from 'next/link';
import { ButtonHTMLAttributes, ReactNode } from 'react';

type ButtonVariant = 'primary' | 'secondary';

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

function getVariantClass(variant: ButtonVariant) {
  return variant === 'secondary' ? 'btn-secondary' : 'btn';
}

export function Button({ variant = 'primary', children, className = '', ...props }: ButtonProps) {
  return (
    <button className={`${getVariantClass(variant)} ${className}`.trim()} {...props}>
      {children}
    </button>
  );
}

export function LinkButton({ href, variant = 'primary', children, className = '' }: LinkButtonProps) {
  return (
    <Link href={href} className={`${getVariantClass(variant)} ${className}`.trim()}>
      {children}
    </Link>
  );
}
