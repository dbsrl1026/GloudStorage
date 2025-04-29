import React from "react";

interface ConfirmModal {
    isOpen: boolean;
    onClose: () => void;
    onConfirm: () => void;
    message: string;
}

const ConfirmModal: React.FC<ConfirmModal> = ({ isOpen, onClose, onConfirm, message }) => {
    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-black bg-opacity-30 flex items-center justify-center z-50">
            <div className="bg-white rounded-lg p-6 w-80 shadow-lg">
                <h2 className="text-xl font-bold mb-4">확인</h2>
                <p className="mb-6">{message}</p>
                <div className="flex justify-end gap-4">
                    <button onClick={onClose} className="text-blue-600 hover:underline">
                        취소
                    </button>
                    <button onClick={onConfirm} className="text-blue-600 hover:underline font-semibold">
                        확인
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ConfirmModal;