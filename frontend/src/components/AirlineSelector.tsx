"use client";

import type { Airline } from "@/types/chat";

interface Props {
  airlines: Airline[];
  selected: string;
  onSelect: (code: string) => void;
}

export default function AirlineSelector({ airlines, selected, onSelect }: Props) {
  const items = [{ code: "ALL", name: "전체", brandColor: "#3182F6" }, ...airlines];
  return (
    <div className="chips" role="tablist" aria-label="항공사 선택">
      {items.map((a) => (
        <button
          key={a.code}
          role="tab"
          aria-selected={selected === a.code}
          className={`chip ${selected === a.code ? "chip--active" : ""}`}
          onClick={() => onSelect(a.code)}
        >
          {a.code !== "ALL" && <span className="chip__dot" style={{ background: a.brandColor }} />}
          {a.name}
        </button>
      ))}
    </div>
  );
}
