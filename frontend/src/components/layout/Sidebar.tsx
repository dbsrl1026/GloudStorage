// Sidebar.tsx
import ChatHistory from "../chat/ChatHistory";
import ChatInput from "../chat/ChatInput";
import {useState} from "react";

interface SidebarProps {
    setSearchedDocumentIds: React.Dispatch<React.SetStateAction<number[] | null>>;
}

const Sidebar = ({setSearchedDocumentIds}: SidebarProps) => {
    const [chatHistory, setChatHistory] = useState<{ question: string; answer: string }[]>([]);

    return (
        <aside className="w-88 flex-shrink-0 h-full bg-gray-50 border-r flex flex-col">
            <div className="flex-1 overflow-y-auto p-3">
                <ChatHistory chatHistory={chatHistory}/>
            </div>
            <div className="p-4 border-t">
                <ChatInput
                    chatHistory={chatHistory}
                    setChatHistory={setChatHistory}
                    setSearchedDocumentIds={setSearchedDocumentIds}
                />
            </div>
        </aside>
    );
};

export default Sidebar;