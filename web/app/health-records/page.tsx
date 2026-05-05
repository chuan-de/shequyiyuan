import { EntityManagementPage } from '@/components/business/entity-management-page';
import { API_ROUTES } from '@/lib/api-contract';

export default function Page() {
  return <EntityManagementPage title='health-records' route={API_ROUTES.healthRecords} permissionPrefix='health-records' />;
}
