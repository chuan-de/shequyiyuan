import { EntityManagementPage } from '@/components/business/entity-management-page';
import { API_ROUTES } from '@/lib/api-contract';

export default function Page() {
  return <EntityManagementPage title='doctors' route={API_ROUTES.doctors} permissionPrefix='doctors' />;
}
