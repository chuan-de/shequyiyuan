import { ButtonHTMLAttributes } from 'react';

type ButtonVariant = 'primary' | 'secondary';

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
};

export function Button({ className = '', variant = 'primary', ...props }: ButtonProps) {
  const styleClass = variant === 'secondary' ? 'btn-secondary' : 'btn';
  return <button className={`${styleClass} ${className}`.trim()} {...props} />;
}
