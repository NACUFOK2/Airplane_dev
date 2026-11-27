import type { NextConfig } from "next";

// 로컬 전용: 브라우저 → Next(3000) → Spring Boot(8080) 프록시 (CORS 불필요)
const API_BASE = process.env.API_BASE_URL ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${API_BASE}/api/:path*` }];
  },
};

export default nextConfig;
