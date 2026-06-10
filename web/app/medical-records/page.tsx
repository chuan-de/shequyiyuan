'use client';
import { Suspense } from 'react';
import { MedicalRecordManagementPage } from '@/components/business/forms/medical-record-form';

// useSearchParams（写病历跨页预填）要求 Suspense 边界。
export default function Page() {
  return (
    <Suspense>
      <MedicalRecordManagementPage />
    </Suspense>
  );
}
