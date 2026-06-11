import { describe, expect, it } from 'vitest';
import { hasPermission } from './permissions';
import type { CurrentUserResponse } from '@/lib/api';

const user = (permissions: string[]): CurrentUserResponse => ({
  username: 'tester',
  enabled: true,
  roles: ['ADMIN'],
  permissions,
});

describe('hasPermission', () => {
  it('拥有权限码时返回 true', () => {
    expect(hasPermission(user(['visits:read', 'visits:write']), 'visits:write')).toBe(true);
  });

  it('没有权限码时返回 false', () => {
    expect(hasPermission(user(['visits:read']), 'visits:write')).toBe(false);
  });

  it('权限码必须整串匹配，不做前缀匹配', () => {
    expect(hasPermission(user(['visits:read']), 'visits')).toBe(false);
  });

  it('user 为 null（未登录）时返回 false', () => {
    expect(hasPermission(null, 'visits:read')).toBe(false);
  });

  it('permissions 为空数组时返回 false', () => {
    expect(hasPermission(user([]), 'visits:read')).toBe(false);
  });
});
