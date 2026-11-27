"use client";

import { useState } from "react";
import type { Source } from "@/types/chat";

export default function SourceList({ sources }: { sources: Source[] }) {
  const [open, setOpen] = useState(false);
  if (!sources.length) return null;
  return (
    <div className="sources">
      <button className="sources__toggle" onClick={() => setOpen((v) => !v)} aria-expanded={open}>
        참고한 규정 {sources.length}개
        <span className={`chevron ${open ? "chevron--up" : ""}`}>›</span>
      </button>
      {open && (
        <ul className="sources__list">
          {sources.map((s) => (
            <li key={`${s.airlineCode}-${s.index}`} className="source">
              <div className="source__head">
                <span className="source__num">{s.index}</span>
                <span className="source__airline">{s.airlineName}</span>
                <span className="source__tag">{s.category}</span>
              </div>
              <p className="source__title">{s.title}</p>
              <p className="source__snippet">{s.snippet}</p>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
