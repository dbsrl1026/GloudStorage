import React, { useEffect, useRef, useState } from "react";

interface CreateFolderModalProps {
    isOpen: boolean;
    onClose: () => void;
    onCreate: (folderName: string) => Promise<void>;
}

const CreateFolderModal: React.FC<CreateFolderModalProps> = ({ isOpen, onClose, onCreate }) => {
    const [folderName, setFolderName] = useState("제목없는 폴더");
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const inputRef = useRef<HTMLInputElement>(null);

    useEffect(() => {
        if (isOpen) {
            setFolderName("제목없는 폴더");
            setTimeout(() => inputRef.current?.select(), 0); // 포커싱 + 전체 선택
        }
    }, [isOpen]);

    const handleSubmit = async () => {
        if (!folderName.trim()) return;

        try {
            await onCreate(folderName);
            setErrorMessage(null);
        } catch (error: any) {
            const message = error?.message || "폴더 생성 중 오류가 발생했습니다.";
            setErrorMessage(message);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-black bg-opacity-30 flex items-center justify-center z-50">
            <div className="bg-white rounded-lg p-6 w-80 shadow-lg">
                <h2 className="text-xl font-bold mb-4">새 폴더</h2>
                <input
                    ref={inputRef}
                    value={folderName}
                    onChange={(e) => setFolderName(e.target.value)}
                    onKeyDown={(e) => {
                        if (e.key === "Enter") {
                            handleSubmit();
                        }
                    }}
                    className="w-full border rounded px-3 py-2 focus:outline-none focus:ring focus:border-blue-500"
                />
                {errorMessage && (
                    <p className="text-red-500 text-sm mb-2">{errorMessage}</p>
                )}
                <div className="flex justify-end gap-4 mt-6">
                    <button onClick={onClose} className="text-blue-600 hover:underline">
                        취소
                    </button>
                    <button onClick={handleSubmit} className="text-blue-600 hover:underline font-semibold">
                        만들기
                    </button>
                </div>
            </div>
        </div>
    );
};

export default CreateFolderModal;