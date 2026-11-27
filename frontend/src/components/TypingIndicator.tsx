export default function TypingIndicator() {
  return (
    <div className="row row--bot">
      <div className="avatar" aria-hidden>✈️</div>
      <div className="bubble bubble--bot typing" aria-label="답변 작성 중">
        <span /><span /><span />
      </div>
    </div>
  );
}
