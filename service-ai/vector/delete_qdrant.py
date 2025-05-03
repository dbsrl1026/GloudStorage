from config import settings
from qdrant_client import QdrantClient
from vector.hybrid_search_integration import remove_from_bm25_index
from qdrant_client.http.models import Filter, FieldCondition, MatchValue
from utils.logging_util import LoggingUtil

logger = LoggingUtil.get_logger(__name__)

client = QdrantClient(host=settings.qdrant_host, port=settings.qdrant_port)


def remove_from_qdrant_and_bm25(document_id: int):
    try:
        # Qdrant에서 document_id 기준으로 삭제
        qdrant_filter = Filter(
            must=[
                FieldCondition(
                    key="metadata.document_id",
                    match=MatchValue(value=document_id)
                )
            ]
        )

        client.delete(
            collection_name=settings.qdrant_collection_name,
            points_selector=qdrant_filter  # ✅ 올바른 키워드 파라미터명
        )
        logger.info(f"[Qdrant] Deleted document_id: {document_id}")
    except Exception as e:
        logger.info(f"[Qdrant] Failed to delete document_id {document_id}: {e}")

    try:
        remove_from_bm25_index(document_id)
        logger.info(f"[BM25] Removed document_id {document_id} from index")
    except Exception as e:
        logger.error(f"[BM25] Failed to remove document_id {document_id}: {e}")