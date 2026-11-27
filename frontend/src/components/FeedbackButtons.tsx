"use client";

import { useState } from "react";
import { api } from "@/lib/api";
import type { ChatMessage } from "@/types/chat";

export default function FeedbackButtons({ message, orgCode }: { message: ChatMessage; orgCode: string }) {
  const [sent, setSent] = useState<"up" | "down" | null>(null);
  const [failed, setFailed] = useState(false);

  const send = async (rating: "up" | "down") => {
    if (sent) return;
    setSent(rating);
    try {
      await api.feedback(message, rating, orgCode);
    } catch {
      setSent(null);
      setFailed(true);
    }
  };

  if (sent) return <span className="feedback__thanks">의견 고마워요{sent === "down" ? ". 더 나은 답변을 준비할게요" : ""}</span>;
  return (
    <div className="feedback" aria-label="답변 평가">
      <span className="feedback__label">{failed ? "전송 실패, 다시 눌러주세요" : "답변이 도움이 됐나요?"}</span>
      <button className="feedback__btn" onClick={() => send("up")} aria-label="도움이 됐어요">👍</button>
      <button className="feedback__btn" onClick={() => send("down")} aria-label="도움이 안 됐어요">👎</button>
    </div>
  );
}
