from fastapi.responses import JSONResponse
from langchain.chains import ConversationalRetrievalChain
from langchain_qdrant import Qdrant
from config import settings
from dto import ChatRequest, SearchResult, ChatResponse
from langchain_openai import ChatOpenAI, OpenAIEmbeddings
from qdrant_client import QdrantClient
from langchain.prompts import ChatPromptTemplate

# LLM & Qdrant client 설정
llm = ChatOpenAI(openai_api_key=settings.openai_api_key, model_name=settings.openai_chat_model)
client = QdrantClient(host=settings.qdrant_host, port=settings.qdrant_port)
embedding_model = OpenAIEmbeddings(openai_api_key=settings.openai_api_key, model=settings.openai_text_embedding_model)

retriever = Qdrant(
    client=client,
    collection_name=settings.qdrant_collection_name,
    embeddings=embedding_model
).as_retriever()

custom_prompt = ChatPromptTemplate.from_messages([
    ("system", "You are a helpful assistant that analyzes internal document context to help the user. "
               "Only use the provided document excerpts to answer the user's question. "
               "If no relevant information is found in the documents, say: '해당 문서에는 명확한 정보가 없습니다.'."),
    ("human", "{question}")
])

answer_chain = ConversationalRetrievalChain.from_llm(
    llm=llm,
    retriever=retriever,
    return_source_documents=True,
    condense_question_prompt=custom_prompt
)

def handle_answer_request(payload: ChatRequest) -> JSONResponse:
    result = answer_chain.invoke({
        "question": payload.query,
        "chat_history": payload.history
    })

    if not result["source_documents"]:
        return JSONResponse(content={
            "answer": "관련된 문서를 찾을 수 없습니다.",
            "documents": []
        })

    seen = set()
    top_k = 5
    docs = []

    for doc in result["source_documents"]:
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
        answer=result.get("answer", ""),
        documents=docs
    ).dict())