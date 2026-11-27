import type { NextConfig } from "next";
import { PHASE_DEVELOPMENT_SERVER } from "next/constants";

// 로컬 전용: 브라우저 → Next(3000) → Spring Boot(8080) 프록시 (CORS 불필요)
const API_BASE = process.env.API_BASE_URL ?? "http://localhost:8080";

export default function config(phase: string): NextConfig {
  return {
    // 개발 서버(.next-dev)와 빌드(.next) 출력 폴더를 분리:
    // 개발 서버 실행 중 `npm run build` 를 해도 서로의 캐시를 덮어써 런타임 에러가 나지 않게 한다.
    distDir: phase === PHASE_DEVELOPMENT_SERVER ? ".next-dev" : ".next",
    // 화면 좌측 하단의 영어 개발 표시기(N 버튼) 숨김
    devIndicators: false,
    async rewrites() {
      return [{ source: "/api/:path*", destination: `${API_BASE}/api/:path*` }];
    },
  };
}
