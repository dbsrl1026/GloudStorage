from fastapi import APIRouter, Query, Header, Request
from starlette.responses import JSONResponse

from dto import SearchResult
from langchain_openai import OpenAIEmbeddings
from langchain_qdrant import Qdrant
from qdrant_client import QdrantClient
from config import settings
from utils.jwt import verify_email_from_token
from vector.hybrid_search_integration import hybrid_search_and_rerank


router = APIRouter()

client = QdrantClient(host=settings.qdrant_host, port=settings.qdrant_port)
embeddings = OpenAIEmbeddings(
    openai_api_key=settings.openai_api_key,
    model=settings.openai_text_embedding_model
)

qdrant = Qdrant(
    client=client,
    collection_name=settings.qdrant_collection_name,
    embeddings=embeddings
)


@router.get("/ai/search", response_model=list[SearchResult])
def search_documents(
    request: Request,
    query: str = Query(..., description="검색할 사용자 질의"),
    email: str = Query(..., description="검색할 대상 email"),
    top_k: int = 5
):
    verify_email_from_token(request, email)

    reranked = hybrid_search_and_rerank(query=query, email=email, top_k=top_k)
    if not reranked:
        return JSONResponse(content=[])

    seen = set()
    top_k = 5
    docs = []

    for doc in reranked:
        metadata = doc.metadata or {}
        doc_id = metadata.get("document_id", -1)
        if doc_id in seen:
            continue
        seen.add(doc_id)
        docs.append(SearchResult(
            document_id=doc_id,
            filename=metadata.get("filename", "unknown"),
            preview=(doc.page_content or "")[:100].replace("\n", " "),
            similarity=metadata.get("similarity", 1.0)
        ))
        if len(docs) >= top_k:
            break

    return docs
