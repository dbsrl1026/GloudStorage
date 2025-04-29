import { useSidebarStore } from "../stores/sidebarStore";
import Header from "../components/layout/Header";
import Sidebar from "../components/layout/Sidebar";
import FileExplorer from "../components/file/FileExplorer";
import FileDetailsPanel from "../components/file/FileDetailsPanel";
import {useEffect, useState} from "react";

const StoragePage = () => {
    const { isSidebarOpen } = useSidebarStore();
    const [searchQuery, setSearchQuery] = useState("");
    const [selectedDocumentDetail, setSelectedDocumentDetail] = useState(null);
    const [selectedDocumentPath, setSelectedDocumentPath] = useState([]);
    const [searchedDocumentIds, setSearchedDocumentIds] = useState<number[] | null>(null);
    const [currentDirectoryId, setCurrentDirectoryId] = useState<number | null>(null);
    const [refreshFlag, setRefreshFlag] = useState(false);

    const refreshContent = () => {
        setRefreshFlag(prev => !prev); // ✅ 값만 토글
    };

    return (
        <div className="flex flex-col h-screen">
            {/* 상단 헤더 */}
            <Header searchQuery={searchQuery} setSearchQuery={setSearchQuery} />

            {/* 본문 전체 */}
            <div className="flex flex-1 h-[calc(100vh-64px)] overflow-hidden">
                {/* Sidebar */}
                <div className={`${isSidebarOpen ? 'w-88' : 'w-0'} flex-shrink-0 transition-all duration-300 overflow-hidden`}>
                    <Sidebar setSearchedDocumentIds={setSearchedDocumentIds} />
                </div>

                {/* 중앙 메인 영역 */}
                <div className="flex flex-1 overflow-hidden">
                    {/* FileExplorer */}
                    <div className="flex-1 min-w-0 overflow-auto">
                        <FileExplorer
                            searchQuery={searchQuery}
                            setSearchQuery={setSearchQuery}
                            onSelectDocument={setSelectedDocumentDetail}
                            setSelectedDocumentPath={setSelectedDocumentPath}
                            searchedDocumentIds={searchedDocumentIds}
                            setSearchedDocumentIds={setSearchedDocumentIds}
                            currentDirectoryId={currentDirectoryId}
                            onFolderClick={(directoryId) => setCurrentDirectoryId(directoryId)}
                            refreshFlag={refreshFlag} // ✅ 추가
                        />
                    </div>

                    {/* FileDetailsPanel */}
                    {selectedDocumentDetail && (
                        <div className="w-64 flex-shrink-0 transition-all duration-300 overflow-hidden">
                            <FileDetailsPanel
                                document={selectedDocumentDetail}
                                onClose={() => setSelectedDocumentDetail(null)}
                                refreshContent={refreshContent} // ✅ 추가
                                currentDirectoryId={currentDirectoryId}
                                path={selectedDocumentPath}
                                onPathClick={(directoryId) => {
                                    setSearchQuery("");
                                    setCurrentDirectoryId(directoryId);
                                    setSearchedDocumentIds(null);
                                }}
                            />
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default StoragePage;