interface Props {
  title: string;
  description: string;
  suggestions: string[];
  onPick: (q: string) => void;
}

export default function Welcome({ title, description, suggestions, onPick }: Props) {
  return (
    <section className="welcome">
      <div className="welcome__emoji" aria-hidden>🛫</div>
      <h1 className="welcome__title">
        {title}
        <br />
        무엇이 궁금하세요?
      </h1>
      <p className="welcome__desc">{description}</p>
      <div className="card">
        <p className="card__label">이런 걸 많이 물어봐요</p>
        <ul className="list">
          {suggestions.map((s) => (
            <li key={s}>
              <button className="list__item" onClick={() => onPick(s)}>
                <span>{s}</span>
                <span className="list__chevron" aria-hidden>›</span>
              </button>
            </li>
          ))}
        </ul>
      </div>
      <p className="notice">🔒 예약번호·여권번호·카드번호 같은 개인정보는 입력하지 마세요.</p>
    </section>
  );
}
