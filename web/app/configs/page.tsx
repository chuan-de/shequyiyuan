import { EntityManagementPage } from '@/components/business/entity-management-page';
import { API_ROUTES } from '@/lib/api-contract';

export default function Page() {
  return <EntityManagementPage title='configs' route={API_ROUTES.configs} permissionPrefix='configs' />;
}
