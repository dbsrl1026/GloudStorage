import { useState } from "react";

interface RenameFolderModalProps {
    isOpen: boolean;
    onClose: () => void;
    onRename: (newName: string) => Promise<void>;
}

const RenameFolderModal = ({ isOpen, onClose, onRename }: RenameFolderModalProps) => {
    const [folderName, setFolderName] = useState("");
    const [error, setError] = useState("");

    if (!isOpen) return null;

    const handleRename = async () => {
        if (!folderName.trim()) {
            setError("새 이름을 입력하세요.");
            return;
        }
        try {
            await onRename(folderName.trim());
            setFolderName("");
            setError("");
        } catch (err: any) {
            setError(err.message || "이름 변경 중 오류가 발생했습니다.");
        }
    };

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white p-6 rounded shadow-lg w-96">
                <h2 className="text-lg font-bold mb-4">폴더 이름 변경</h2>
                <input
                    type="text"
                    value={folderName}
                    onChange={(e) => setFolderName(e.target.value)}
                    placeholder="새 폴더 이름"
                    className="w-full border px-3 py-2 rounded mb-2"
                />
                {error && <p className="text-red-500 text-sm mb-2">{error}</p>}
                <div className="flex justify-end gap-2">
                    <button
                        className="border px-4 py-2 rounded"
                        onClick={() => {
                            setFolderName("");
                            setError("");
                            onClose();
                        }}
                    >
                        취소
                    </button>
                    <button
                        className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded"
                        onClick={handleRename}
                    >
                        이름 변경
                    </button>
                </div>
            </div>
        </div>
    );
};

export default RenameFolderModal;