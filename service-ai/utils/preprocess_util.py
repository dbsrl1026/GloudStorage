import re

ABUSE_PATTERNS = [
    # 영어 기반 프롬프트 주입
    r"(?i)ignore previous instructions",
    r"(?i)pretend.*system",
    r"(?i)disregard system message",
    r"(?i)you are.*now",
    r"(?i)act as .*assistant",

    # 한국어 기반 프롬프트 주입
    r"(이전|위)의 지시.*무시",  # 이전 지시 무시해
    r"(너는|당신은) 이제 .*로 행동해",  # 이제 넌 해커야
    r"(시스템|프롬프트).*무시",  # 시스템 메시지를 무시하고
    r"(지금부터|이후).*응답해",  # 지금부터는 그대로 따라해
]

def check_for_prompt_injection(query: str) -> tuple[bool, str]:
    """
    위험한 지시 문구 탐지 → True: 차단 필요
    """
    for pattern in ABUSE_PATTERNS:
        if re.search(pattern, query):
            return True, query
    return False, re.sub(r"\s+", " ", query.strip())