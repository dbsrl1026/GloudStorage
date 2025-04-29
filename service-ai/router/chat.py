from fastapi import APIRouter, Header, Request
from config import settings
from langchain.chains import ConversationalRetrievalChain
from langchain_openai import ChatOpenAI
from dto import ChatRequest, ChatResponse
from utils.jwt import verify_email_from_token
from langchain_core.prompts import PromptTemplate
from langchain_core.runnables import RunnableLambda
from chat.chat_answer import handle_answer_request
from chat.chat_document import handle_document_request
from utils.logging_util import LoggingUtil
from fastapi.responses import StreamingResponse
import asyncio
import json

logger = LoggingUtil.get_logger(__name__)
router = APIRouter()

classifier_llm = ChatOpenAI(
    openai_api_key=settings.openai_api_key,
    model_name=settings.openai_classifier_model
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
async def chat_with_documents(request: Request, payload: ChatRequest):
    logger.info(f"Received chat request: {payload}")
    verify_email_from_token(request, payload.email)

    async def event_stream():
        # Step 1: 질의 분류
        mode = await asyncio.to_thread(classifier_chain.invoke, {"query": payload.query})
        is_search_mode = "(a)" in mode or "document" in mode

        # Step 2: 질의 타입에 따라 다르게 처리
        response = await asyncio.to_thread(
            handle_document_request if is_search_mode else handle_answer_request,
            payload
        )

        # Step 3: Pydantic Model → dict → JSON
        yield json.dumps(response.dict(), ensure_ascii=False)

    return StreamingResponse(event_stream(), media_type="application/json")