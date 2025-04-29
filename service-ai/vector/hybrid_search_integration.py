from whoosh.index import create_in, open_dir
from whoosh.fields import Schema, TEXT, ID
from whoosh.qparser import QueryParser
from config import settings
from langchain.schema import Document
from qdrant_client import QdrantClient
from qdrant_client.http.models import Filter, FieldCondition, MatchValue
from langchain_openai import OpenAIEmbeddings
import os
from utils.logging_util import LoggingUtil

logger = LoggingUtil.get_logger(__name__)

# Qdrant & Embedding 세팅
client = QdrantClient(host=settings.qdrant_host, port=settings.qdrant_port)
embedding_model = OpenAIEmbeddings(
    openai_api_key=settings.openai_api_key,
    model=settings.openai_text_embedding_model
)

# BM25 인덱스 빌드
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
        logger.info(f"[BM25] Successfully removed document_id {document_id} from index.")
    except Exception as e:
        logger.error(f"[BM25] Failed to remove document_id {document_id}: {e}")

# BM25 검색
def get_bm25_doc_ids(query, index_dir=settings.bm25_index_path, top_k=5):
    ix = open_dir(index_dir)
    with ix.searcher() as searcher:
        parser = QueryParser("content", ix.schema)
        parsed_query = parser.parse(query)
        results = searcher.search(parsed_query, limit=top_k)
        return [int(hit["doc_id"]) for hit in results]

# BM25 + Vector soft rerank
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
        upload_time = metadata.get("upload_time", "")
        preview = (content or "")[:100].replace("\n", " ")

        score = point.score

        if query.lower() in content.lower():
            score += 0.05
        if doc_id in bm25_doc_ids:
            score += 0.05

        reranked.append({
            "document_id": doc_id,
            "filename": filename,
            "preview": preview,
            "upload_time": upload_time,
            "score": score
        })

    reranked.sort(key=lambda x: x["score"], reverse=True)
    return reranked[:top_k]

# Hybrid search
def hybrid_search_and_rerank(query: str, email: str, top_k: int = 5) -> list[Document]:
    bm25_doc_ids = get_bm25_doc_ids(query, top_k=top_k)

    query_vector = embedding_model.embed_query(query)

    search_result = client.search(
        collection_name=settings.qdrant_collection_name,
        query_vector=query_vector,
        query_filter=Filter(
            must=[
                FieldCondition(
                    key="metadata.email",
                    match=MatchValue(value=email)
                )
            ]
        ),
        limit=top_k * 3,
        with_payload=True
    )

    reranked = rerank_with_bm25_and_vector(search_result, bm25_doc_ids, query, top_k)

    documents = [
        Document(
            page_content=point.get("preview", ""),
            metadata={
                "document_id": point.get("document_id", -1),
                "filename": point.get("filename", "unknown"),
                "email": email,
                "upload_time": point.get("upload_time", ""),
                "similarity": point.get("score", 0.0)
            }
        )
        for point in reranked
    ]
    return documents