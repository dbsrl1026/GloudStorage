import requests
import socket
from config import settings

def register_service_to_consul(service_name: str, service_port: int):
    service_id = f"{service_name}-{service_port}"
    # service_host = socket.gethostbyname(socket.gethostname())  # 또는 직접 IP 지정
    service_host = settings.service_host
    consul_host = settings.consul_host

    payload = {
        "Name": service_name,
        "ID": service_id,
        "Address": service_host,
        "Port": service_port,
        "Check": {
            "HTTP": f"http://{service_host}:{service_port}/health",
            "Interval": "10s"
        }
    }

    try:
        res = requests.put(f"{consul_host}/v1/agent/service/register", json=payload)
        print(f"[Consul] 등록 상태: {res.status_code}")
        # print(f"[Consul] 응답 내용: {res.text}")
    except Exception as e:
        print(f"[Consul] 등록 실패: {e}")