import React, { useState } from "react";
import axios from "../../services/axios";
import { useAuthStore } from "../../stores/authStore";

interface ChatItem {
  question: string;
  answer: string;
}

interface ChatInputProps {
  chatHistory: ChatItem[];
  setChatHistory: React.Dispatch<React.SetStateAction<ChatItem[]>>;
  setSearchedDocumentIds: React.Dispatch<React.SetStateAction<number[] | null>>;
}

const ChatInput = ({ chatHistory, setChatHistory, setSearchedDocumentIds }: ChatInputProps) => {
  const [inputValue, setInputValue] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const email = useAuthStore((state) => state.email);

  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setInputValue(e.target.value);
    e.target.style.height = "auto";
    e.target.style.height = `${e.target.scrollHeight}px`;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputValue.trim() || !email) return;

    const query = inputValue.trim();
    setInputValue("");

    // 1. 사용자의 질문을 즉시 추가
    setChatHistory((prev) => [
      ...prev,
      { question: query, answer: "" }, // 답변 없이 질문만 추가
    ]);

    // 0.5초 후 "답변 생성중..." 메시지로 교체
    setTimeout(() => {
      // @ts-ignore
      setChatHistory((prev) => [
        ...prev.slice(0, -1), // 마지막 질문을 가져와서
        {
          question: query,
          answer: (
            <div className="flex items-center gap-2">
              <span>🤖 답변을 생성하는 중입니다...</span>
              <div className="w-4 h-4 border-2 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
            </div>
          ),
        },
      ]);
    }, 500);
    setIsLoading(true);

    try {
      const response = await axios.post("/ai/chat", {
        query,
        email,
        history: chatHistory.map((chat) => [chat.question, chat.answer]),
      });

      const { answer, documents } = response.data;

      // 마지막 "답변 생성중"을 진짜 답변으로 교체
      setChatHistory((prev) => {
        const newHistory = [...prev];
        newHistory[newHistory.length - 1] = { question: query, answer };
        return newHistory;
      });
      console.log(response);
      setSearchedDocumentIds(documents.map((doc: any) => doc.document_id));


    } catch (error: any) {
      if (error.code === "ECONNABORTED") {
        console.error("요청 시간이 초과되었습니다.");
        alert("응답 시간이 너무 오래 걸렸습니다. 나중에 다시 시도해주세요.");
      } else if (error.response) {
        console.error("서버 응답 오류:", error.response.data);
        alert(error.response.data.message || "서버 오류가 발생했습니다.");
      } else {
        console.error("알 수 없는 오류:", error);
        alert("서버 오류가 발생했습니다. 다시 시도해주세요.");
      }
      // 에러 발생 시 "답변 생성 중" 메시지도 삭제
      setChatHistory((prev) => prev.slice(0, -1));
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSubmit(e as any);
    }
  };

  return (
      <form onSubmit={handleSubmit} className="flex gap-2">
        <textarea
            value={inputValue}
            onChange={handleChange}
            onKeyDown={handleKeyDown}
            placeholder="질문을 입력하세요..."
            className="flex-1 p-2 border rounded resize-none overflow-hidden max-h-48 text-sm"
            rows={1}
            disabled={isLoading}
        />
        <button
            type="submit"
            className="p-2 rounded bg-blue-600 hover:bg-blue-700"
        >
          <img src="/src/assets/icons/enter.png" alt="Send" className="w-5 h-5" />
        </button>
      </form>
  );
};

export default ChatInput;