import type { CurrentUserResponse } from '@/lib/api';

export function hasPermission(user: CurrentUserResponse | null, permission: string): boolean {
  return Boolean(user?.permissions?.includes(permission));
}
