import type { ChatMessage } from "@/types/chat";
import RichText from "./RichText";
import SourceList from "./SourceList";

export default function MessageBubble({ message }: { message: ChatMessage }) {
  if (message.role === "user") {
    return (
      <div className="row row--user">
        <div className="bubble bubble--user">{message.content}</div>
      </div>
    );
  }
  return (
    <div className="row row--bot">
      <div className="avatar" aria-hidden>✈️</div>
      <div className="bot-col">
        <div className={`bubble bubble--bot ${message.error ? "bubble--error" : ""}`}>
          <RichText text={message.content} />
        </div>
        {message.sources && <SourceList sources={message.sources} />}
        {message.retrieval === "bm25" && (
          <span className="mode-badge">임베딩 서버 미연결 · 키워드 검색 결과예요</span>
        )}
      </div>
    </div>
  );
}
