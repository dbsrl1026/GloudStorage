from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_qdrant import Qdrant
from langchain_openai import OpenAIEmbeddings
from langchain.schema import Document
from qdrant_client import QdrantClient
from qdrant_client.http.models import Distance, VectorParams
from config import settings
from my_kafka.producer import safe_send_status
from vector.hybrid_search_integration import build_bm25_index

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
# client.recreate_collection(
#     collection_name=collection_name,
#     vectors_config=VectorParams(size=3072, distance=Distance.COSINE)
# )

embeddings = OpenAIEmbeddings(
    openai_api_key=settings.openai_api_key,
    model=settings.openai_text_embedding_model
)

text = (
    "이 문서는 이메일 필터링 기능이 정상적으로 작동하는지 확인하기 위해 작성된 테스트 문서입니다.\n"
    "작성자는 someone@fake.com 이며, 검색 필터에 따라 이 문서가 검색되지 않아야 합니다."
)

# 2. metadata에 다른 이메일 지정
metadata = {
    "document_id": 9998,
    "email": "someone@fake.com",
    "filename": "filter_test_other_user.txt",
    "upload_time": "2025-04-17T16:15:00"
}



def index_text_to_qdrant(text: str, metadata: dict):
    # print("📦 Metadata being passed to Qdrant:")
    # print(metadata)
    splitter = RecursiveCharacterTextSplitter(chunk_size=400, chunk_overlap=80)
    flattened_chunks = [
        Document(page_content=chunk.page_content, metadata={**chunk.metadata, **metadata})
        for chunk in splitter.create_documents([text], metadatas=[metadata])
    ]

    try:
        qdrant = Qdrant(
            client=client,
            collection_name=collection_name,
            embeddings=embeddings,
        )
        qdrant.add_documents(flattened_chunks)

        # 가장 먼저 확인: Qdrant에서 직접 payload 구조 보기
        points, _ = client.scroll(collection_name="documents", limit=5, with_payload=True)

        for i, point in enumerate(points):
            print(f"\n[Point {i}]")
            print("ID:", point.id)
            print("Payload:", point.payload)

        documents = [
            {
                "document_id": metadata["document_id"],
                "page_content": chunk.page_content
            }
            for chunk in flattened_chunks  # Qdrant에 저장한 LangChain 문서들
        ]

        build_bm25_index(documents)

    except Exception as e:
        print(f"[Error] Failed to index document: {e}")
index_text_to_qdrant(text, metadata)