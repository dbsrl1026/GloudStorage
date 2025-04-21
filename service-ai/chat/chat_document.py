from fastapi.responses import JSONResponse
from dto import ChatRequest, SearchResult, ChatResponse
from vector.hybrid_search_integration import hybrid_search_and_rerank
from langchain.prompts import PromptTemplate
from langchain.chains import LLMChain
from langchain_openai import ChatOpenAI
from config import settings

# Query simplifier chain using lightweight model
simplifier_llm = ChatOpenAI(
    openai_api_key=settings.openai_api_key,
    model_name=settings.openai_classifier_model
)

query_simplifier_prompt = PromptTemplate.from_template("""
The following sentence is a user's natural language search query written in Korean.
Please extract the core search keywords from it and return them **in Korean** as a simplified one-line query.

Original Query: {query}
Simplified Keywords (in Korean):
""")

simplify_chain = LLMChain(llm=simplifier_llm, prompt=query_simplifier_prompt)

def handle_document_request(payload: ChatRequest) -> JSONResponse:
    # Step 0: Query Simplification
    simplified_query = simplify_chain.run({"query": payload.query})

    reranked_docs = hybrid_search_and_rerank(simplified_query, payload.email, top_k=5)

    if not reranked_docs:
        return JSONResponse(content={
            "answer": "관련된 문서를 찾을 수 없습니다.",
            "documents": []
        })

    seen = set()
    top_k = 5
    docs = []

    for doc in reranked_docs:
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

    return JSONResponse(content=ChatResponse(
        answer= "'" + simplified_query + "' 에 대한 문서 검색 결과입니다.",
        documents=docs
    ).dict())