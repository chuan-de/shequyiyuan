'use client';

import { useEffect, useState } from 'react';
import { fetchEffectiveConfigs } from './api';

/**
 * 系统配置消费层：读取后端 /api/v1/configs/effective 白名单运行参数。
 * 已登记的 key（与后端 DefaultSystemConfigService.EFFECTIVE_KEY_WHITELIST 对应）：
 *   system.announcement            首页公告（空/缺省 = 不显示）
 *   medication.low-stock-threshold 药品低库存预警阈值（数字）
 *   visit.default-fee              新建就诊默认挂号费（数字）
 */

const TTL_MS = 60 * 1000;

let cached: { values: Record<string, string>; fetchedAt: number } | null = null;
let inflight: Promise<Record<string, string>> | null = null;

export async function getEffectiveConfigs(token: string): Promise<Record<string, string>> {
  if (cached && Date.now() - cached.fetchedAt < TTL_MS) return cached.values;
  if (inflight) return inflight;
  inflight = fetchEffectiveConfigs(token)
    .then((values) => {
      cached = { values, fetchedAt: Date.now() };
      return values;
    })
    .finally(() => { inflight = null; });
  return inflight;
}

export function invalidateConfigCache() { cached = null; }

export function useEffectiveConfigs(token: string): Record<string, string> | null {
  const [values, setValues] = useState<Record<string, string> | null>(null);
  useEffect(() => {
    if (!token) return;
    let cancelled = false;
    getEffectiveConfigs(token)
      .then((v) => { if (!cancelled) setValues(v); })
      .catch(() => { if (!cancelled) setValues({}); });
    return () => { cancelled = true; };
  }, [token]);
  return values;
}

export function configNumber(values: Record<string, string> | null, key: string): number | null {
  const raw = values?.[key];
  if (raw === undefined || raw === null || raw.trim() === '') return null;
  const n = Number(raw);
  return Number.isFinite(n) ? n : null;
}
