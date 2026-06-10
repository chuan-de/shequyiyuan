import type { ReactNode } from 'react';
import type { EntityRecord } from '@/lib/api';

/** Extra wiring handed to {@link EntityFormField.customRender}. */
export type CustomFieldContext = {
  /** Read another field's current value from the surrounding form. */
  getFormValue: (key: string) => string;
  /** Patch any field in the surrounding form (used by the AI suggestion panel). */
  setFormValue: (key: string, value: string) => void;
  /** Batch patch — applies multiple fields atomically in one re-render. */
  patchForm: (patch: Record<string, string>) => void;
  /** Current user's permissions, so the custom renderer can gate UI elements. */
  permissions: string[];
};

export type EntityFormField = {
  key: string;
  label: string;
  required?: boolean;
  placeholder?: string;
  defaultValue?: string;
  type?: 'text' | 'password' | 'textarea' | 'number' | 'select' | 'dict-select' | 'photo' | 'datetime' | 'date' | 'custom';
  options?: { value: string; label: string }[];
  /** type 为 'dict-select' 时必填：选项实时来自数据字典（仅启用项）。 */
  dictCode?: string;
  customRender?: (value: string, onChange: (next: string) => void, mode: 'create' | 'edit', ctx: CustomFieldContext) => ReactNode;
};

export type EntityColumn = {
  key: string;
  title: string;
  type?: 'text' | 'photo' | 'currency' | 'badge';
  /** 设置后列值按数据字典翻译显示（码值 → 中文标签），详情弹窗同样生效。 */
  dictCode?: string;
  render?: (row: EntityRecord) => ReactNode;
};

export type PromptOptions = {
  title: string;
  label?: string;
  placeholder?: string;
  type?: 'text' | 'password' | 'number';
  initialValue?: string;
  confirmText?: string;
  validate?: (value: string) => string | null;
};

export type ActionHelpers = {
  token: string;
  reload: () => void;
  showToast: (msg: string, type?: 'success' | 'error') => void;
  prompt: (opts: PromptOptions) => Promise<string | null>;
};

export type EntityPageConfig = {
  title: string;
  route: string;
  permissionPrefix: string;
  columns: EntityColumn[];
  formFields: EntityFormField[];
  statusField?: 'enabled' | 'status';
  createPayload: (form: Record<string, string>) => Record<string, unknown>;
  updatePayload: (form: Record<string, string>, row: EntityRecord) => Record<string, unknown>;
  searchFields?: { key: string; label: string; type?: 'text' | 'select' | 'dict-select'; options?: { value: string; label: string }[]; dictCode?: string }[];
  rowActions?: {
    key: string;
    label: string;
    permission?: string;
    variant?: 'primary' | 'danger';
    onClick: (row: EntityRecord, helpers: ActionHelpers) => void;
  }[];
  labelMap?: Record<string, string>;
  /** 进入页面即弹出新建表单（配合 formFields defaultValue 实现跨页预填，如就诊→写病历）。 */
  autoOpenCreate?: boolean;
};

export function isoToLocalInput(iso: string): string {
  const d = new Date(iso);
  if (isNaN(d.getTime())) return '';
  const tz = d.getTimezoneOffset() * 60000;
  return new Date(d.getTime() - tz).toISOString().slice(0, 16);
}

export function nowLocalInput(): string {
  return isoToLocalInput(new Date().toISOString());
}

export function initForm(fields: EntityFormField[], row?: EntityRecord | null): Record<string, string> {
  return fields.reduce<Record<string, string>>((acc, f) => {
    const v = row?.[f.key];
    if (f.type === 'datetime') {
      if (typeof v === 'string' && v) acc[f.key] = isoToLocalInput(v);
      else if (!row) acc[f.key] = nowLocalInput();
      else acc[f.key] = '';
    } else if (f.type === 'date') {
      // 后端 LocalDate 序列化为 yyyy-MM-dd，直接截取适配 <input type="date">。
      acc[f.key] = typeof v === 'string' && v ? v.slice(0, 10) : (f.defaultValue ?? '');
    } else if (f.type === 'custom') {
      if (v !== undefined && v !== null && typeof v !== 'string') {
        acc[f.key] = JSON.stringify(v);
      } else if (typeof v === 'string') {
        acc[f.key] = v;
      } else {
        acc[f.key] = f.defaultValue ?? '';
      }
    } else {
      acc[f.key] = typeof v === 'string' ? v : (typeof v === 'number' ? String(v) : f.defaultValue ?? '');
    }
    return acc;
  }, {});
}
