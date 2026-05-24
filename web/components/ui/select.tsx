'use client';
import { useEffect, useRef, useState } from 'react';

export type SelectOption = { value: string; label: string };

type Props = {
  value: string;
  onChange: (value: string) => void;
  options: SelectOption[];
  placeholder?: string;
  className?: string;
  disabled?: boolean;
  allowEmpty?: boolean;
  emptyLabel?: string;
};

export function Select({
  value,
  onChange,
  options,
  placeholder = '请选择',
  className = '',
  disabled,
  allowEmpty,
  emptyLabel = '（全部）',
}: Props) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    function onDocClick(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    }
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') setOpen(false);
    }
    document.addEventListener('mousedown', onDocClick);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onDocClick);
      document.removeEventListener('keydown', onKey);
    };
  }, [open]);

  const selected = options.find(o => o.value === value);
  const display = selected?.label ?? '';

  return (
    <div ref={ref} className={`relative ${className}`}>
      <button
        type="button"
        disabled={disabled}
        onClick={() => !disabled && setOpen(o => !o)}
        className="select flex w-full items-center justify-between text-left disabled:cursor-not-allowed disabled:opacity-60"
        style={{ backgroundImage: 'none', paddingRight: '0.75rem' }}
      >
        <span className={`truncate ${display ? '' : 'text-slate-400'}`}>
          {display || placeholder}
        </span>
        <svg className={`ml-2 h-4 w-4 flex-shrink-0 text-slate-500 transition-transform ${open ? 'rotate-180' : ''}`} viewBox="0 0 20 20" fill="currentColor">
          <path fillRule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clipRule="evenodd" />
        </svg>
      </button>
      {open && (
        <div className="absolute left-0 right-0 z-50 mt-1 max-h-60 overflow-auto rounded-xl border border-slate-200 bg-white py-1 shadow-lg ring-1 ring-black/5">
          {allowEmpty && (
            <button
              type="button"
              onClick={() => { onChange(''); setOpen(false); }}
              className={`block w-full px-3 py-2 text-left text-sm transition hover:bg-slate-50 ${value === '' ? 'bg-brand/10 text-brand' : 'text-slate-700'}`}
            >
              {emptyLabel}
            </button>
          )}
          {options.length === 0 ? (
            <div className="px-3 py-2 text-sm text-slate-400">暂无选项</div>
          ) : (
            options.map(o => (
              <button
                key={o.value}
                type="button"
                onClick={() => { onChange(o.value); setOpen(false); }}
                className={`block w-full truncate px-3 py-2 text-left text-sm transition hover:bg-slate-50 ${o.value === value ? 'bg-brand/10 text-brand' : 'text-slate-700'}`}
              >
                {o.label}
              </button>
            ))
          )}
        </div>
      )}
    </div>
  );
}
