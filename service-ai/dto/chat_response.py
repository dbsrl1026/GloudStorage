from pydantic import BaseModel
from typing import List, Optional
from dto import SearchResult


class ChatResponse(BaseModel):
    answer: Optional[str] = None
    documents: List[SearchResult]