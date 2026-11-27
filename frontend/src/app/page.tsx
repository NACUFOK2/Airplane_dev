"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import ChatInput from "@/components/ChatInput";
import MessageBubble from "@/components/MessageBubble";
import OrgSelector from "@/components/OrgSelector";
import TypingIndicator from "@/components/TypingIndicator";
import Welcome from "@/components/Welcome";
import { useChat } from "@/hooks/useChat";
import { api } from "@/lib/api";
import type { Organization } from "@/types/chat";

const DEFAULT_SUGGESTIONS = [
  "항공권 취소하면 수수료 얼마나 나와요?",
  "보조배터리 몇 개까지 들고 탈 수 있어요?",
  "아기랑 타면 모바일 탑승권 쓸 수 있어요?",
  "패키지여행 출발 일주일 전에 취소하면 위약금은?",
];

export default function Home() {
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [selected, setSelected] = useState("ALL");
  const [loadError, setLoadError] = useState<string | null>(null);
  const { messages, loading, send, reset } = useChat(selected);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    api.organizations().then(setOrganizations).catch((e: Error) => setLoadError(e.message));
  }, []);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, loading]);

  const current = organizations.find((o) => o.code === selected);
  const suggestions = useMemo(
    () => (current?.suggestedQuestions?.length ? current.suggestedQuestions : DEFAULT_SUGGESTIONS),
    [current],
  );

  const onSelect = (code: string) => {
    if (code === selected) return;
    setSelected(code);
    reset();
  };

  return (
    <main className="app">
      <header className="header">
        <div className="header__top">
          <span className="header__title">여행 QnA</span>
          {messages.length > 0 && <button className="text-btn" onClick={reset}>새 대화</button>}
        </div>
        <OrgSelector organizations={organizations} selected={selected} onSelect={onSelect} />
      </header>

      <div className="content">
        {loadError && <div className="toast">{loadError}</div>}
        {messages.length === 0 ? (
          <Welcome
            title={current ? `${current.name}에 대해` : "여행사·항공사 규정,"}
            description={
              current
                ? `${current.name} Q&A ${current.qaCount}건에서 찾아 답해드릴게요.`
                : "여행사 4곳·항공사 3곳의 Q&A에서 찾아 답해드릴게요."
            }
            suggestions={suggestions}
            onPick={send}
          />
        ) : (
          <div className="messages">
            {messages.map((m) => <MessageBubble key={m.id} message={m} />)}
            {loading && <TypingIndicator />}
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      <footer className="footer">
        <ChatInput
          disabled={loading}
          placeholder={current ? `${current.name}에 대해 물어보세요` : "예약·환불·수하물 무엇이든 물어보세요"}
          onSend={send}
        />
      </footer>
    </main>
  );
}
