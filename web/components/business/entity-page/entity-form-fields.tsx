import type { Dispatch, SetStateAction } from 'react';
import { Input } from '@/components/ui/input';
import { Select } from '@/components/ui/select';
import { PhotoUploader } from '@/components/ui/file-upload';
import type { EntityFormField } from './types';

/** 新建/编辑弹窗共用的表单字段渲染（编辑模式跳过 password 字段）。 */
export function EntityFormFields({ fields, form, setForm, mode, dictOptions, permissions }: {
  fields: EntityFormField[];
  form: Record<string, string>;
  setForm: Dispatch<SetStateAction<Record<string, string>>>;
  mode: 'create' | 'edit';
  dictOptions: (dictCode?: string) => { value: string; label: string }[];
  permissions: string[];
}) {
  const setField = (key: string, value: string) => setForm(prev => ({ ...prev, [key]: value }));
  return (
    <div className="space-y-4">
      {fields.map(f => {
        if (mode === 'edit' && f.type === 'password') return null;
        return (
          <div key={f.key}>
            <label className="label">
              {f.label}
              {f.required && <span className="ml-0.5 text-rose-500">*</span>}
            </label>
            {f.type === 'select' || f.type === 'dict-select' ? (
              <Select
                value={form[f.key] ?? ''}
                onChange={(v) => setField(f.key, v)}
                options={f.type === 'dict-select' ? dictOptions(f.dictCode) : f.options ?? []}
                placeholder={f.placeholder ?? '请选择'}
              />
            ) : f.type === 'textarea' ? (
              <textarea
                value={form[f.key] ?? ''}
                placeholder={f.placeholder ?? `请输入${f.label}`}
                onChange={e => setField(f.key, e.target.value)}
                className="input w-full min-h-[80px]"
                rows={3}
              />
            ) : f.type === 'photo' ? (
              <PhotoUploader
                value={form[f.key] ?? ''}
                onChange={url => setField(f.key, url)}
              />
            ) : f.type === 'datetime' || f.type === 'date' ? (
              <Input
                type={f.type === 'date' ? 'date' : 'datetime-local'}
                value={form[f.key] ?? ''}
                onChange={e => setField(f.key, e.target.value)}
              />
            ) : f.type === 'custom' && f.customRender ? (
              f.customRender(
                form[f.key] ?? '',
                (next) => setField(f.key, next),
                mode,
                {
                  getFormValue: (k) => form[k] ?? '',
                  setFormValue: setField,
                  patchForm: (patch) => setForm(prev => ({ ...prev, ...patch })),
                  permissions,
                }
              )
            ) : (
              <Input
                type={f.type === 'password' ? 'password' : f.type === 'number' ? 'number' : 'text'}
                value={form[f.key] ?? ''}
                placeholder={f.placeholder ?? `请输入${f.label}`}
                onChange={e => setField(f.key, e.target.value)}
              />
            )}
          </div>
        );
      })}
    </div>
  );
}
