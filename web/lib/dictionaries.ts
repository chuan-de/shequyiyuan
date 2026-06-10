'use client';

import { useEffect, useState } from 'react';
import { listEnabledDictionaryItems, type DictionaryItemResponse } from './api';

/**
 * 字典消费层：业务表单/列表通过这里读取启用的字典项。
 * 模块级缓存（5 分钟 TTL）避免同页多个字段重复请求同一字典。
 */

const TTL_MS = 5 * 60 * 1000;

type CacheEntry = { items: DictionaryItemResponse[]; fetchedAt: number };
const cache = new Map<string, CacheEntry>();
const inflight = new Map<string, Promise<DictionaryItemResponse[]>>();

export async function getDictItems(token: string, dictCode: string): Promise<DictionaryItemResponse[]> {
  const cached = cache.get(dictCode);
  if (cached && Date.now() - cached.fetchedAt < TTL_MS) return cached.items;

  const pending = inflight.get(dictCode);
  if (pending) return pending;

  const promise = listEnabledDictionaryItems(token, dictCode)
    .then((items) => {
      cache.set(dictCode, { items, fetchedAt: Date.now() });
      return items;
    })
    .finally(() => inflight.delete(dictCode));
  inflight.set(dictCode, promise);
  return promise;
}

/** 管理端改完字典后调用，强制业务页下次重新拉取。 */
export function invalidateDictCache(dictCode?: string) {
  if (dictCode) cache.delete(dictCode);
  else cache.clear();
}

export type DictMaps = {
  /** dictCode → 启用项列表（含 value/name/sortOrder） */
  items: Record<string, DictionaryItemResponse[]>;
  /** dictCode → (itemCode → itemName)，码值统一转 string 后比对 */
  labelOf: (dictCode: string, code: unknown) => string | null;
};

/**
 * 一次加载多个字典。dictCodes 用稳定引用或字面量数组（内部按 join key 比较）。
 */
export function useDictionaries(token: string, dictCodes: string[]): DictMaps {
  const [items, setItems] = useState<Record<string, DictionaryItemResponse[]>>({});
  const key = dictCodes.slice().sort().join(',');

  useEffect(() => {
    if (!token || !key) return;
    let cancelled = false;
    Promise.all(key.split(',').map(async (code) => [code, await getDictItems(token, code)] as const))
      .then((entries) => {
        if (!cancelled) setItems(Object.fromEntries(entries));
      })
      .catch(() => {
        // 字典加载失败时业务页降级为显示原始码值，不阻塞主流程。
      });
    return () => { cancelled = true; };
  }, [token, key]);

  return {
    items,
    labelOf: (dictCode, code) => {
      if (code === null || code === undefined || code === '') return null;
      const found = items[dictCode]?.find((i) => i.value === String(code));
      return found ? found.name : null;
    },
  };
}
