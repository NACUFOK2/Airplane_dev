export interface Airline {
  code: string;
  name: string;
  nameEn: string;
  brandColor: string;
  customerCenter: string;
  suggestedQuestions: string[];
}

export interface Source {
  index: number;
  airlineCode: string;
  airlineName: string;
  category: string;
  title: string;
  snippet: string;
  score: number;
}

export interface ChatResponse {
  answer: string;
  mode: "claude" | "extractive";
  sources: Source[];
}

export interface ChatMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
  sources?: Source[];
  mode?: ChatResponse["mode"];
  error?: boolean;
}
