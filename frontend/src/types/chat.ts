export interface Organization {
  code: string;
  name: string;
  type: "항공사" | "여행사" | "숙박업체";
  brandColor: string;
  qaCount: number;
  suggestedQuestions: string[];
}

export interface Source {
  index: number;
  id: string;
  org: string;
  orgType: string;
  question: string;
  snippet: string;
  url: string;
  official: boolean;
  score: number;
}

export interface ChatResponse {
  answer: string;
  mode: "claude" | "extractive" | "out_of_scope";
  retrieval: "hybrid" | "bm25";
  sources: Source[];
}

export interface ChatMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
  sources?: Source[];
  mode?: ChatResponse["mode"];
  retrieval?: ChatResponse["retrieval"];
  /** 이 답변을 만든 사용자 질문 (피드백 전송용) */
  question?: string;
  error?: boolean;
}

export interface Count {
  key: string;
  count: number;
}

export interface Insights {
  feedbackCount: number;
  upCount: number;
  downCount: number;
  outOfScopeCount: number;
  topOutOfScopeQuestions: Count[];
  topDownvotedQuestions: Count[];
  topDownvotedSourceIds: Count[];
}
