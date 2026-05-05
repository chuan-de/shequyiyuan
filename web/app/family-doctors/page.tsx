import { EntityManagementPage } from '@/components/business/entity-management-page';
import { API_ROUTES } from '@/lib/api-contract';

export default function Page() {
  return <EntityManagementPage title='family-doctors' route={API_ROUTES.familyDoctors} permissionPrefix='family-doctors' />;
}
