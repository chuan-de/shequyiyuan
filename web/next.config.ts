import type { NextConfig } from 'next';

const nextConfig: NextConfig = {
  reactStrictMode: true,
  devIndicators: false,
  // Docker 镜像构建时输出自包含产物（node .next/standalone/server.js）。
  // 仅在 BUILD_STANDALONE=1 时启用，避免本地 `next start` 流程受影响。
  output: process.env.BUILD_STANDALONE === '1' ? 'standalone' : undefined,
};

export default nextConfig;
