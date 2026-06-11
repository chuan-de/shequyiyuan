import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { clearToken, readToken, writeToken } from './token-storage';

// token-storage 通过 typeof window 判断环境，这里手工注入最小 window/localStorage。
function stubBrowserGlobals() {
  const store = new Map<string, string>();
  const localStorage = {
    getItem: (k: string) => store.get(k) ?? null,
    setItem: (k: string, v: string) => void store.set(k, v),
    removeItem: (k: string) => void store.delete(k),
  };
  vi.stubGlobal('window', { localStorage });
  vi.stubGlobal('localStorage', localStorage);
}

describe('token-storage（浏览器环境）', () => {
  beforeEach(stubBrowserGlobals);
  afterEach(() => vi.unstubAllGlobals());

  it('write 后 read 取回同一 token', () => {
    writeToken({ accessToken: 'abc123', tokenType: 'Bearer' });
    expect(readToken()).toEqual({ accessToken: 'abc123', tokenType: 'Bearer' });
  });

  it('未写入时 read 返回 null', () => {
    expect(readToken()).toBeNull();
  });

  it('两个键缺一即视为未登录', () => {
    writeToken({ accessToken: 'abc123', tokenType: 'Bearer' });
    localStorage.removeItem('token_type');
    expect(readToken()).toBeNull();
  });

  it('clear 后 read 返回 null', () => {
    writeToken({ accessToken: 'abc123', tokenType: 'Bearer' });
    clearToken();
    expect(readToken()).toBeNull();
  });
});

describe('token-storage（SSR，无 window）', () => {
  it('read 返回 null、write/clear 不抛错', () => {
    expect(readToken()).toBeNull();
    expect(() => writeToken({ accessToken: 'x', tokenType: 'Bearer' })).not.toThrow();
    expect(() => clearToken()).not.toThrow();
  });
});
