'use client';
import { useEffect, useMemo, useState } from 'react';
import { EntityManagementPage, EntityPageConfig } from '@/components/business/entity-management-page';
import { medicationsPageConfig } from '@/components/business/forms/entity-form-configs';
import { configNumber, getEffectiveConfigs } from '@/lib/system-config';
import { readToken } from '@/lib/token-storage';

const DEFAULT_LOW_STOCK = 10;

export default function Page() {
  // 低库存阈值来自「系统配置」medication.low-stock-threshold，管理员可调。
  const [threshold, setThreshold] = useState<number>(DEFAULT_LOW_STOCK);

  useEffect(() => {
    const t = readToken();
    if (!t) return;
    getEffectiveConfigs(t.accessToken)
      .then((configs) => {
        const n = configNumber(configs, 'medication.low-stock-threshold');
        if (n !== null && n >= 0) setThreshold(n);
      })
      .catch(() => {});
  }, []);

  const config: EntityPageConfig = useMemo(() => ({
    ...medicationsPageConfig,
    columns: medicationsPageConfig.columns.map((c) =>
      c.key === 'stock'
        ? {
            ...c,
            render: (row) => {
              const stock = Number(row.stock ?? 0);
              return (
                <span className="inline-flex items-center gap-1.5">
                  {stock}
                  {stock <= threshold && (
                    <span className="badge badge-red" title={`库存 ≤ ${threshold}（系统配置 medication.low-stock-threshold）`}>
                      库存不足
                    </span>
                  )}
                </span>
              );
            },
          }
        : c,
    ),
  }), [threshold]);

  return <EntityManagementPage config={config} />;
}
