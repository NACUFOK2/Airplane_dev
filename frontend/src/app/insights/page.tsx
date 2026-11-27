"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { Count, Insights } from "@/types/chat";

function CountList({ title, desc, items }: { title: string; desc: string; items: Count[] }) {
  return (
    <section className="card insights__card">
      <p className="insights__title">{title}</p>
      <p className="insights__desc">{desc}</p>
      {items.length === 0 ? (
        <p className="insights__empty">아직 기록이 없어요</p>
      ) : (
        <ol className="insights__list">
          {items.map((c) => (
            <li key={c.key}>
              <span>{c.key}</span>
              <span className="insights__count">{c.count}회</span>
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}

export default function InsightsPage() {
  const [data, setData] = useState<Insights | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.insights().then(setData).catch((e: Error) => setError(e.message));
  }, []);

  const rate = data && data.feedbackCount > 0 ? Math.round((data.upCount / data.feedbackCount) * 100) : null;

  return (
    <main className="app">
      <header className="header">
        <div className="header__top">
          <span className="header__title">답변 품질 리포트</span>
          <Link className="text-btn" href="/">채팅으로</Link>
        </div>
      </header>
      <div className="content insights">
        {error && <div className="toast">{error}</div>}
        {data && (
          <>
            <div className="stats">
              <div className="stat"><p className="stat__label">만족도</p><p className="stat__value">{rate === null ? "–" : `${rate}%`}</p></div>
              <div className="stat"><p className="stat__label">👍 / 👎</p><p className="stat__value">{data.upCount} / {data.downCount}</p></div>
              <div className="stat"><p className="stat__label">범위 밖 질문</p><p className="stat__value">{data.outOfScopeCount}</p></div>
            </div>
            <CountList title="답하지 못한 질문" desc="자료가 없어 답을 못 했어요. 문답 데이터에 추가할 후보예요." items={data.topOutOfScopeQuestions} />
            <CountList title="👎 받은 질문" desc="검색은 됐지만 답변이 부족했던 질문이에요." items={data.topDownvotedQuestions} />
            <CountList title="👎 답변의 1순위 근거 문답" desc="이 번호의 문답 내용을 먼저 점검해 보세요." items={data.topDownvotedSourceIds} />
            <p className="notice">🔒 질문은 개인정보를 가린 뒤 이 PC 의 chat-logs/ 에만 저장돼요.</p>
          </>
        )}
      </div>
    </main>
  );
}
