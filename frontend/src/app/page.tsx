"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import AirlineSelector from "@/components/AirlineSelector";
import ChatInput from "@/components/ChatInput";
import MessageBubble from "@/components/MessageBubble";
import TypingIndicator from "@/components/TypingIndicator";
import Welcome from "@/components/Welcome";
import { useChat } from "@/hooks/useChat";
import { api } from "@/lib/api";
import type { Airline } from "@/types/chat";

const DEFAULT_SUGGESTIONS = [
  "위탁 수하물 몇 kg까지 무료예요?",
  "기내에 들고 탈 수 있는 가방 크기는?",
  "반려동물이랑 같이 탈 수 있어요?",
  "결항되면 환불받을 수 있나요?",
];

export default function Home() {
  const [airlines, setAirlines] = useState<Airline[]>([]);
  const [selected, setSelected] = useState("ALL");
  const [loadError, setLoadError] = useState<string | null>(null);
  const { messages, loading, send, reset } = useChat(selected);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    api.airlines().then(setAirlines).catch((e: Error) => setLoadError(e.message));
  }, []);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, loading]);

  const current = airlines.find((a) => a.code === selected);
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
          <span className="header__title">항공 QnA</span>
          {messages.length > 0 && (
            <button className="text-btn" onClick={reset}>새 대화</button>
          )}
        </div>
        <AirlineSelector airlines={airlines} selected={selected} onSelect={onSelect} />
      </header>

      <div className="content">
        {loadError && <div className="toast">{loadError}</div>}
        {messages.length === 0 ? (
          <Welcome title={current ? `${current.name}에 대해` : "항공사 규정,"} suggestions={suggestions} onPick={send} />
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
          placeholder={current ? `${current.name}에 대해 물어보세요` : "궁금한 항공 규정을 물어보세요"}
          onSend={send}
        />
      </footer>
    </main>
  );
}
