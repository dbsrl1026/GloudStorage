from fastapi import Request, FastAPI
from my_kafka.consumer import consume_kafka
from my_kafka.dlt_consumer import consume_dlt
from router import search_router, chat_router
from utils.logging_util import LoggingUtil
from utils.exception_handlers import add_exception_handlers
from utils.consul_util import register_service_to_consul
from config import settings
from fastapi.middleware.cors import CORSMiddleware
import threading


logger = LoggingUtil.get_logger(__name__)


app = FastAPI()

add_exception_handlers(app)
app.include_router(search_router)
app.include_router(chat_router)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 또는 ["https://your-frontend.com"]
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.on_event("startup")
def startup_event():
    # Kafka consumer 시작
    for _ in range(3):
        threading.Thread(target=consume_kafka, daemon=True).start()
    # DLT consumer는 일반적으로 1개만 있어도 충분(오류 로깅용)
    threading.Thread(target=consume_dlt, daemon=True).start()
    # Consul 등록
    register_service_to_consul("service-ai", settings.service_port)

@app.get("/health")
def health():
    return {"status": "ok"}
