from whoosh.index import create_in, open_dir
from whoosh.fields import Schema, TEXT, ID
from whoosh.qparser import QueryParser
from config import settings
from langchain.schema import Document
from qdrant_client import QdrantClient
from langchain_openai import OpenAIEmbeddings
import os
from utils.logging_util import LoggingUtil

logger = LoggingUtil.get_logger(__name__)


client = QdrantClient(host=settings.qdrant_host, port=settings.qdrant_port)
embedding_model = OpenAIEmbeddings(
    openai_api_key=settings.openai_api_key,
    model=settings.openai_text_embedding_model
)

# Step 1: Create and build BM25 index using Whoosh
def build_bm25_index(documents, index_dir=settings.bm25_index_path):
    schema = Schema(doc_id=ID(stored=True), content=TEXT)
    if not os.path.exists(index_dir):
        os.mkdir(index_dir)
        ix = create_in(index_dir, schema)
    else:
        ix = open_dir(index_dir)

    writer = ix.writer()
    for doc in documents:
        writer.add_document(doc_id=str(doc["document_id"]), content=doc["page_content"])
    writer.commit()

def remove_from_bm25_index(document_id: int, index_dir=settings.bm25_index_path):
    try:
        ix = open_dir(index_dir)
        writer = ix.writer()
        writer.delete_by_term("doc_id", str(document_id))
        writer.commit()
        logger.error(f"[BM25] Successfully removed document_id {document_id} from index.")
    except Exception as e:
        logger.error(f"[BM25] Failed to remove document_id {document_id}: {e}")


# Step 2: BM25 검색 결과를 반환하는 함수
def get_bm25_doc_ids(query, index_dir=settings.bm25_index_path, top_k=5):
    ix = open_dir(index_dir)
    with ix.searcher() as searcher:
        parser = QueryParser("content", ix.schema)
        parsed_query = parser.parse(query)
        results = searcher.search(parsed_query, limit=top_k)
        return [int(hit["doc_id"]) for hit in results]


# Step 3: 기존 FastAPI search_documents 라우터에 통합 (중요 부분만)

def rerank_with_bm25_and_vector(search_result, bm25_doc_ids, query, top_k):
    seen_doc_ids = set()
    reranked = []

    for point in search_result:
        payload = point.payload or {}
        metadata = payload.get("metadata", {})
        doc_id = metadata.get("document_id", -1)

        if doc_id in seen_doc_ids:
            continue
        seen_doc_ids.add(doc_id)

        filename = metadata.get("filename", "unknown")
        content = payload.get("page_content", "")
        preview = content[:100].replace("\n", " ")

        score = point.score
        if query.lower() in content.lower():
            score += 0.05  # soft rerank
        if doc_id in bm25_doc_ids:
            score += 0.05  # hybrid rerank

        reranked.append({
            "document_id": doc_id,
            "filename": filename,
            "preview": preview,
            "score": score
        })

    reranked.sort(key=lambda x: x["score"], reverse=True)
    return reranked[:top_k]


def hybrid_search_and_rerank(query: str, email: str, top_k: int = 5) -> list[Document]:
    # BM25 검색
    bm25_doc_ids = get_bm25_doc_ids(query, top_k=top_k)

    # 쿼리 임베딩
    query_vector = embedding_model.embed_query(query)

    # Qdrant 벡터 검색
    search_result = client.search(
        collection_name=settings.qdrant_collection_name,
        query_vector=query_vector,
        limit=top_k * 3, # 중복 제거를 고려해 여유 있게 요청
        with_payload=True,
        query_filter={
            "must": [
                {
                    "key": "metadata.email",
                    "match": {
                        "value": email
                    }
                }
            ]
        }
    )

    # rerank 적용
    reranked = rerank_with_bm25_and_vector(search_result, bm25_doc_ids, query, top_k)

    # LangChain Document 포맷으로 변환
    documents = [
        Document(
            page_content=point.get("preview", ""),
            metadata={
                "document_id": point.get("document_id", -1),
                "filename": point.get("filename", "unknown"),
                "email": point.get("email", "unknown"),
                "upload_time": point.get("upload_time", ""),
                "similarity": point.get("score", 0.0)
            }
        )
        for point in reranked
    ]
    return documents
