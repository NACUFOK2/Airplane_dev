import { Fragment, type ReactNode } from "react";

/** 안전한 경량 마크다운 렌더러: **굵게**, _기울임_, - 목록, [n] 인용 배지만 지원 (HTML 주입 없음) */
function inline(text: string, key: string): ReactNode[] {
  const parts = text.split(/(\*\*[^*]+\*\*|\[\d+\]|_[^_]+_)/g);
  return parts.map((p, i) => {
    const k = `${key}-${i}`;
    if (/^\*\*[^*]+\*\*$/.test(p)) return <strong key={k}>{p.slice(2, -2)}</strong>;
    if (/^\[\d+\]$/.test(p)) return <sup key={k} className="cite">{p.slice(1, -1)}</sup>;
    if (/^_[^_]+_$/.test(p)) return <em key={k} className="muted">{p.slice(1, -1)}</em>;
    return <Fragment key={k}>{p}</Fragment>;
  });
}

export default function RichText({ text }: { text: string }) {
  const lines = text.split("\n");
  const blocks: ReactNode[] = [];
  let list: string[] = [];
  const flush = (i: number) => {
    if (list.length) {
      blocks.push(
        <ul key={`ul-${i}`}>{list.map((l, j) => <li key={j}>{inline(l, `li-${i}-${j}`)}</li>)}</ul>,
      );
      list = [];
    }
  };
  lines.forEach((line, i) => {
    const m = line.match(/^\s*[-•*]\s+(.*)$/);
    if (m) {
      list.push(m[1]);
      return;
    }
    flush(i);
    if (line.trim() === "") return;
    const h = line.match(/^#{1,4}\s+(.*)$/);
    blocks.push(h ? <p key={i} className="rt-heading">{inline(h[1], `h-${i}`)}</p> : <p key={i}>{inline(line, `p-${i}`)}</p>);
  });
  flush(lines.length);
  return <div className="rich-text">{blocks}</div>;
}
