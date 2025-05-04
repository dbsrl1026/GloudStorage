import {useEffect, useState} from "react";
import { useSidebarStore } from "../../stores/sidebarStore";
import axios from "../../services/axios";
import {useAuthStore} from "../../stores/authStore";
import folder from '/src/assets/icons/folder.png';
import refreshIcon from '/src/assets/icons/refresh.png';
import newFolderIcon from '/src/assets/icons/new_folder.png';
import uploadIcon from '/src/assets/icons/upload.png';
import trashIcon from '/src/assets/icons/trash.png';
import CreateFolderModal from "./CreateFolderModal";
import ConfirmModal from "./ConfirmModal";
import RenameFolderModal from "./RenameFolderModal";
export interface DirectoryDto {
    directoryId: number;
    name: string;
    ownerEmail: string;
}

export interface MetadataDto {
    metadataId: number;
    originalFilename: string;
    fileSize: number;
    contentType: string;
    uploadTime: string;
    processingStatus: string;
    directoryId?: number;
}

export interface DocumentDetailDto {
    metadataId: number;
    originalFilename: string;
    fileSize: number;
    contentType: string;
    uploadTime: string;
    processingStatus: string;
    directoryId: number;
    previewPath: string;
    // ...other fields as needed
}
interface FileExplorerProps {
    searchQuery: string;
    setSearchQuery: (q: string) => void;
    onSelectDocument: (document: DocumentDetailDto) => void;  // ✅ 경로는 따로 관리
    setSelectedDocumentPath: (path: DirectoryDto[]) => void; // ✅ 추가
    searchedDocumentIds: number[] | null;
    setSearchedDocumentIds: (ids: number[] | null) => void;
    currentDirectoryId: number | null;
    onFolderClick: (directoryId: number | null) => void;
    refreshFlag: boolean;
}

const FileExplorer = ({ searchQuery, setSearchQuery, onSelectDocument, setSelectedDocumentPath, searchedDocumentIds, setSearchedDocumentIds, currentDirectoryId, onFolderClick, refreshFlag }: FileExplorerProps) => {
    const {user} = useAuthStore();
    const { toggleSidebar } = useSidebarStore();
    const [directories, setDirectories] = useState<DirectoryDto[]>([]);
    const [documents, setDocuments] = useState<MetadataDto[]>([]);
    // 문서 필터링
    const filteredDocuments = searchedDocumentIds
        ? documents.filter((doc) => searchedDocumentIds.includes(doc.metadataId))
        : documents;
    const [path, setPath] = useState<DirectoryDto[]>([]);
    const [viewType, setViewType] = useState<"list" | "grid">("list");

    // Sorting and filtering state
    const [sortBy, setSortBy] = useState("uploadTime");
    const [direction, setDirection] = useState("desc");
    const [status, setStatus] = useState<string | undefined>();
    const [contentType, setContentType] = useState<string | undefined>();

    const [isCreateFolderModalOpen, setIsCreateFolderModalOpen] = useState(false);
    const [isConfirmModalOpen, setIsConfirmModalOpen] = useState(false);

    // Context menu and modals for folder actions
    const [contextMenu, setContextMenu] = useState<{
        x: number;
        y: number;
        targetDirectory: DirectoryDto
    } | null>(null);
    const [isRenameModalOpen, setIsRenameModalOpen] = useState(false);
    const [isMoveMode, setIsMoveMode] = useState(false);
    const [isFolderDeleteConfirmOpen, setIsFolderDeleteConfirmOpen] = useState(false);
    const [contextTargetDirectory, setContextTargetDirectory] = useState<DirectoryDto | null>(null);


    // Right click handler for folder row
    const handleFolderRightClick = (e: React.MouseEvent, directory: DirectoryDto) => {
        e.preventDefault();
        console.log("우클릭 감지", directory);
        setContextMenu({x: e.clientX, y: e.clientY, targetDirectory: directory});
        setContextTargetDirectory(directory);
    };

    const handleDocumentClick = async (metadataId: number) => {
        if (!email) return;
        try {
            const docRes = await axios.get(`/documents/${metadataId}`, {
                params: { email },
            });
            const document = docRes.data;
            if (document.directoryId !== null && document.directoryId !== undefined) {
                const pathRes = await axios.get(`/directories/${document.directoryId}/path`, {
                    params: { email },
                });
                onSelectDocument(document); // ✅ document만 넘김
                setSelectedDocumentPath(pathRes.data || []); // ✅ 별도로 path 저장
            } else {
                onSelectDocument(document);
                setSelectedDocumentPath([]);
            }
        } catch (err) {
            console.error("문서 상세 조회 실패:", err);
        }
    };

    // Multi-selection state
    const [selectedDocumentIds, setSelectedDocumentIds] = useState<number[]>([]);
    const [selectedDirectoryIds, setSelectedDirectoryIds] = useState<number[]>([]);

    const email = user?.email;

    const formatFileSize = (sizeInKB: number): string => {
        if (sizeInKB >= 1024) {
            return `${(sizeInKB / 1024).toFixed(1)} MB`;
        }
        return `${sizeInKB.toFixed(1)} KB`;
    };

    // Helper to get icon path for a given content type
    const getIconForType = (type: string) => {
        try {
            return new URL(`../../assets/icons/${type.toLowerCase()}.png`, import.meta.url).href;
        } catch {
            return new URL(`../../assets/icons/default.png`, import.meta.url).href;
        }
    };

    const fetchContent = async () => {
        if (!email) return;
        try {
            if (searchedDocumentIds) {
                const res = await axios.post("/documents/metadatas", searchedDocumentIds, {
                    params: { email },
                });
                setDirectories([]);
                setDocuments(res.data);
                setPath([]);
            } else if (searchQuery && searchQuery.trim()) {
                const res = await axios.get("/documents/search", {
                    params: { email, query: searchQuery },
                });
                setDirectories([]);
                setDocuments(res.data);
                setPath([]);
            } else {
                const res = await axios.get("/directories/contents", {
                    params: {
                        email,
                        parentId: currentDirectoryId,
                        sortBy,
                        direction,
                        status,
                        contentType,
                    },
                });
                setDirectories(res.data.directories);
                setDocuments(res.data.documents);
                setPath(res.data.fullpath || []);
            }
        } catch (err) {
            console.error("데이터 조회 실패:", err);
        }
    };



    const toggleSort = (field: string) => {
        if (sortBy === field) {
            setDirection((prev) => (prev === "asc" ? "desc" : "asc"));
        } else {
            setSortBy(field);
            setDirection("asc");
        }
    };

    const handleRefresh = async () => {
        if (email) {
            await fetchContent();
        }
    };

    const handleCreateFolder = () => {
        setIsCreateFolderModalOpen(true);
    };

    const createFolder = async (folderName: string): Promise<void> => {
        if (!folderName.trim()) return;
        try {
            await axios.post(`/directories/create`, null, {
                params: {
                    name: folderName,
                    email,
                    parentId: currentDirectoryId,
                },
            });
            setIsCreateFolderModalOpen(false);
            await fetchContent();
        } catch (err: any) {
            console.error("폴더 생성 실패:", err);
            const errorMessage = err?.response?.data?.message || "폴더 생성 중 오류가 발생했습니다.";
            throw new Error(errorMessage);
        }
    };

    const handleUploadFiles = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const files = e.target.files;
        if (!files) return;

        const formData = new FormData();
        Array.from(files).forEach(file => formData.append('files', file));
        formData.append('email', email!);
        formData.append('directoryId', (currentDirectoryId ?? "").toString());

        try {
            await axios.post(`/documents/upload/multiple`, formData, {
                headers: {'Content-Type': 'multipart/form-data'},
            });
            await fetchContent();
        } catch (err) {
            console.error("파일 업로드 실패:", err);
        }
    };

    // Selection helpers
    const toggleDocumentSelection = (metadataId: number) => {
        setSelectedDocumentIds((prev) =>
            prev.includes(metadataId) ? prev.filter(id => id !== metadataId) : [...prev, metadataId]
        );
    };

    const toggleDirectorySelection = (directoryId: number) => {
        setSelectedDirectoryIds((prev) =>
            prev.includes(directoryId) ? prev.filter(id => id !== directoryId) : [...prev, directoryId]
        );
    };

    const toggleSelectAll = () => {
        if (selectedDocumentIds.length + selectedDirectoryIds.length === documents.length + directories.length) {
            setSelectedDocumentIds([]);
            setSelectedDirectoryIds([]);
        } else {
            setSelectedDocumentIds(documents.map(doc => doc.metadataId));
            setSelectedDirectoryIds(directories.map(dir => dir.directoryId));
        }
    };

    // Delete handler - open confirm modal
    const handleDeleteSelected = () => {
        setIsConfirmModalOpen(true);
    };

    // Actual deletion logic
    const confirmDelete = async () => {
        if (!email) return;
        try {
            if (selectedDocumentIds.length > 0) {
                await axios.delete(`/documents`, {
                    params: {email},
                    data: selectedDocumentIds,
                });
            }
            if (selectedDirectoryIds.length > 0) {
                await axios.delete(`/directories`, {
                    params: {email},
                    data: selectedDirectoryIds,
                });
            }
            setSelectedDocumentIds([]);
            setSelectedDirectoryIds([]);
            await fetchContent();
        } catch (err) {
            console.error("삭제 실패:", err);
        } finally {
            setIsConfirmModalOpen(false);
        }
    };

    const handleRetry = async (metadataId: number) => {
        if (!email) return;
        try {
            await axios.post(`/documents/retry-vector-db`, null, {
                params: {
                    email,
                    metadataId,
                },
            });
            await fetchContent();
        } catch (err) {
            console.error("재시도 실패:", err);
        }
    };
    useEffect(() => {
        fetchContent();
    }, [email, currentDirectoryId, sortBy, direction, contentType, status, searchQuery, searchedDocumentIds, refreshFlag]);
    // ✅ 여기 useEffect 수정: ContextMenu가 열린 뒤에만 click 이벤트를 등록 (prevent premature close)
    useEffect(() => {
        if (!contextMenu) return;
        const handleClickOutside = () => {
            setContextMenu(null);
        };
        window.addEventListener("click", handleClickOutside);
        return () => {
            window.removeEventListener("click", handleClickOutside);
        };
    }, [contextMenu]);

return (
    <div className="flex flex-col flex-1 h-full p-4 overflow-auto min-w-0 relative">
        {/* Left: file list area */}
            <div className="flex-1 flex flex-col text-sm text-gray-800">
                {/* Breadcrumb */}
                <div className="mb-2 py-3 text-gray-600 text-base flex gap-3 items-center flex-wrap">
                  {searchedDocumentIds ? (
                    <>
                      <span className="text-xl">&nbsp;&nbsp;&nbsp;챗봇 응답 관련 문서입니다.</span>

                    </>
                  ) : (
                    <>
                      <span className="cursor-pointer hover:underline text-xl" onClick={() => onFolderClick(null)}>
                        ROOT
                      </span>
                      {path.map((dir) => (
                        <>
                          &gt;&nbsp;
                          <span
                            key={dir.directoryId}
                            className="cursor-pointer hover:underline text-xl truncate max-w-[200px]"
                            title={dir.name}
                            onClick={() => {
                                if (typeof dir.directoryId === "number") {
                                    onFolderClick(dir.directoryId);
                                }
                            }}
                          >
                            {dir.name}
                          </span>
                        </>
                      ))}
                    </>
                  )}
                </div>

                {/* Filter Bar */}
                <div className="flex items-center justify-between mb-4">
                    {/* Left: view toggle and filters grouped closely */}
                    <div className="flex items-center gap-3">
                        {/* View toggle buttons */}
                        <div className="flex border rounded-full overflow-hidden divide-x text-xs">
                            <button
                                onClick={() => setViewType("list")}
                                className={`px-2 py-1 flex items-center gap-1 ${viewType === "list" ? "bg-blue-100" : ""}`}
                            >
                                <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" viewBox="0 0 20 20"
                                     fill="currentColor">
                                    <path
                                        d="M3 5h14a1 1 0 100-2H3a1 1 0 100 2zm0 6h14a1 1 0 100-2H3a1 1 0 100 2zm0 6h14a1 1 0 100-2H3a1 1 0 100 2z"/>
                                </svg>
                            </button>
                            <button
                                onClick={() => setViewType("grid")}
                                className={`px-2 py-1 flex items-center gap-1 ${viewType === "grid" ? "bg-blue-100" : ""}`}
                            >
                                <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" viewBox="0 0 20 20"
                                     fill="currentColor">
                                    <path
                                        d="M5 3a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2V5a2 2 0 00-2-2H5zm8 0a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2V5a2 2 0 00-2-2h-2zM5 11a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2v-2a2 2 0 00-2-2H5zm8 0a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2v-2a2 2 0 00-2-2h-2z"/>
                                </svg>
                            </button>
                        </div>

                        {/* Filters */}
                        <div className="flex items-center gap-2">
                            <select
                                className="border px-2 py-1 rounded-full text-sm"
                                value={contentType}
                                onChange={(e) => setContentType(e.target.value || undefined)}
                            >
                                <option value="">모든 타입</option>
                                <option value="PDF">PDF</option>
                                <option value="TXT">TXT</option>
                                <option value="CSV">CSV</option>
                                <option value="MD">MD</option>
                                <option value="DOC">DOC</option>
                                <option value="DOCX">DOCX</option>
                                <option value="HWP">HWP</option>
                                <option value="HWPX">HWPX</option>
                                <option value="PPT">PPT</option>
                                <option value="PPTX">PPTX</option>
                            </select>
                            <select
                                className="border px-2 py-1 rounded-full text-sm"
                                value={status}
                                onChange={(e) => setStatus(e.target.value || undefined)}
                            >
                                <option value="">AI 분석 여부</option>
                                <option value="COMPLETED">COMPLETED</option>
                                <option value="PENDING">PENDING</option>
                                <option value="PROCESSING">PROCESSING</option>
                                <option value="FAILED">FAILED</option>
                            </select>
                        </div>
                    </div>

                    {/* Right: action buttons */}
                    <div className="flex items-center gap-2">
                      {isMoveMode ? (
                        <>
                          <button
                            onClick={async () => {
                              if (!contextTargetDirectory || !email) return;
                              try {
                                await axios.patch(
                                  `/directories/${contextTargetDirectory.directoryId}/move`,
                                  null,
                                  {
                                    params: {
                                      newParentId: currentDirectoryId ?? undefined,
                                      email,
                                    },
                                  }
                                );
                                setIsMoveMode(false);
                                setContextTargetDirectory(null);
                                await fetchContent();
                              } catch (err: any) {
                                const errorMessage = err?.response?.data?.message || "이동 중 오류가 발생했습니다.";
                                alert(errorMessage);
                              }
                            }}
                            className="p-2 bg-green-600 hover:bg-green-700 text-white rounded text-sm"
                          >
                            여기로 이동
                          </button>
                          <button
                            onClick={() => {
                              setIsMoveMode(false);
                              setContextTargetDirectory(null);
                            }}
                            className="p-2 bg-gray-300 hover:bg-gray-400 rounded text-sm"
                          >
                            취소
                          </button>
                        </>
                      ) : searchedDocumentIds ? (
                        // 챗봇 검색 결과 상태일 때
                        <button
                          onClick={() => {
                            setSearchQuery("");
                            setSearchedDocumentIds(null);
                          }}
                          className="p-2 mr-2 hover:bg-gray-100 rounded bg-green-600 hover:bg-green-700"
                          title="돌아가기"
                        >
                          <img src="/src/assets/icons/back.png" alt="back" className="w-5 h-5"/>
                        </button>
                      ) : (
                        <>
                          <button onClick={handleRefresh} className="p-2 hover:bg-gray-100 rounded">
                            <img src={refreshIcon} alt="refresh" className="w-5 h-5"/>
                          </button>
                          <button onClick={handleCreateFolder} className="p-2 hover:bg-gray-100 rounded">
                            <img src={newFolderIcon} alt="new folder" className="w-5 h-5"/>
                          </button>
                          {selectedDocumentIds.length > 0 || selectedDirectoryIds.length > 0 ? (
                            <button onClick={handleDeleteSelected}
                                    className="p-2 mr-2 bg-red-600 hover:bg-red-700 text-white rounded">
                              <img src={trashIcon} alt="delete" className="w-5 h-5"/>
                            </button>
                          ) : (
                            <label className="p-2 mr-2 rounded bg-blue-700 hover:bg-blue-800 text-white cursor-pointer">
                              <input type="file" multiple hidden onChange={handleUploadFiles}/>
                              <img src={uploadIcon} alt="upload" className="w-5 h-5"/>
                            </label>
                          )}
                        </>
                      )}
                    </div>
                </div>
                {viewType === "list" ? (
                    <>
                        {/* Table Header */}
                        <div className="flex font-semibold py-2 border-b">
                            <div className="w-6 flex items-center justify-center">
                                <input
                                    type="checkbox"
                                    checked={directories.length + documents.length > 0 && selectedDirectoryIds.length + selectedDocumentIds.length === directories.length + documents.length}
                                    onChange={toggleSelectAll}
                                />
                            </div>
                            <div
                                className="flex-[2] cursor-pointer hover:underline"
                                onClick={() => toggleSort("originalFilename")}
                            >
                                이름 {sortBy === "originalFilename" && (direction === "asc" ? "↑" : "↓")}
                            </div>
                            <div
                                className="flex-1 cursor-pointer hover:underline"
                                onClick={() => toggleSort("uploadTime")}
                            >
                                날짜 {sortBy === "uploadTime" && (direction === "asc" ? "↑" : "↓")}
                            </div>
                            <div
                                className="w-24 cursor-pointer hover:underline"
                                onClick={() => toggleSort("contentType")}
                            >
                                타입 {sortBy === "contentType" && (direction === "asc" ? "↑" : "↓")}
                            </div>
                            <div className="w-24">크기</div>
                            <div className="w-32">분석 상태</div>
                        </div>

                        {/* Folder Rows */}
                        {directories.map((dir) => (
                            <div
                                key={dir.directoryId}
                                className="flex py-2 border-b hover:bg-gray-50 cursor-pointer"
                                onClick={() => {
                                    if (typeof dir.directoryId === "number") {
                                        onFolderClick(dir.directoryId);
                                    }
                                }}
                                onContextMenu={(e) => handleFolderRightClick(e, dir)}
                            >
                                <div className="w-6 flex items-center justify-center">
                                    <input
                                        type="checkbox"
                                        checked={selectedDirectoryIds.includes(dir.directoryId)}
                                        onChange={() => toggleDirectorySelection(dir.directoryId)}
                                        onClick={(e) => e.stopPropagation()}
                                    />
                                </div>
                                <div className="flex-[2] flex items-center gap-2">
                                    <img
                                        src={folder}
                                        alt="folder name"
                                        className="w-5 h-5"
                                    />
                                    <span className="truncate" title={dir.name}>
                          {dir.name}
                        </span>
                                </div>
                                <div className="flex-1">-</div>
                                <div className="w-24">-</div>
                                <div className="w-24">-</div>
                                <div className="w-32">-</div>
                            </div>
                        ))}

                        {/* File Rows */}
                        {filteredDocuments.map((doc) => (
                            <div
                                key={doc.metadataId}
                                className="flex py-2 border-b hover:bg-gray-50 cursor-pointer"
                                onClick={() => handleDocumentClick(doc.metadataId)}
                            >
                                <div className="w-6 flex items-center justify-center">
                                    <input
                                        type="checkbox"
                                        checked={selectedDocumentIds.includes(doc.metadataId)}
                                        onChange={() => toggleDocumentSelection(doc.metadataId)}
                                        onClick={(e) => e.stopPropagation()}
                                    />
                                </div>
                                <div className="flex-[2] flex items-center gap-2 min-w-0">
                                    <img
                                        src={getIconForType(doc.contentType)}
                                        alt={doc.contentType}
                                        className="w-5 h-5"
                                    />
                                    <span className="truncate" title={doc.originalFilename}>
                          {doc.originalFilename}
                        </span>
                                </div>
                                <div className="flex-1">
                                    {new Date(doc.uploadTime).toLocaleString("ko-KR", {
                                        year: "numeric",
                                        month: "2-digit",
                                        day: "2-digit",
                                        hour: "2-digit",
                                        minute: "2-digit",
                                        hour12: false,
                                    })}
                                </div>
                                <div className="w-24 flex items-center">{doc.contentType}</div>
                                <div className="w-24 flex items-center">{formatFileSize(doc.fileSize)}</div>
                                <div className="w-32 flex items-center gap-2">
                                    {doc.processingStatus}
                                    {doc.processingStatus === "FAILED" && (
                                        <button
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                handleRetry(doc.metadataId);
                                            }}
                                            className="p-1 hover:bg-gray-100 rounded"
                                            title="다시 시도"
                                        >
                                            <img src={refreshIcon} alt="retry" className="w-4 h-4"/>
                                        </button>
                                    )}
                                </div>
                            </div>
                        ))}
                    </>
                ) : (
                    <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 mt-2">
                        {directories.map((dir) => (
                            <div
                                key={dir.directoryId}
                                className="border rounded p-3 hover:shadow cursor-pointer flex flex-col items-center"
                                onClick={() => {
                                    if (typeof dir.directoryId === "number") {
                                        onFolderClick(dir.directoryId);
                                    }
                                }}
                                onContextMenu={(e) => handleFolderRightClick(e, dir)}
                            >
                                <img
                                    src={folder}
                                    alt="folder name"
                                    className="w-10 h-10 mb-2"
                                />
                                <div className="text-center text-sm truncate w-full">{dir.name}</div>
                            </div>
                        ))}
                        {filteredDocuments.map((doc) => (
                            <div
                                key={doc.metadataId}
                                className="border rounded p-3 hover:shadow cursor-pointer flex flex-col items-center"
                                onClick={() => handleDocumentClick(doc.metadataId)}
                            >
                                <img
                                    src={getIconForType(doc.contentType)}
                                    alt={doc.contentType}
                                    className="w-10 h-10 mb-2"
                                />
                                <div className="text-center text-sm truncate w-full" title={doc.originalFilename}>
                                    {doc.originalFilename}
                                </div>
                            </div>
                        ))}
                    </div>
                )}

                {/* Context Menu for Folders (always rendered, after file/folder area) */}
                {contextMenu && (
                    <div
                        className="fixed bg-white shadow-lg border rounded text-sm z-50"
                        style={{top: contextMenu.y, left: contextMenu.x}}
                        onClick={(e) => e.stopPropagation()}
                    >
                        <button
                            className="block w-full text-left px-4 py-2 hover:bg-gray-100"
                            onClick={(e) => {
                                e.stopPropagation();
                                setIsRenameModalOpen(true);
                                setContextMenu(null);
                            }}
                        >
                            이름 변경
                        </button>
                        <button
                            className="block w-full text-left px-4 py-2 hover:bg-gray-100"
                            onClick={(e) => {
                                e.stopPropagation();
                                setIsMoveMode(true);
                                setContextMenu(null);
                            }}
                        >
                            이동
                        </button>
                        <button
                            className="block w-full text-left px-4 py-2 hover:bg-gray-100 text-red-500"
                            onClick={(e) => {
                                e.stopPropagation();
                                setIsFolderDeleteConfirmOpen(true);
                                setContextMenu(null);
                            }}
                        >
                            삭제
                        </button>
                    </div>
                )}

                {/* Rename Modal */}
                {isRenameModalOpen && (
                    <RenameFolderModal
                        isOpen={isRenameModalOpen}
                        onClose={() => setIsRenameModalOpen(false)}
                        onRename={async (newName) => {
                            if (!contextTargetDirectory || !email) return;
                            try {
                                await axios.patch(`/directories/${contextTargetDirectory.directoryId}/rename`, null, {
                                    params: {newName, email},
                                });
                                setIsRenameModalOpen(false);
                                await fetchContent();
                            } catch (err: any) {
                                console.error("폴더 이름 변경 실패:", err);
                                const errorMessage = err?.response?.data?.message || "폴더 이름 변경 중 오류가 발생했습니다.";
                                throw new Error(errorMessage);
                            }
                        }}
                    />
                )}
                {/* Move Modal */}
                {/* 삭제: 이동 모달은 사용하지 않고, 상단 버튼으로 처리 */}
                {/* Folder Delete Confirm Modal */}
                {isFolderDeleteConfirmOpen && (
                    <ConfirmModal
                        isOpen={isFolderDeleteConfirmOpen}
                        onClose={() => setIsFolderDeleteConfirmOpen(false)}
                        onConfirm={async () => {
                            if (!contextTargetDirectory || !email) return;
                            try {
                                await axios.delete(`/directories/${contextTargetDirectory.directoryId}`, {
                                    params: { email },
                                });
                                setIsFolderDeleteConfirmOpen(false);
                                setContextTargetDirectory(null);
                                await fetchContent();
                            } catch (err: any) {
                                console.error("폴더 삭제 실패:", err);
                                const errorMessage = err?.response?.data?.message || "폴더 삭제 중 오류가 발생했습니다.";
                                alert(errorMessage);
                            }
                        }}
                        message="이 폴더를 삭제하시겠습니까?"
                    />
                )}

                <CreateFolderModal
                    isOpen={isCreateFolderModalOpen}
                    onClose={() => setIsCreateFolderModalOpen(false)}
                    onCreate={createFolder}
                />
                <ConfirmModal
                    isOpen={isConfirmModalOpen}
                    onClose={() => setIsConfirmModalOpen(false)}
                    onConfirm={confirmDelete}
                    message="선택한 항목을 삭제하시겠습니까?"
                />
            </div>
            {/* Right: FileDetailsPanel (removed, handled only by StoragePage) */}
        {/* 챗봇 토글 버튼 - 좌하단 고정 (FileExplorer 안에서 absolute로 배치) */}
        <div className="absolute bottom-4 left-4">
          <button
            onClick={toggleSidebar}
            className="p-3 bg-blue-600 hover:bg-blue-700 text-white rounded-full shadow-lg"
            title="챗봇 열기/닫기"
          >
              <img src="/src/assets/icons/chatbot.png" alt="new folder" className="w-6 h-6"/>
          </button>
        </div>
    </div>
    );
};

export default FileExplorer;