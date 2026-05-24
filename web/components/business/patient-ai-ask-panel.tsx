'use client';

import { useState } from 'react';
import { askPatientAi, AiConsentRequiredError, type AiPatientCitation, type AskPatientAiResponse } from '@/lib/api';
import { AiConsentModal } from './ai-consent-modal';

/**
 * Doctor-facing side panel for asking AI questions grounded in ONE patient's
 * records. Renders a simple chat bubble feed (user right, assistant left) plus
 * a clickable footnote list ([#1] [#2] …) that opens a side drawer showing the
 * cited chunk text + a deep-link back to the source record.
 *
 * Consent flow: when the backend returns 412 + AI_CONSENT_REQUIRED, we
 * surface {@link AiConsentModal}. On confirm we silently retry the last
 * question so the medic doesn't have to re-type it.
 *
 * Streaming is deliberately out of scope here — Phase 3 will add SSE.
 */
export type PatientAiAskPanelProps = {
  token: string;
  patientId: number | string;
  patientName?: string;
};

type ChatTurn =
  | { role: 'user'; text: string }
  | { role: 'assistant'; text: string; citations: AiPatientCitation[]; latencyMs: number };

function formatSourceLabel(c: AiPatientCitation): string {
  const map: Record<AiPatientCitation['sourceType'], string> = {
    medical_record: '病例',
    health_record: '健康档案',
    visit: '就诊记录',
  };
  return `${map[c.sourceType]} #${c.sourceId}（${c.fieldKey}）`;
}

function deepLinkFor(c: AiPatientCitation): string | null {
  switch (c.sourceType) {
    case 'medical_record': return `/medical-records?focus=${c.sourceId}`;
    case 'health_record':  return `/health-records?focus=${c.sourceId}`;
    case 'visit':          return `/visits?focus=${c.sourceId}`;
    default:               return null;
  }
}

export function PatientAiAskPanel({ token, patientId, patientName }: PatientAiAskPanelProps) {
  const [question, setQuestion] = useState('');
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [turns, setTurns] = useState<ChatTurn[]>([]);
  const [drawerCitation, setDrawerCitation] = useState<AiPatientCitation | null>(null);

  const [consentOpen, setConsentOpen] = useState(false);
  // Cached question to auto-resend after the patient grants consent.
  const [pendingAfterConsent, setPendingAfterConsent] = useState<string | null>(null);

  async function send(text: string) {
    if (!text.trim() || pending) return;
    setPending(true);
    setError(null);
    setTurns((prev) => [...prev, { role: 'user', text }]);
    try {
      const reply: AskPatientAiResponse = await askPatientAi(token, patientId, text);
      setTurns((prev) => [
        ...prev,
        { role: 'assistant', text: reply.answer, citations: reply.citations ?? [], latencyMs: reply.latencyMs },
      ]);
    } catch (err) {
      if (err instanceof AiConsentRequiredError) {
        // Stash the question so we can auto-resend after consent.
        setPendingAfterConsent(text);
        setConsentOpen(true);
        // Roll back the optimistic user bubble — the consent flow owns the retry.
        setTurns((prev) => prev.slice(0, -1));
      } else {
        setError(err instanceof Error ? err.message : '问询失败，请稍后重试');
      }
    } finally {
      setPending(false);
    }
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const text = question;
    setQuestion('');
    void send(text);
  }

  function handleConsented() {
    setConsentOpen(false);
    const retry = pendingAfterConsent;
    setPendingAfterConsent(null);
    if (retry) void send(retry);
  }

  return (
    <div className="flex h-full flex-col rounded-xl border border-slate-200 bg-white">
      <div className="border-b border-slate-100 px-4 py-3">
        <h3 className="text-sm font-semibold text-slate-900">AI 智能问询</h3>
        <p className="mt-1 text-xs text-slate-500">
          基于该患者的病例 / 健康档案 / 就诊记录回答；AI 不会编造资料里没有的事实。
        </p>
      </div>

      <div className="flex-1 space-y-3 overflow-y-auto px-4 py-3">
        {turns.length === 0 && !pending && (
          <div className="rounded-lg bg-slate-50 px-3 py-4 text-center text-xs text-slate-500">
            示例：「最近一次就诊的主诉是什么？」「过去开过哪些抗生素？」
          </div>
        )}

        {turns.map((turn, idx) =>
          turn.role === 'user' ? (
            <div key={idx} className="flex justify-end">
              <div className="max-w-[80%] rounded-lg bg-blue-600 px-3 py-2 text-sm text-white shadow-sm">
                {turn.text}
              </div>
            </div>
          ) : (
            <div key={idx} className="flex justify-start">
              <div className="max-w-[85%] space-y-2">
                <div className="rounded-lg bg-slate-100 px-3 py-2 text-sm whitespace-pre-wrap text-slate-800">
                  {turn.text}
                </div>
                {turn.citations.length > 0 && (
                  <div className="flex flex-wrap items-center gap-1 text-xs text-slate-500">
                    <span>引用：</span>
                    {turn.citations.map((c, i) => (
                      <button
                        key={c.chunkId}
                        type="button"
                        onClick={() => setDrawerCitation(c)}
                        className="rounded bg-slate-200 px-1.5 py-0.5 font-mono text-[11px] text-slate-700 hover:bg-slate-300"
                        title={formatSourceLabel(c)}
                      >
                        [#{i + 1}]
                      </button>
                    ))}
                    <span className="ml-2 text-slate-400">耗时 {Math.round(turn.latencyMs)}ms</span>
                  </div>
                )}
              </div>
            </div>
          )
        )}

        {pending && (
          <div className="flex justify-start">
            <div className="rounded-lg bg-slate-100 px-3 py-2 text-sm text-slate-500">
              识别中…
            </div>
          </div>
        )}

        {error && (
          <div className="rounded-lg bg-rose-50 px-3 py-2 text-xs text-rose-700 ring-1 ring-rose-200">
            {error}
          </div>
        )}
      </div>

      <form onSubmit={handleSubmit} className="flex gap-2 border-t border-slate-100 px-4 py-3">
        <input
          type="text"
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          placeholder="向 AI 提问关于该患者的问题…"
          disabled={pending}
          className="input flex-1 text-sm"
        />
        <button
          type="submit"
          disabled={pending || !question.trim()}
          className="rounded-lg bg-blue-600 px-3 py-1.5 text-sm font-medium text-white shadow hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
        >
          发送
        </button>
      </form>

      {drawerCitation && (
        <div className="fixed inset-y-0 right-0 z-40 flex w-full max-w-md flex-col border-l border-slate-200 bg-white shadow-2xl">
          <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4">
            <h3 className="text-base font-semibold text-slate-900">引用来源</h3>
            <button
              type="button"
              onClick={() => setDrawerCitation(null)}
              className="rounded-lg p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600"
              aria-label="关闭"
            >
              <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <div className="flex-1 space-y-3 overflow-y-auto px-5 py-4 text-sm">
            <div className="text-xs text-slate-500">
              {formatSourceLabel(drawerCitation)}
              {drawerCitation.sourceDate ? ` · ${drawerCitation.sourceDate}` : ''}
              {typeof drawerCitation.similarity === 'number'
                ? ` · 相似度 ${(drawerCitation.similarity * 100).toFixed(0)}%`
                : ''}
            </div>
            <div className="whitespace-pre-wrap rounded-lg bg-slate-50 px-3 py-2 text-slate-800 ring-1 ring-slate-200">
              {drawerCitation.snippet}
            </div>
            {deepLinkFor(drawerCitation) && (
              <a
                href={deepLinkFor(drawerCitation) ?? '#'}
                className="inline-block text-xs text-blue-600 hover:underline"
              >
                打开原始记录 →
              </a>
            )}
          </div>
        </div>
      )}

      <AiConsentModal
        open={consentOpen}
        patientId={patientId}
        patientName={patientName}
        token={token}
        onConsented={handleConsented}
        onClose={() => {
          setConsentOpen(false);
          setPendingAfterConsent(null);
        }}
      />
    </div>
  );
}
