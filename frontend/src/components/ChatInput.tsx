"use client";

import { useState, type FormEvent, type KeyboardEvent } from "react";

interface Props {
  disabled: boolean;
  placeholder: string;
  onSend: (text: string) => void;
}

export default function ChatInput({ disabled, placeholder, onSend }: Props) {
  const [value, setValue] = useState("");

  const submit = (e?: FormEvent) => {
    e?.preventDefault();
    if (!value.trim() || disabled) return;
    onSend(value);
    setValue("");
  };

  const onKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey && !e.nativeEvent.isComposing) {
      e.preventDefault();
      submit();
    }
  };

  return (
    <form className="composer" onSubmit={submit}>
      <textarea
        className="composer__input"
        rows={1}
        value={value}
        maxLength={1000}
        placeholder={placeholder}
        onChange={(e) => setValue(e.target.value)}
        onKeyDown={onKeyDown}
        aria-label="질문 입력"
      />
      <button className="composer__send" type="submit" disabled={disabled || !value.trim()} aria-label="보내기">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden>
          <path d="M12 19V5M5 12l7-7 7 7" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </button>
    </form>
  );
}
