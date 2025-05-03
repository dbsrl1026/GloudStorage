import React from "react";
import { useAuthStore } from "../../stores/authStore";
import axios from "../../services/axios";
import ConfirmModal from "./ConfirmModal";

interface DocumentDetailDto {
  metadataId: number;
  originalFilename: string;
  contentType: string;
  uploadTime: string;
  fileSize: number;
  filePath: string;
  previewPath: string;
  directoryId: number;
}

interface FileDetailsPanelProps {
  document: DocumentDetailDto | null;
  onClose: () => void;
  refreshContent: () => void;
  currentDirectoryId: number | null;
  path: DirectoryDto[]; // ✅ fullPath 추가
  onPathClick: (directoryId: number | null) => void; // ✅ 경로 클릭시 이동 함수 추가
}

const formatFileSize = (sizeInKB: number): string => {
  if (sizeInKB >= 1024) {
    return `${(sizeInKB / 1024).toFixed(1)} MB`;
  }
  return `${sizeInKB.toFixed(1)} KB`;
};

const FileDetailsPanel: React.FC<FileDetailsPanelProps> = ({
  document: file,
  onClose,
  refreshContent,
  currentDirectoryId,
  path,
  onPathClick,
}) => {
  const [isPreviewOpen, setIsPreviewOpen] = React.useState(false);
  const [isConfirmModalOpen, setIsConfirmModalOpen] = React.useState(false);
  const [isMoveMode, setIsMoveMode] = React.useState(false);
  const { user } = useAuthStore();

  const handleDownload = async () => {
    if (!file || !user?.email) return;
    try {
      const response = await axios.get(`/documents/${file.metadataId}/download`, {
        params: { email: user.email },
        responseType: "blob",
      });

      const blob = new Blob([response.data]);
      const downloadUrl = window.URL.createObjectURL(blob);

      const a = document.createElement("a");
      a.href = downloadUrl;
      a.download = file.originalFilename;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(downloadUrl);
    } catch (error) {
      console.error("파일 다운로드 실패", error);
    }
  };

  const handleDelete = async () => {
    if (!file || !user?.email) return;
    try {
      await axios.delete(`/documents/${file.metadataId}`, {
        params: { email: user.email },
      });
      setIsConfirmModalOpen(false);
      onClose();
      refreshContent();
    } catch (error) {
      console.error("파일 삭제 실패", error);
    }
  };

  // Move file to current directory
  const handleMoveHere = async () => {
    if (!file || !user?.email) return;
    try {
      // Move to current directory (using FileExplorer's currentDirectoryId)
      const targetDirectoryId = currentDirectoryId;
      await axios.patch(
        `/documents/${file.metadataId}/move`,
        null,
        {
          params: {
            email: user.email,
            targetDirectoryId: targetDirectoryId !== null ? targetDirectoryId : "",
          },
        }
      );
      setIsMoveMode(false);
      onClose();
      refreshContent();
    } catch (error: any) {
      console.error("파일 이동 실패", error);
      const errorMessage = error?.response?.data?.message || "파일 이동 중 오류가 발생했습니다.";
      alert(errorMessage);
      setIsMoveMode(false);
    }
  };

  if (!file) return null;

  return (
    <aside
      className={`transition-all duration-300 overflow-y-auto bg-gray-100 border-l h-full w-64 p-4 flex flex-col`}
    >
      <div className="flex justify-start mb-4">
        <button
          onClick={onClose}
          className="text-gray-400 hover:text-gray-600 flex items-center gap-1"
          aria-label="닫기"
        >
          <img src="/src/assets/icons/right-arrow.png" alt="닫기" className="w-5 h-5" />
        </button>
      </div>

      {/* File view */}
      <div className="flex items-center gap-2 mb-10">
        <img
          src={`/src/assets/icons/${file.contentType.toLowerCase()}.png`}
          alt={file.contentType}
          className="w-7 h-7"
        />
        <h2 className="text-gl font-bold truncate">{file.originalFilename}</h2>
      </div>

      <div className="flex flex-col gap-2 mb-10">
        {!isMoveMode ? (
          <>
            <button
              className="flex-1 border hover:bg-gray-200 text-neutral-600 py-1.5 rounded text-sm"
              onClick={() => setIsPreviewOpen(true)}
            >
              Preview
            </button>
            <button
              className="flex-1 border hover:bg-gray-200 text-neutral-600 py-1.5 rounded text-sm"
              onClick={handleDownload}
            >
              Download
            </button>
            <button
              className="flex-1 border hover:bg-gray-200 text-neutral-600 py-1.5 rounded text-sm"
              onClick={() => setIsMoveMode(true)}
            >
              Move
            </button>
            <button
              className="flex-1 bg-red-500 hover:bg-red-600 text-white py-1.5 rounded text-sm"
              onClick={() => setIsConfirmModalOpen(true)}
            >
              Delete
            </button>
          </>
        ) : (
          <>
            <button
              className="flex-1 bg-blue-500 hover:bg-blue-600 text-white py-1.5 rounded text-sm"
              onClick={handleMoveHere}
            >
              여기로 이동
            </button>
            <button
              className="flex-1 border hover:bg-gray-200 text-neutral-600 py-1.5 rounded text-sm"
              onClick={() => setIsMoveMode(false)}
            >
              취소
            </button>
          </>
        )}
      </div>

      <div className="space-y-3 text-sm text-gray-700">
        {file && (
          <div className="mt-4 text-gray-500 text-sm">
              <b>경로: </b>
            {path.length === 0 ? (
                <span
                    onClick={() => onPathClick(null)}
                    className="cursor-pointer hover:underline"
                >ROOT</span>
            ) : (
              <>
                <span
                  onClick={() => onPathClick(null)}
                  className="cursor-pointer hover:underline"
                >
                  ROOT
                </span>
                {path.map((dir) => (
                  <span key={dir.directoryId}>
                    {" > "}
                    <span
                      onClick={() => onPathClick(dir.directoryId)}
                      className="cursor-pointer hover:underline"
                    >
                      {dir.name}
                    </span>
                  </span>
                ))}
              </>
            )}
          </div>
        )}
        <div>
          <b>타입:</b> {file.contentType}
        </div>
        <div>
          <b>크기:</b> {formatFileSize(file.fileSize)}
        </div>
        <div>
          <b>업로드 일시:</b>{" "}
          {new Date(file.uploadTime).toLocaleString("ko-KR", {
            year: "numeric",
            month: "2-digit",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit",
            hour12: false,
          })}
        </div>
      </div>

      {isPreviewOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white w-11/12 max-w-5xl h-[95vh] p-6 rounded shadow-lg flex flex-col">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-semibold">미리보기</h3>
              <button
                onClick={() => setIsPreviewOpen(false)}
                className="text-gray-500 hover:text-gray-700 text-xl"
                aria-label="미리보기 닫기"
              >
                ✕
              </button>
            </div>
            <iframe
              src={file.previewPath}
              title="미리보기"
              className="flex-1 border rounded"
            ></iframe>
          </div>
        </div>
      )}

      {isConfirmModalOpen && (
        <ConfirmModal
          isOpen={isConfirmModalOpen}
          onClose={() => setIsConfirmModalOpen(false)}
          onConfirm={handleDelete}
          message="이 파일을 삭제하시겠습니까?"
        />
      )}
    </aside>
  );
};

export default FileDetailsPanel;