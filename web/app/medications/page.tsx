import { EntityManagementPage } from '@/components/business/entity-management-page';
import { API_ROUTES } from '@/lib/api-contract';

export default function Page() {
  return <EntityManagementPage title='medications' route={API_ROUTES.medications} permissionPrefix='medications' />;
}
