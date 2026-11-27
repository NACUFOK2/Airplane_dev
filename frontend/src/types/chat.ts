export interface Organization {
  code: string;
  name: string;
  type: "여행사" | "항공사";
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
  error?: boolean;
}
