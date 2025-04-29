from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_qdrant import Qdrant
from langchain_openai import OpenAIEmbeddings
from qdrant_client import QdrantClient
from qdrant_client.http.models import Distance, VectorParams
from config import settings
from my_kafka.producer import safe_send_status
from vector.hybrid_search_integration import build_bm25_index
from utils.logging_util import LoggingUtil

logger = LoggingUtil.get_logger(__name__)

client = QdrantClient(
    host=settings.qdrant_host,
    port=settings.qdrant_port
)

collection_name = settings.qdrant_collection_name

# 컬렉션이 없을 때만 생성
if collection_name not in [col.name for col in client.get_collections().collections]:
    client.create_collection(
        collection_name=collection_name,
        vectors_config=VectorParams(size=3072, distance=Distance.COSINE)
    )

embeddings = OpenAIEmbeddings(
    openai_api_key=settings.openai_api_key,
    model=settings.openai_text_embedding_model
)


def index_text_to_qdrant(text: str, metadata: dict):
    # 제목(파일명)을 본문 앞에 추가하여 벡터화 정확도 향상
    title = metadata.get("filename", "").replace("_", " ").replace("-", " ").split(".")[0]
    text = f"[제목: {title}]\n{text}"

    splitter = RecursiveCharacterTextSplitter(chunk_size=400, chunk_overlap=80)
    chunks = splitter.create_documents([text], metadatas=[metadata])

    try:
        # 각 Document의 metadata에서 email을 꺼내 payload 최상단에 포함시키도록 수정
        docs_with_email_payload = []
        for chunk in chunks:
            payload = dict(chunk.metadata)
            email = payload.pop("email", None)
            if email is not None:
                payload = {"email": email, **payload}
            docs_with_email_payload.append(chunk.copy(update={"metadata": payload}))

        qdrant = Qdrant(
            client=client,
            collection_name=collection_name,
            embeddings=embeddings,
        )
        qdrant.add_documents(docs_with_email_payload)

        documents = [
            {
                "document_id": metadata["document_id"],
                "page_content": chunk.page_content,
                "email": chunk.metadata.get("email")  # Ensure email is included for bm25 indexing
            }
            for chunk in docs_with_email_payload  # Qdrant에 저장한 LangChain 문서들
        ]

        build_bm25_index(documents)
        safe_send_status(metadata["document_id"], "COMPLETED")

    except Exception as e:
        logger.error(f"[Error] Failed to index document: {e}")
        safe_send_status(metadata["document_id"], "FAILED")

# Note: After this modification, please verify in hybrid_search_integration.py that:
# - Vector search filtering is properly working based on the email field at the payload top level.
# - The bm25_index also correctly stores and uses the email metadata information.
