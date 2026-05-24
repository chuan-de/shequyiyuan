'use client';

import { useEffect, useState } from 'react';
import type { AiVisionResponse, MedicalRecordFields } from '@/lib/api';

/**
 * Side panel shown after the medic clicks "AI 识别" on an attachment.
 * Lists every field the model returned with a confidence badge, lets the
 * medic tick the ones they trust, then "填入表单" pushes those values back
 * up to the parent form via {@link AiSuggestionPanelProps.onApply}.
 *
 * Field order mirrors {@link MedicalRecordFields} so it matches the
 * medical-record form layout below it.
 */

export type SuggestionFieldKey = keyof MedicalRecordFields;

export type AiSuggestionPanelProps = {
  open: boolean;
  loading: boolean;
  error: string | null;
  response: AiVisionResponse | null;
  /** Keys that already have a non-empty value in the parent form. */
  formHasValueFor: (key: SuggestionFieldKey) => boolean;
  onApply: (picked: Partial<MedicalRecordFields>) => void;
  onClose: () => void;
};

const FIELD_LABELS: Record<SuggestionFieldKey, string> = {
  patientName: '患者姓名',
  gender: '性别',
  age: '年龄',
  visitDate: '就诊日期',
  department: '科室',
  chiefComplaint: '主诉',
  presentIllness: '现病史',
  diagnosis: '诊断',
  prescription: '处方/医嘱',
  doctor: '医生',
};

const FIELD_ORDER: SuggestionFieldKey[] = [
  'patientName', 'gender', 'age', 'visitDate', 'department',
  'chiefComplaint', 'presentIllness', 'diagnosis', 'prescription', 'doctor',
];

function ConfidenceBadge({ value }: { value: number }) {
  const colour =
    value >= 80 ? 'bg-emerald-50 text-emerald-700 ring-emerald-200' :
    value >= 50 ? 'bg-amber-50 text-amber-700 ring-amber-200' :
                  'bg-rose-50 text-rose-700 ring-rose-200';
  return (
    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ring-1 ${colour}`}>
      置信度 {value.toFixed(0)}%
    </span>
  );
}

export function AiSuggestionPanel({ open, loading, error, response, formHasValueFor, onApply, onClose }: AiSuggestionPanelProps) {
  // Per-field tick state + per-field edited value (medic may tweak before applying).
  const [picked, setPicked] = useState<Record<SuggestionFieldKey, boolean>>({} as Record<SuggestionFieldKey, boolean>);
  const [edited, setEdited] = useState<Partial<Record<SuggestionFieldKey, string>>>({});

  useEffect(() => {
    if (!response) return;
    // Default: tick every field the model returned non-null AND the form doesn't already have.
    const nextPicked: Record<SuggestionFieldKey, boolean> = {} as Record<SuggestionFieldKey, boolean>;
    const nextEdited: Partial<Record<SuggestionFieldKey, string>> = {};
    FIELD_ORDER.forEach(key => {
      const raw = response.fields[key];
      const hasValue = raw !== null && raw !== undefined && String(raw).trim() !== '';
      nextPicked[key] = hasValue && !formHasValueFor(key);
      if (hasValue) nextEdited[key] = String(raw);
    });
    setPicked(nextPicked);
    setEdited(nextEdited);
  }, [response, formHasValueFor]);

  if (!open) return null;

  function toggle(key: SuggestionFieldKey) {
    setPicked(prev => ({ ...prev, [key]: !prev[key] }));
  }
  function setValue(key: SuggestionFieldKey, value: string) {
    setEdited(prev => ({ ...prev, [key]: value }));
  }
  function handleApply() {
    if (!response) return;
    const result: Partial<MedicalRecordFields> = {};
    FIELD_ORDER.forEach(key => {
      if (!picked[key]) return;
      const value = edited[key] ?? '';
      if (key === 'age') {
        const n = Number(value);
        if (Number.isFinite(n)) (result[key] as number | null) = n;
      } else {
        (result[key] as string | null) = value;
      }
    });
    onApply(result);
  }

  return (
    <div className="fixed inset-y-0 right-0 z-40 flex w-full max-w-md flex-col border-l border-slate-200 bg-white shadow-2xl">
      <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4">
        <h3 className="text-base font-semibold text-slate-900">AI 识别建议</h3>
        <button
          type="button"
          onClick={onClose}
          className="rounded-lg p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600"
          aria-label="关闭"
        >
          <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <div className="flex-1 overflow-y-auto px-5 py-4">
        {loading && (
          <div className="flex flex-col items-center justify-center gap-2 py-12 text-sm text-slate-500">
            <span className="inline-block h-5 w-5 animate-spin rounded-full border-2 border-slate-300 border-t-slate-700" />
            正在识别图片，请稍候…
          </div>
        )}

        {error && !loading && (
          <div className="rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-700 ring-1 ring-rose-200">
            {error}
          </div>
        )}

        {response && !loading && !error && (
          <div className="space-y-4">
            <div className="flex flex-wrap items-center gap-2 text-xs text-slate-500">
              <ConfidenceBadge value={response.confidence} />
              <span>token 输入 {response.tokensIn}</span>
              <span>· 输出 {response.tokensOut}</span>
              <span>· 耗时 {Math.round(response.latencyMs)}ms</span>
            </div>

            <ul className="space-y-3">
              {FIELD_ORDER.map(key => {
                const raw = response.fields[key];
                const present = raw !== null && raw !== undefined && String(raw).trim() !== '';
                const conflict = present && formHasValueFor(key);
                return (
                  <li key={key} className={`rounded-lg border px-3 py-2 ${present ? 'border-slate-200 bg-white' : 'border-dashed border-slate-200 bg-slate-50 opacity-60'}`}>
                    <label className="flex items-start gap-3">
                      <input
                        type="checkbox"
                        checked={!!picked[key]}
                        onChange={() => toggle(key)}
                        disabled={!present}
                        className="mt-1 h-4 w-4 cursor-pointer rounded border-slate-300"
                      />
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2">
                          <span className="text-sm font-medium text-slate-700">{FIELD_LABELS[key]}</span>
                          {conflict && (
                            <span className="rounded-full bg-amber-100 px-2 py-0.5 text-xs text-amber-700">将覆盖现有值</span>
                          )}
                          {!present && (
                            <span className="text-xs text-slate-400">未识别</span>
                          )}
                        </div>
                        {present && (
                          key === 'presentIllness' || key === 'prescription' || key === 'chiefComplaint' ? (
                            <textarea
                              value={edited[key] ?? ''}
                              onChange={e => setValue(key, e.target.value)}
                              rows={3}
                              className="input mt-1 w-full text-sm"
                            />
                          ) : (
                            <input
                              type="text"
                              value={edited[key] ?? ''}
                              onChange={e => setValue(key, e.target.value)}
                              className="input mt-1 w-full text-sm"
                            />
                          )
                        )}
                      </div>
                    </label>
                  </li>
                );
              })}
            </ul>
          </div>
        )}
      </div>

      <div className="flex justify-end gap-2 border-t border-slate-100 px-5 py-3">
        <button
          type="button"
          onClick={onClose}
          className="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-sm text-slate-700 hover:bg-slate-50"
        >
          取消
        </button>
        <button
          type="button"
          onClick={handleApply}
          disabled={!response || loading}
          className="rounded-lg bg-blue-600 px-3 py-1.5 text-sm font-medium text-white shadow hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
        >
          填入表单
        </button>
      </div>
    </div>
  );
}
