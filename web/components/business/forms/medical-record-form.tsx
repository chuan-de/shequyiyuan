'use client';
import { useEffect, useMemo, useState } from 'react';
import { EntityManagementPage, EntityPageConfig, CustomFieldContext } from '@/components/business/entity-management-page';
import { medicalRecordsPageConfig } from './entity-form-configs';
import { API_ROUTES, EntityRecord, AiVisionResponse, MedicalRecordFields } from '@/lib/api-contract';
import { listEntities, parseMedicalRecordPhoto } from '@/lib/api';
import { readToken } from '@/lib/token-storage';
import { MultiFileUploader, AttachmentItem } from '@/components/ui/file-upload';
import { Select } from '@/components/ui/select';
import { AiSuggestionPanel, SuggestionFieldKey } from '@/components/business/ai-suggestion-panel';

type PrescriptionItem = { medicationId: number; name: string; quantity: number };

function safeParse<T>(s: string, fallback: T): T {
  try { return JSON.parse(s) as T; } catch { return fallback; }
}

function PrescriptionPicker({
  value, onChange, medications,
}: {
  value: string;
  onChange: (next: string) => void;
  medications: EntityRecord[];
}) {
  const items = safeParse<PrescriptionItem[]>(value || '[]', []);
  const update = (next: PrescriptionItem[]) => onChange(JSON.stringify(next));

  function add() {
    if (medications.length === 0) return;
    const first = medications[0];
    update([...items, { medicationId: Number(first.id), name: String(first.name ?? first.id), quantity: 1 }]);
  }
  function remove(idx: number) { update(items.filter((_, i) => i !== idx)); }
  function setMed(idx: number, medId: number) {
    const m = medications.find(x => Number(x.id) === medId);
    update(items.map((it, i) => i === idx ? { ...it, medicationId: medId, name: String(m?.name ?? medId) } : it));
  }
  function setQty(idx: number, qty: number) {
    update(items.map((it, i) => i === idx ? { ...it, quantity: qty } : it));
  }

  return (
    <div className="space-y-2">
      {items.length === 0 && <p className="text-xs text-slate-400">暂无药品，点击下方按钮添加</p>}
      {items.map((it, idx) => (
        <div key={idx} className="flex items-center gap-2">
          <Select
            value={String(it.medicationId)}
            onChange={(v) => setMed(idx, Number(v))}
            options={medications.map(m => ({ value: String(m.id), label: String(m.name ?? m.id) }))}
            placeholder="请选择药品"
            className="flex-1"
          />
          <input
            type="number"
            min={1}
            value={it.quantity}
            onChange={e => setQty(idx, Number(e.target.value) || 1)}
            className="input w-24"
            placeholder="数量"
          />
          <button type="button" onClick={() => remove(idx)} className="rounded-lg border border-rose-200 bg-white px-3 py-1.5 text-sm text-rose-600 hover:bg-rose-50">移除</button>
        </div>
      ))}
      <button
        type="button"
        onClick={add}
        disabled={medications.length === 0}
        className="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-sm text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
      >
        + 添加药品
      </button>
    </div>
  );
}

/**
 * Pull the photo UUID out of the photo URL stored on an attachment.
 * URLs follow the shape /api/v1/photos/{uuid}/content (or the absolute
 * equivalent), so we match the segment between /photos/ and /content.
 */
function extractPhotoId(url: string): string | null {
  const m = url.match(/\/photos\/([0-9a-fA-F-]{36})(?:\/content)?/);
  return m ? m[1] : null;
}

/**
 * Field-to-target mapping: where each AI suggestion lands in the form.
 *
 * The form binds patient / doctor / gender / age via dropdowns + lookups,
 * not free text, so we skip those keys here — the medic will pick the
 * matching record manually. chiefComplaint + presentIllness both fold into
 * the single conditionDesc textarea (concatenated on apply).
 */
const FIELD_TO_FORM: Partial<Record<SuggestionFieldKey, string>> = {
  visitDate: 'recordDate',
  department: 'examItems',
  chiefComplaint: 'conditionDesc',
  presentIllness: 'conditionDesc',
  diagnosis: 'caseName',
  prescription: 'examResults',
};

function AttachmentsField({
  value, onChange, ctx,
}: {
  value: string;
  onChange: (next: string) => void;
  ctx: CustomFieldContext;
}) {
  const items = safeParse<AttachmentItem[]>(value || '[]', []);
  const canUseAi = ctx.permissions.includes('ai:vision');

  const [panelOpen, setPanelOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [response, setResponse] = useState<AiVisionResponse | null>(null);

  async function runAiOnImage(url: string) {
    const photoId = extractPhotoId(url);
    if (!photoId) {
      setError('无法从图片地址识别 photoId');
      setPanelOpen(true);
      return;
    }
    setPanelOpen(true);
    setLoading(true);
    setError(null);
    setResponse(null);
    try {
      const t = readToken();
      if (!t) throw new Error('登录已过期');
      const result = await parseMedicalRecordPhoto(t.accessToken, { photoId });
      setResponse(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : '识别失败');
    } finally {
      setLoading(false);
    }
  }

  function applySuggestions(picked: Partial<MedicalRecordFields>) {
    // Translate AI field keys into the form's actual field keys, merging
    // chiefComplaint + presentIllness into the single conditionDesc field
    // when both are picked.
    const patch: Record<string, string> = {};
    const condParts: string[] = [];
    Object.entries(picked).forEach(([key, val]) => {
      if (val === null || val === undefined || val === '') return;
      const targetKey = FIELD_TO_FORM[key as SuggestionFieldKey];
      if (!targetKey) return;
      let coerced = String(val);
      // The form's recordDate uses <input type="datetime-local"> which expects
      // YYYY-MM-DDTHH:mm. Promote a bare date to noon local time so the date
      // doesn't slip a day under TZ conversion.
      if (targetKey === 'recordDate' && /^\d{4}-\d{2}-\d{2}$/.test(coerced)) {
        coerced = coerced + 'T12:00';
      }
      if (targetKey === 'conditionDesc') {
        condParts.push(coerced);
      } else {
        patch[targetKey] = coerced;
      }
    });
    if (condParts.length > 0) {
      const existing = ctx.getFormValue('conditionDesc');
      patch.conditionDesc = existing ? existing + '\n' + condParts.join('\n') : condParts.join('\n');
    }
    ctx.patchForm(patch);
    setPanelOpen(false);
  }

  function formHasValueFor(key: SuggestionFieldKey): boolean {
    const targetKey = FIELD_TO_FORM[key];
    if (!targetKey) return false;
    const value = ctx.getFormValue(targetKey);
    return value !== undefined && value !== null && value !== '';
  }

  return (
    <div className="space-y-2">
      <MultiFileUploader
        value={items}
        onChange={(next) => onChange(JSON.stringify(next))}
      />
      {canUseAi && items.length > 0 && (
        <div className="flex flex-wrap gap-2 pt-1">
          {items.map((it, idx) => {
            const isImg = it.contentType?.startsWith('image/');
            if (!isImg) return null;
            return (
              <button
                key={idx}
                type="button"
                onClick={() => runAiOnImage(it.url)}
                className="rounded-lg border border-blue-200 bg-blue-50 px-2 py-1 text-xs font-medium text-blue-700 hover:bg-blue-100"
                title={it.filename}
              >
                AI 识别：{it.filename}
              </button>
            );
          })}
        </div>
      )}
      <AiSuggestionPanel
        open={panelOpen}
        loading={loading}
        error={error}
        response={response}
        formHasValueFor={formHasValueFor}
        onApply={applySuggestions}
        onClose={() => setPanelOpen(false)}
      />
    </div>
  );
}

export function MedicalRecordManagementPage() {
  const [doctors, setDoctors] = useState<EntityRecord[]>([]);
  const [medications, setMedications] = useState<EntityRecord[]>([]);
  const [patients, setPatients] = useState<EntityRecord[]>([]);

  useEffect(() => {
    const t = readToken();
    if (!t) return;
    listEntities(t.accessToken, API_ROUTES.doctors, { page: 1, size: 200 })
      .then(res => setDoctors(res.records))
      .catch(() => setDoctors([]));
    listEntities(t.accessToken, API_ROUTES.medications, { page: 1, size: 500 })
      .then(res => setMedications(res.records))
      .catch(() => setMedications([]));
    listEntities(t.accessToken, API_ROUTES.patients, { page: 1, size: 500 })
      .then(res => setPatients(res.records))
      .catch(() => setPatients([]));
  }, []);

  const config: EntityPageConfig = useMemo(() => {
    const doctorOptions = doctors.map(d => ({
      value: String(d.id),
      label: String(d.fullName ?? d.username ?? d.id),
    }));
    const doctorIdToName = new Map(doctors.map(d => [Number(d.id), String(d.fullName ?? d.username ?? d.id)]));
    const doctorById = new Map(doctors.map(d => [Number(d.id), d]));

    const patientOptions = patients.map(p => ({
      value: String(p.id),
      label: String(p.fullName ?? p.username ?? p.id),
    }));
    const patientIdToName = new Map(patients.map(p => [Number(p.id), String(p.fullName ?? p.username ?? p.id)]));
    const patientById = new Map(patients.map(p => [Number(p.id), p]));

    const enrich = (form: Record<string, string>, base: Record<string, unknown>) => {
      const d = doctorById.get(Number(form.doctorId));
      const p = patientById.get(Number(form.patientId));
      const result: Record<string, unknown> = { ...base };
      if (d) {
        result.doctorUuidNumber = d.uuidNumber ?? undefined;
        result.doctorName = d.fullName ?? d.username ?? undefined;
        result.doctorPhone = d.phone ?? undefined;
        result.doctorIdNumber = d.idNumber ?? undefined;
        result.doctorEmail = d.email ?? undefined;
      }
      if (p) {
        result.patientName = p.fullName ?? p.username ?? undefined;
        result.patientPhone = p.phone ?? undefined;
        result.patientIdNumber = p.idNumber ?? undefined;
        result.patientEmail = p.email ?? undefined;
      }
      return result;
    };

    return {
      ...medicalRecordsPageConfig,
      columns: medicalRecordsPageConfig.columns.map(c => {
        if (c.key === 'doctorName') return {
          ...c,
          render: (row) => row.doctorName ? String(row.doctorName) : (doctorIdToName.get(Number(row.doctorId)) ?? '—'),
        };
        if (c.key === 'patientName') return {
          ...c,
          render: (row) => row.patientName ? String(row.patientName) : (patientIdToName.get(Number(row.patientId)) ?? '—'),
        };
        return c;
      }),
      formFields: medicalRecordsPageConfig.formFields.map(f => {
        if (f.key === 'doctorId') return { ...f, type: 'select' as const, options: doctorOptions };
        if (f.key === 'patientId') return { ...f, type: 'select' as const, options: patientOptions };
        if (f.key === 'prescriptionItems') return {
          ...f,
          customRender: (value, onChange) => (
            <PrescriptionPicker value={value} onChange={onChange} medications={medications} />
          ),
        };
        if (f.key === 'attachments') return {
          ...f,
          customRender: (value, onChange, _mode, ctx) => (
            <AttachmentsField value={value} onChange={onChange} ctx={ctx} />
          ),
        };
        return f;
      }),
      createPayload: (form) => enrich(form, medicalRecordsPageConfig.createPayload(form)),
      updatePayload: (form, row) => enrich(form, medicalRecordsPageConfig.updatePayload(form, row)),
    };
  }, [doctors, medications, patients]);

  return <EntityManagementPage config={config} />;
}
