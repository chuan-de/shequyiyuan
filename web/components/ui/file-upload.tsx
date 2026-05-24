'use client';

import { useRef, useState } from 'react';
import { uploadPhoto } from '@/lib/file';

type Props = {
  value?: string;
  onChange: (url: string) => void;
  disabled?: boolean;
};

export function PhotoUploader({ value, onChange, disabled }: Props) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>('');

  async function handleFile(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    setError('');
    setLoading(true);
    try {
      const token = localStorage.getItem('access_token') ?? '';
      const result = await uploadPhoto(token, file);
      onChange(result.url);
    } catch (err) {
      setError(err instanceof Error ? err.message : '上传失败');
    } finally {
      setLoading(false);
      if (inputRef.current) inputRef.current.value = '';
    }
  }

  return (
    <div className="space-y-2">
      <div className="flex items-center gap-3">
        {value ? (
          <img
            src={value}
            alt="预览"
            className="h-16 w-16 rounded-lg border border-slate-200 object-cover"
            onError={(e) => { (e.target as HTMLImageElement).style.opacity = '0.3'; }}
          />
        ) : (
          <div className="flex h-16 w-16 items-center justify-center rounded-lg border border-dashed border-slate-300 bg-slate-50 text-xs text-slate-400">
            无图片
          </div>
        )}
        <div className="flex items-center gap-2">
          <button
            type="button"
            disabled={disabled || loading}
            onClick={() => inputRef.current?.click()}
            className="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {loading ? '上传中…' : value ? '更换图片' : '上传图片'}
          </button>
          {value && !loading && (
            <button
              type="button"
              disabled={disabled}
              onClick={() => onChange('')}
              className="rounded-lg border border-rose-200 bg-white px-3 py-1.5 text-sm font-medium text-rose-600 hover:bg-rose-50 disabled:opacity-50"
            >
              移除
            </button>
          )}
        </div>
        <input
          ref={inputRef}
          type="file"
          accept="image/jpeg,image/png,image/gif,image/webp"
          className="hidden"
          onChange={handleFile}
          disabled={disabled || loading}
        />
      </div>
      {error && <p className="text-xs text-rose-600">{error}</p>}
      <p className="text-xs text-slate-400">支持 JPG / PNG / GIF / WebP，最大 5MB</p>
    </div>
  );
}

// Keep old export name for backward compatibility
export { PhotoUploader as FileUpload };

export type AttachmentItem = { url: string; filename: string; contentType: string };

type MultiProps = {
  value?: AttachmentItem[];
  onChange: (items: AttachmentItem[]) => void;
  disabled?: boolean;
};

export function MultiFileUploader({ value, onChange, disabled }: MultiProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>('');
  const items = value ?? [];

  async function handleFiles(e: React.ChangeEvent<HTMLInputElement>) {
    const files = Array.from(e.target.files ?? []);
    if (files.length === 0) return;
    setError('');
    setLoading(true);
    try {
      const token = localStorage.getItem('access_token') ?? '';
      const uploaded: AttachmentItem[] = [];
      for (const f of files) {
        const r = await uploadPhoto(token, f);
        uploaded.push({ url: r.url, filename: r.originalFilename ?? f.name, contentType: r.contentType });
      }
      onChange([...items, ...uploaded]);
    } catch (err) {
      setError(err instanceof Error ? err.message : '上传失败');
    } finally {
      setLoading(false);
      if (inputRef.current) inputRef.current.value = '';
    }
  }

  function remove(idx: number) {
    onChange(items.filter((_, i) => i !== idx));
  }

  return (
    <div className="space-y-2">
      <div className="flex flex-wrap gap-2">
        {items.map((it, idx) => {
          const isImg = it.contentType?.startsWith('image/');
          return (
            <div key={idx} className="relative w-24 rounded-lg border border-slate-200 bg-white p-1 text-center">
              {isImg ? (
                <img src={it.url} alt={it.filename} className="h-16 w-full rounded object-cover" />
              ) : (
                <div className="flex h-16 w-full items-center justify-center rounded bg-rose-50 text-xs font-medium text-rose-600">PDF</div>
              )}
              <a href={it.url} target="_blank" rel="noopener noreferrer" className="mt-1 block truncate text-xs text-slate-600 hover:underline" title={it.filename}>
                {it.filename}
              </a>
              {!disabled && (
                <button type="button" onClick={() => remove(idx)} className="absolute -right-1 -top-1 h-5 w-5 rounded-full bg-rose-500 text-xs text-white shadow hover:bg-rose-600">×</button>
              )}
            </div>
          );
        })}
        <button
          type="button"
          disabled={disabled || loading}
          onClick={() => inputRef.current?.click()}
          className="flex h-[88px] w-24 flex-col items-center justify-center rounded-lg border border-dashed border-slate-300 bg-slate-50 text-xs text-slate-500 hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {loading ? '上传中…' : '+ 添加'}
        </button>
        <input
          ref={inputRef}
          type="file"
          multiple
          accept="image/jpeg,image/png,image/gif,image/webp,application/pdf"
          className="hidden"
          onChange={handleFiles}
          disabled={disabled || loading}
        />
      </div>
      {error && <p className="text-xs text-rose-600">{error}</p>}
      <p className="text-xs text-slate-400">支持 JPG / PNG / GIF / WebP / PDF，单文件最大 10MB</p>
    </div>
  );
}
