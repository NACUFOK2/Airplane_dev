import type { ChatMessage, ChatResponse, Insights, Organization } from "@/types/chat";

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, {
    ...init,
    headers: { "Content-Type": "application/json", ...(init?.headers ?? {}) },
  });
  if (res.status === 204) return undefined as T;
  if (!res.ok) {
    const body = await res.json().catch(() => null);
    throw new Error(body?.message ?? "서버와 연결할 수 없어요. 백엔드가 실행 중인지 확인해 주세요.");
  }
  return res.json() as Promise<T>;
}

export const api = {
  organizations: () => request<Organization[]>("/api/organizations"),
  ask: (question: string, orgCode: string, history: ChatMessage[]) =>
    request<ChatResponse>("/api/chat", {
      method: "POST",
      body: JSON.stringify({
        question,
        orgCode,
        history: history
          .filter((m) => !m.error && m.mode !== "out_of_scope")
          .slice(-6)
          .map((m) => ({ role: m.role, content: m.content })),
      }),
    }),
  feedback: (m: ChatMessage, rating: "up" | "down", orgCode: string) =>
    request<void>("/api/feedback", {
      method: "POST",
      body: JSON.stringify({
        rating,
        question: m.question ?? "",
        orgCode,
        mode: m.mode,
        sourceIds: (m.sources ?? []).map((s) => s.id),
      }),
    }),
  insights: () => request<Insights>("/api/insights?limit=10"),
};
