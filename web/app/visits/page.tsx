import { EntityManagementPage } from '@/components/business/entity-management-page';
import { visitPageConfig } from '@/components/business/forms/visit-form';

export default function Page() {
  return <EntityManagementPage config={visitPageConfig} />;
}
