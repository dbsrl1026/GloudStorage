from fastapi import APIRouter, Header
from config import settings
from langchain.chains import ConversationalRetrievalChain
from langchain_openai import ChatOpenAI
from dto import ChatRequest, ChatResponse
from utils.jwt import verify_email_from_token
from langchain_core.prompts import PromptTemplate
from langchain_core.runnables import RunnableLambda
from chat.chat_answer import handle_answer_request
from chat.chat_document import handle_document_request
router = APIRouter()

classifier_llm = ChatOpenAI(
    openai_api_key=settings.openai_api_key,
    model_name=settings.openai_classifier_model  # faster model for classification
)


classification_prompt = PromptTemplate.from_template("""
You are a classifier. Label the following user query as either:
(A) document request
(B) answer request

Query: {query}
Answer:
""")

classifier_chain = classification_prompt | classifier_llm | RunnableLambda(lambda x: x.content.lower())


@router.post("/ai/chat", response_model=ChatResponse)
def chat_with_documents(payload: ChatRequest, authorization: str = Header(...)):
    verify_email_from_token(authorization, payload.email)

    # Step 1: 질의 분류
    mode = classifier_chain.invoke({"query": payload.query})
    is_search_mode = "(a)" in mode or "document" in mode

    return handle_document_request(payload) if is_search_mode else handle_answer_request(payload)