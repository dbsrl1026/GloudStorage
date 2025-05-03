from qdrant_client import QdrantClient
import pandas as pd
import matplotlib.pyplot as plt


client = QdrantClient(host="169.211.207.99", port=1031)
collection_name = "documents"
# 컬렉션 목록
print(client.get_collections())

# 문서 수 확인
print(client.count(collection_name=collection_name, exact=True))


# 전체 포인트 가져오기 (최대 1000건 예시)
result = client.scroll(
    collection_name=collection_name,
    limit=1000,
    with_payload=True
)

points = result[0]  # List[PointStruct]

# 포인트들을 pandas로 정리
data = []
for p in points:
    meta = p.payload or {}
    inner = meta.get("metadata", {})
    content = meta.get("page_content", "N/A")

    data.append({
        "document_id": inner.get("document_id", "unknown"),
        "email": inner.get("email", "unknown"),
        "filename": inner.get("filename", "unknown"),
        "length": len(content),
        "preview": content[:100].replace("\n", " ") + "..."
    })
df = pd.DataFrame(data)

if df.empty:
    print(" Qdrant에서 데이터를 가져오지 못했습니다. 저장된 벡터가 없습니다.")
else:
    df['document_id'] = df['document_id'].astype(str)
    df.groupby("document_id").size().plot(kind='bar', title="nums of chunks by Document ID")
    plt.ylabel("Chunk Count")
    plt.xlabel("Document ID")
    plt.tight_layout()
    plt.show()

    df["length"].plot.hist(bins=30, title="Chunk length distribution")
    plt.xlabel("Text Length (chars)")
    plt.show()

    print(df[["document_id", "filename", "length", "preview"]].head(30))