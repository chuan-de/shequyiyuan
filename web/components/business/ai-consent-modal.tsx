'use client';

import { useState } from 'react';
import { grantAiConsent } from '@/lib/api';

/**
 * Modal shown the first time anyone asks an AI question about a patient that
 * hasn't yet granted AI processing consent (server returns 412 +
 * {@code AI_CONSENT_REQUIRED}). The patient (or admin) ticks the box, the
 * modal calls {@link grantAiConsent}, and {@code onConsented} fires so the
 * caller can retry the original question.
 *
 * Closing without consenting is fine — the patient just won't get an answer.
 */
export type AiConsentModalProps = {
  open: boolean;
  patientId: number | string;
  patientName?: string;
  token: string;
  onConsented: () => void;
  onClose: () => void;
};

export function AiConsentModal({ open, patientId, patientName, token, onConsented, onClose }: AiConsentModalProps) {
  const [agreed, setAgreed] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!open) return null;

  async function handleConfirm() {
    if (!agreed || submitting) return;
    setSubmitting(true);
    setError(null);
    try {
      await grantAiConsent(token, patientId);
      onConsented();
    } catch (err) {
      setError(err instanceof Error ? err.message : '签署失败，请稍后重试');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 px-4">
      <div className="w-full max-w-lg rounded-xl bg-white shadow-2xl">
        <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4">
          <h3 className="text-base font-semibold text-slate-900">AI 智能辅助 授权说明</h3>
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

        <div className="space-y-3 px-5 py-4 text-sm text-slate-700">
          <p>
            为了帮助医生快速了解
            {patientName ? `「${patientName}」` : '该患者'}
            的历史诊疗情况，本系统将启用 AI 智能辅助：
          </p>
          <ul className="ml-5 list-disc space-y-1 text-slate-600">
            <li>读取患者既往的病例、健康档案与就诊记录；</li>
            <li>调用第三方大模型服务（火山引擎方舟）生成参考回答；</li>
            <li>所发送的数据仅用于本次问询，不会用于模型训练；</li>
            <li>所有问询过程会留有审计日志，可联系系统管理员撤回授权。</li>
          </ul>
          <p className="text-slate-600">
            点击下方确认后，将视为本人或本人监护人已知悉并同意上述事项。
          </p>

          <label className="flex items-start gap-2 rounded-lg bg-slate-50 px-3 py-2 text-sm text-slate-700">
            <input
              type="checkbox"
              checked={agreed}
              onChange={(e) => setAgreed(e.target.checked)}
              className="mt-1 h-4 w-4 cursor-pointer rounded border-slate-300"
            />
            <span>我已阅读并同意上述 AI 智能辅助授权说明</span>
          </label>

          {error && (
            <div className="rounded-lg bg-rose-50 px-3 py-2 text-xs text-rose-700 ring-1 ring-rose-200">
              {error}
            </div>
          )}
        </div>

        <div className="flex justify-end gap-2 border-t border-slate-100 px-5 py-3">
          <button
            type="button"
            onClick={onClose}
            disabled={submitting}
            className="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-sm text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
          >
            取消
          </button>
          <button
            type="button"
            onClick={handleConfirm}
            disabled={!agreed || submitting}
            className="rounded-lg bg-blue-600 px-3 py-1.5 text-sm font-medium text-white shadow hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {submitting ? '签署中…' : '同意并启用 AI'}
          </button>
        </div>
      </div>
    </div>
  );
}
