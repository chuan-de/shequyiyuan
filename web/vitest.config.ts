import { defineConfig } from 'vitest/config';
import path from 'node:path';

export default defineConfig({
  resolve: {
    // 对齐 tsconfig 的 "@/*" 路径别名
    alias: { '@': path.resolve(__dirname) },
  },
  test: {
    include: ['**/*.test.ts'],
    environment: 'node',
  },
});
