FROM python:3.11-slim

# 시스템 패키지 설치
RUN apt-get update && apt-get install -y gcc curl && rm -rf /var/lib/apt/lists/*

# 작업 디렉토리 설정
WORKDIR /app

# 의존성 복사 및 설치
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 애플리케이션 코드 복사
COPY . .

# 환경변수 파일 (.env) 주입 위치 (컨테이너 생성 시 ConfigMap이 여기에 mount됨)
ENV ENV_PATH=/app/.env

# FastAPI 실행
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8613"]