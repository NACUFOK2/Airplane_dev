import type { Airline, ChatMessage, ChatResponse } from "@/types/chat";

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, {
    ...init,
    headers: { "Content-Type": "application/json", ...(init?.headers ?? {}) },
  });
  if (!res.ok) {
    const body = await res.json().catch(() => null);
    throw new Error(body?.message ?? "서버와 연결할 수 없어요. 백엔드가 실행 중인지 확인해 주세요.");
  }
  return res.json() as Promise<T>;
}

export const api = {
  airlines: () => request<Airline[]>("/api/airlines"),
  ask: (question: string, airlineCode: string, history: ChatMessage[]) =>
    request<ChatResponse>("/api/chat", {
      method: "POST",
      body: JSON.stringify({
        question,
        airlineCode,
        history: history
          .filter((m) => !m.error)
          .slice(-6)
          .map((m) => ({ role: m.role, content: m.content })),
      }),
    }),
};
