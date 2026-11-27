"use client";

import { useCallback, useState } from "react";
import { api } from "@/lib/api";
import type { ChatMessage } from "@/types/chat";

const uid = () => Math.random().toString(36).slice(2, 10);

export function useChat(orgCode: string) {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [loading, setLoading] = useState(false);

  const send = useCallback(
    async (question: string) => {
      const q = question.trim();
      if (!q || loading) return;
      const history = messages;
      setMessages((prev) => [...prev, { id: uid(), role: "user", content: q }]);
      setLoading(true);
      try {
        const res = await api.ask(q, orgCode, history);
        setMessages((prev) => [
          ...prev,
          { id: uid(), role: "assistant", content: res.answer, sources: res.sources, mode: res.mode, retrieval: res.retrieval, question: q },
        ]);
      } catch (e) {
        setMessages((prev) => [
          ...prev,
          { id: uid(), role: "assistant", content: (e as Error).message, error: true },
        ]);
      } finally {
        setLoading(false);
      }
    },
    [orgCode, loading, messages],
  );

  const reset = useCallback(() => setMessages([]), []);

  return { messages, loading, send, reset };
}
