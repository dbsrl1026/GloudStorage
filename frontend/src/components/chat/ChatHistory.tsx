import React from "react";

interface ChatItem {
    question: string;
    answer: string;
}

interface ChatHistoryProps {
    chatHistory: ChatItem[];
}

const ChatHistory = ({chatHistory}: ChatHistoryProps) => {
    if (!chatHistory || chatHistory.length === 0) {
        return (
            <div className="flex flex-col gap-2">
                <p className="text-gray-400 text-sm text-center mt-4 mb-0">챗봇을 통해 스마트한 문서 조회를 경험하세요!</p>
                <p className="text-gray-400 text-sm text-center mb-0">예) "3분기 실적 관련 문서 찾아줘",</p>
                <p className="text-gray-400 text-sm text-center">또는 "이번분기 영업이익이 얼마나 되지?"</p>


            </div>
        );
    }

    return (
        <div className="flex flex-col gap-4 text-sm">
            {chatHistory.map((chat, index) => (
                <div key={index}
                     className="flex flex-col gap-2 transition-all duration-500 ease-out opacity-0 animate-fadeIn">
                    <div className="self-end bg-blue-500 text-white p-2 rounded max-w-xs">
                        {chat.question}
                    </div>
                    <div className="self-start bg-gray-200 p-2 rounded max-w-xs whitespace-pre-wrap">
                        {chat.answer}
                    </div>
                </div>
            ))}
        </div>
    );
};

export default ChatHistory;