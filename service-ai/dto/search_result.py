from pydantic import BaseModel
from typing import Optional

class SearchResult(BaseModel):
    document_id: int
    filename: str
    preview: str
    similarity: Optional[float] = 1.0