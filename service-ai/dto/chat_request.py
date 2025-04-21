from pydantic import BaseModel
from typing import List, Tuple

class ChatRequest(BaseModel):
    query: str
    email: str
    history: List[Tuple[str, str]] = []