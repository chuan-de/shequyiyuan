'use client';
import { useEffect, useMemo, useState } from 'react';
import { EntityManagementPage, EntityPageConfig } from '@/components/business/entity-management-page';
import { medicalRecordsPageConfig } from './entity-form-configs';
import { API_ROUTES, EntityRecord } from '@/lib/api-contract';
import { listEntities } from '@/lib/api';
import { readToken } from '@/lib/token-storage';
import { MultiFileUploader, AttachmentItem } from '@/components/ui/file-upload';
import { Select } from '@/components/ui/select';

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

function AttachmentsField({
  value, onChange,
}: {
  value: string;
  onChange: (next: string) => void;
}) {
  const items = safeParse<AttachmentItem[]>(value || '[]', []);
  return (
    <MultiFileUploader
      value={items}
      onChange={(next) => onChange(JSON.stringify(next))}
    />
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
          customRender: (value, onChange) => (
            <AttachmentsField value={value} onChange={onChange} />
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
