"use client";

import type { Organization } from "@/types/chat";

interface Props {
  organizations: Organization[];
  selected: string;
  onSelect: (code: string) => void;
}

const TYPES = ["전체", "항공사", "여행사", "숙박업체"] as const;

export default function OrgSelector({ organizations, selected, onSelect }: Props) {
  const selectedOrg = organizations.find((o) => o.code === selected);
  const activeType = selectedOrg?.type ?? "전체";
  const visible = organizations.filter((o) => activeType === "전체" || o.type === activeType);

  return (
    <div className="selector">
      <div className="segment" role="tablist" aria-label="기관 유형">
        {TYPES.map((t) => (
          <button
            key={t}
            role="tab"
            aria-selected={activeType === t}
            className={`segment__item ${activeType === t ? "segment__item--active" : ""}`}
            onClick={() => {
              if (t === "전체") onSelect("ALL");
              else if (activeType !== t) onSelect(organizations.find((o) => o.type === t)?.code ?? "ALL");
            }}
          >
            {t}
          </button>
        ))}
      </div>
      <div className="chips" role="tablist" aria-label="기관 선택">
        {activeType === "전체" && (
          <button className="chip chip--active" aria-selected>모든 기관</button>
        )}
        {visible.map((o) => (
          <button
            key={o.code}
            role="tab"
            aria-selected={selected === o.code}
            className={`chip ${selected === o.code ? "chip--active" : ""}`}
            onClick={() => onSelect(o.code)}
          >
            <span className="chip__dot" style={{ background: o.brandColor }} />
            {o.name}
          </button>
        ))}
      </div>
    </div>
  );
}
