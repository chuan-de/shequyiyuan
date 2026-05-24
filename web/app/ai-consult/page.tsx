'use client';

/**
 * Community AI consult — main chat surface.
 *
 * Layout:
 *  - Left  (280px): session list, "New conversation" button, per-row rename/delete actions.
 *  - Right (flex):  collapsible disclaimer, message stream, input area at the bottom.
 *
 * The streaming reply is rendered as the LAST assistant bubble. We append every
 * `delta` from the SSE callback onto its content; a `▍` cursor is shown while
 * the stream is still open. Failures (rate limit, upstream) surface as red
 * banners under the bubble rather than throwing the user back to the list.
 *
 * Permission gating happens at two levels:
 *  1. Sidebar entry only shows if the user has `ai:consult`.
 *  2. The page itself redirects to /login when no token is present and shows a
 *     friendly forbidden state if the API replies 403 (e.g. role revoked).
 */

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useRouter } from 'next/navigation';
import { AppShell } from '@/components/layout/app-shell';
import {
  createConsultSession,
  deleteConsultSession,
  getConsultSession,
  listConsultSessions,
  renameConsultSession,
  streamConsultMessage,
  type AiConsultMessage,
  type AiConsultSessionDetail,
  type AiConsultSessionSummary,
} from '@/lib/api';
import { readToken } from '@/lib/token-storage';

const SUGGESTIONS = [
  '我最近老头疼，可能是什么原因？',
  '感冒可以吃什么药？',
  '高血压日常注意什么？',
];

type DraftMessage = AiConsultMessage & { streaming?: boolean };

export default function AiConsultPage() {
  const router = useRouter();
  const [token, setToken] = useState<string | null>(null);
  const [sessions, setSessions] = useState<AiConsultSessionSummary[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [detail, setDetail] = useState<AiConsultSessionDetail | null>(null);
  const [draftMessages, setDraftMessages] = useState<DraftMessage[]>([]);
  const [input, setInput] = useState('');
  const [streaming, setStreaming] = useState(false);
  const [showDisclaimer, setShowDisclaimer] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [renamingId, setRenamingId] = useState<number | null>(null);
  const [renameValue, setRenameValue] = useState('');
  const scrollRef = useRef<HTMLDivElement | null>(null);

  // ----- bootstrap ----------------------------------------------------------

  useEffect(() => {
    const t = readToken();
    if (!t) { router.replace('/login'); return; }
    setToken(t.accessToken);
  }, [router]);

  const refreshSessions = useCallback(async (tok: string) => {
    try {
      const page = await listConsultSessions(tok, { page: 0, size: 50 });
      setSessions(page.records);
      return page.records;
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载会话列表失败');
      return [];
    }
  }, []);

  useEffect(() => {
    if (!token) return;
    refreshSessions(token).then(rows => {
      if (rows.length > 0 && selectedId === null) setSelectedId(rows[0].id);
    });
    // selectedId intentionally excluded — we only want to auto-pick on first load.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, refreshSessions]);

  useEffect(() => {
    if (!token || selectedId === null) { setDetail(null); setDraftMessages([]); return; }
    let cancelled = false;
    getConsultSession(token, selectedId)
      .then(d => { if (!cancelled) { setDetail(d); setDraftMessages(d.messages); } })
      .catch(err => { if (!cancelled) setError(err instanceof Error ? err.message : '加载会话失败'); });
    return () => { cancelled = true; };
  }, [token, selectedId]);

  // Auto-scroll to bottom whenever messages or streaming content change.
  useEffect(() => {
    const el = scrollRef.current;
    if (el) el.scrollTop = el.scrollHeight;
  }, [draftMessages, streaming]);

  // ----- session ops --------------------------------------------------------

  async function handleNewSession() {
    if (!token) return;
    try {
      const s = await createConsultSession(token);
      setSessions(prev => [{ ...s, messageCount: 0 }, ...prev]);
      setSelectedId(s.id);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : '创建会话失败');
    }
  }

  async function handleDeleteSession(id: number) {
    if (!token) return;
    if (!confirm('确认删除这个会话？历史消息会一并清除。')) return;
    const prev = sessions;
    setSessions(prev.filter(s => s.id !== id)); // optimistic
    if (selectedId === id) setSelectedId(null);
    try {
      await deleteConsultSession(token, id);
    } catch (err) {
      setSessions(prev); // rollback
      setError(err instanceof Error ? err.message : '删除失败');
    }
  }

  async function commitRename(id: number) {
    if (!token) return;
    const next = renameValue.trim();
    setRenamingId(null);
    if (!next) return;
    const prev = sessions;
    setSessions(prev.map(s => s.id === id ? { ...s, title: next } : s));
    try {
      await renameConsultSession(token, id, next);
    } catch (err) {
      setSessions(prev);
      setError(err instanceof Error ? err.message : '重命名失败');
    }
  }

  // ----- send + stream ------------------------------------------------------

  async function sendMessage(text?: string) {
    if (!token) return;
    const content = (text ?? input).trim();
    if (!content || streaming) return;

    let sessionId = selectedId;
    if (sessionId === null) {
      try {
        const s = await createConsultSession(token);
        setSessions(prev => [{ ...s, messageCount: 0 }, ...prev]);
        sessionId = s.id;
        setSelectedId(s.id);
      } catch (err) {
        setError(err instanceof Error ? err.message : '创建会话失败');
        return;
      }
    }
    setInput('');
    setError(null);

    // Optimistic user bubble + an empty assistant bubble we'll stream into.
    const tempUserId = -Date.now();
    const tempAsstId = tempUserId - 1;
    const nowIso = new Date().toISOString();
    setDraftMessages(prev => [
      ...prev,
      { id: tempUserId, role: 'user', content, tokensIn: null, tokensOut: null, model: null, status: 'completed', createdAt: nowIso },
      { id: tempAsstId, role: 'assistant', content: '', tokensIn: null, tokensOut: null, model: null, status: 'completed', createdAt: nowIso, streaming: true },
    ]);
    setStreaming(true);

    await streamConsultMessage(token, sessionId, content, {
      onChunk: (delta) => {
        setDraftMessages(prev => prev.map(m => m.id === tempAsstId ? { ...m, content: m.content + delta } : m));
      },
      onDone: (meta) => {
        setDraftMessages(prev => prev.map(m => m.id === tempAsstId ? {
          ...m,
          id: meta.messageId,
          tokensIn: meta.tokensIn,
          tokensOut: meta.tokensOut,
          status: meta.failed ? 'failed' : (meta.refused ? 'refused_by_guardrail' : 'completed'),
          streaming: false,
        } : m));
        setStreaming(false);
        // Bump the session to the top of the list with a fresh updatedAt.
        if (sessionId !== null) {
          setSessions(prev => {
            const idx = prev.findIndex(s => s.id === sessionId);
            if (idx === -1) return prev;
            const updated = { ...prev[idx], updatedAt: new Date().toISOString(), messageCount: prev[idx].messageCount + 2 };
            const rest = [...prev.slice(0, idx), ...prev.slice(idx + 1)];
            return [updated, ...rest];
          });
        }
      },
      onError: (err) => {
        setDraftMessages(prev => prev.map(m => m.id === tempAsstId ? {
          ...m, content: m.content || err.message, status: 'failed', streaming: false,
        } : m));
        setStreaming(false);
        setError(err.message);
      },
    });
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  }

  // ----- render -------------------------------------------------------------

  const currentTitle = useMemo(() => {
    if (selectedId === null) return '社区 AI 问诊';
    const s = sessions.find(s => s.id === selectedId);
    return s?.title ?? detail?.title ?? '社区 AI 问诊';
  }, [sessions, selectedId, detail]);

  return (
    <AppShell title="社区 AI 问诊" description="基于火山方舟 · Doubao 模型，仅供健康咨询参考">
      <div className="flex h-[calc(100vh-180px)] gap-4">
        {/* Left rail */}
        <aside className="w-[280px] flex-shrink-0 table-wrap flex flex-col overflow-hidden">
          <div className="border-b border-slate-200 p-3">
            <button
              className="btn-primary w-full"
              onClick={handleNewSession}
              type="button"
            >
              + 新对话
            </button>
          </div>
          <div className="flex-1 overflow-y-auto p-2 space-y-1">
            {sessions.length === 0 ? (
              <p className="hint p-3 text-center">暂无会话，点击上方按钮开始</p>
            ) : sessions.map(s => (
              <div
                key={s.id}
                className={`group rounded-lg border px-3 py-2 cursor-pointer transition ${
                  selectedId === s.id
                    ? 'border-brand bg-brand/5'
                    : 'border-transparent hover:bg-slate-100'
                }`}
                onClick={() => setSelectedId(s.id)}
              >
                {renamingId === s.id ? (
                  <input
                    autoFocus
                    className="w-full rounded border border-slate-300 bg-white px-2 py-1 text-sm"
                    value={renameValue}
                    onChange={(e) => setRenameValue(e.target.value)}
                    onBlur={() => commitRename(s.id)}
                    onKeyDown={(e) => { if (e.key === 'Enter') commitRename(s.id); if (e.key === 'Escape') setRenamingId(null); }}
                    onClick={(e) => e.stopPropagation()}
                  />
                ) : (
                  <>
                    <p className="text-sm font-medium text-slate-800 truncate">{s.title || '新对话'}</p>
                    <div className="mt-0.5 flex items-center justify-between">
                      <span className="text-xs text-slate-400">
                        {new Date(s.updatedAt).toLocaleString('zh-CN', { hour12: false })}
                      </span>
                      <div className="hidden group-hover:flex gap-1">
                        <button
                          type="button"
                          title="重命名"
                          className="text-xs text-slate-500 hover:text-brand"
                          onClick={(e) => { e.stopPropagation(); setRenamingId(s.id); setRenameValue(s.title); }}
                        >
                          重命名
                        </button>
                        <button
                          type="button"
                          title="删除"
                          className="text-xs text-slate-500 hover:text-rose-500"
                          onClick={(e) => { e.stopPropagation(); handleDeleteSession(s.id); }}
                        >
                          删除
                        </button>
                      </div>
                    </div>
                  </>
                )}
              </div>
            ))}
          </div>
        </aside>

        {/* Right pane */}
        <section className="flex-1 table-wrap flex flex-col overflow-hidden">
          <header className="border-b border-slate-200 px-5 py-3 flex items-center gap-3">
            <h2 className="font-semibold text-slate-800 truncate flex-1">{currentTitle}</h2>
            <button
              type="button"
              className="text-xs text-slate-500 hover:text-brand"
              onClick={() => setShowDisclaimer(v => !v)}
            >
              {showDisclaimer ? '收起声明' : 'ⓘ AI 仅供参考'}
            </button>
          </header>
          {showDisclaimer && (
            <div className="border-b border-amber-200 bg-amber-50 px-5 py-3 text-sm text-amber-800">
              AI 给出的建议基于通用医学常识，不构成专业诊疗意见。请勿据此自行用药或停药，
              如有持续不适请及时前往医院就诊。
            </div>
          )}

          {/* Messages */}
          <div ref={scrollRef} className="flex-1 overflow-y-auto px-5 py-4 space-y-4">
            {(!detail && selectedId !== null) ? (
              <p className="hint text-center">加载中…</p>
            ) : draftMessages.length === 0 ? (
              <EmptyState onPick={(q) => sendMessage(q)} />
            ) : draftMessages.map(m => (
              <MessageBubble key={m.id} message={m} />
            ))}
          </div>

          {error && (
            <div className="mx-5 mb-2 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
              {error}
            </div>
          )}

          {/* Input */}
          <div className="border-t border-slate-200 p-3">
            <div className="flex gap-2">
              <textarea
                rows={2}
                className="flex-1 resize-none rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm focus:border-brand focus:outline-none"
                placeholder="描述你的症状 / 健康问题（Enter 发送，Shift+Enter 换行）"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={handleKeyDown}
                disabled={streaming}
              />
              <button
                type="button"
                className="btn-primary self-end"
                disabled={streaming || !input.trim()}
                onClick={() => sendMessage()}
              >
                {streaming ? '生成中…' : '发送'}
              </button>
            </div>
          </div>
        </section>
      </div>
    </AppShell>
  );
}

function EmptyState({ onPick }: { onPick: (q: string) => void }) {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-center space-y-4">
      <div className="text-5xl">🤖</div>
      <p className="text-base font-medium text-slate-700">你好，我是社区医院 AI 健康助手</p>
      <p className="hint">不构成诊断意见，请遵医嘱用药。试试这些问题：</p>
      <div className="flex flex-wrap justify-center gap-2 pt-2">
        {SUGGESTIONS.map(q => (
          <button
            key={q}
            type="button"
            onClick={() => onPick(q)}
            className="rounded-full border border-slate-300 bg-white px-3 py-1.5 text-sm text-slate-700 hover:border-brand hover:text-brand transition"
          >
            {q}
          </button>
        ))}
      </div>
    </div>
  );
}

function MessageBubble({ message }: { message: DraftMessage }) {
  const isUser = message.role === 'user';
  const refused = message.status === 'refused_by_guardrail';
  const failed = message.status === 'failed';
  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
      <div
        className={`max-w-[80%] rounded-2xl px-4 py-2.5 text-sm whitespace-pre-wrap leading-relaxed ${
          isUser
            ? 'bg-brand text-white rounded-br-sm'
            : refused
              ? 'bg-amber-50 text-amber-800 border border-amber-200 rounded-bl-sm'
              : failed
                ? 'bg-rose-50 text-rose-800 border border-rose-200 rounded-bl-sm'
                : 'bg-slate-100 text-slate-800 rounded-bl-sm'
        }`}
      >
        {message.content || (message.streaming ? ' ' : '')}
        {message.streaming && (
          <span className="ml-0.5 animate-pulse text-slate-400">▍</span>
        )}
      </div>
    </div>
  );
}
