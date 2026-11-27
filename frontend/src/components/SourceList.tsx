"use client";

import { useState } from "react";
import type { Source } from "@/types/chat";

export default function SourceList({ sources }: { sources: Source[] }) {
  const [open, setOpen] = useState(false);
  if (!sources.length) return null;
  return (
    <div className="sources">
      <button className="sources__toggle" onClick={() => setOpen((v) => !v)} aria-expanded={open}>
        참고한 Q&A {sources.length}개
        <span className={`chevron ${open ? "chevron--up" : ""}`}>›</span>
      </button>
      {open && (
        <ul className="sources__list">
          {sources.map((s) => (
            <li key={s.id} className="source">
              <div className="source__head">
                <span className="source__num">{s.index}</span>
                <span className="source__airline">{s.org}</span>
                <span className="source__tag">{s.orgType}</span>
                {s.score > 0 && <span className="source__score">유사도 {s.score.toFixed(2)}</span>}
              </div>
              <p className="source__title">{s.question}</p>
              <p className="source__snippet">{s.snippet}</p>
              {s.url && (
                <a className="source__link" href={s.url} target="_blank" rel="noopener noreferrer">
                  원문 보기 ↗
                </a>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
