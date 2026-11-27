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
  "델타항공 예약하고 24시간 안에 취소하면 전액 환불돼요?",
  "부킹닷컴 환불 불가 예약도 위약금 면제받을 수 있어요?",
  "보조배터리 몇 개까지 들고 탈 수 있어요?",
  "패키지여행 출발 일주일 전에 취소하면 위약금은?",
];

const TYPE_ORDER = ["항공사", "여행사", "숙박업체"];

function scopeDescription(orgs: Organization[]): string {
  if (!orgs.length) return "항공사·여행사·숙박업체 문답 자료에서 찾아 답해드릴게요.";
  const counts = TYPE_ORDER.map((t) => [t, orgs.filter((o) => o.type === t).length] as const).filter(([, n]) => n > 0);
  const total = orgs.reduce((sum, o) => sum + o.qaCount, 0);
  return `${counts.map(([t, n]) => `${t} ${n}곳`).join(" · ")}의 문답 ${total}건에서 찾아 답해드릴게요.`;
}

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
          <span className="header__title">여행 상담</span>
          {messages.length > 0 && <button className="text-btn" onClick={reset}>새 대화</button>}
        </div>
        <OrgSelector organizations={organizations} selected={selected} onSelect={onSelect} />
      </header>

      <div className="content">
        {loadError && <div className="toast">{loadError}</div>}
        {messages.length === 0 ? (
          <Welcome
            title={current ? `${current.name}에 대해` : "항공·여행·숙박 규정,"}
            description={
              current
                ? `${current.name} 문답 ${current.qaCount}건에서 찾아 답해드릴게요.`
                : scopeDescription(organizations)
            }
            suggestions={suggestions}
            onPick={send}
          />
        ) : (
          <div className="messages">
            {messages.map((m) => <MessageBubble key={m.id} message={m} orgCode={selected} />)}
            {loading && <TypingIndicator />}
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      <footer className="footer">
        <ChatInput
          disabled={loading}
          placeholder={current ? `${current.name}에 대해 물어보세요` : "예약·환불·수하물·숙소 무엇이든 물어보세요"}
          onSend={send}
        />
      </footer>
    </main>
  );
}
