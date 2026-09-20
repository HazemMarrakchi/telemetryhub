import os

os.environ["TELEMETRYHUB_DB_OFF"] = "1"
os.environ["TELEMETRYHUB_KAFKA_OFF"] = "1"

from fastapi.testclient import TestClient

from app.main import app


def test_health_endpoint():
    with TestClient(app) as client:
        response = client.get("/api/v1/ai/health")
        assert response.status_code == 200
        body = response.json()
        assert body["status"] == "ok"
        assert body["service"] == "ai-service"
        assert body["dbEnabled"] is False


def test_detect_batch_detects_spike():
    with TestClient(app) as client:
        readings = [
            {"equipmentId": "eq-1", "tenantId": "t", "metric": "temperature",
             "value": 72.0 + (i % 2) * 0.1, "unit": "°C", "recordedAt": f"2026-09-09T08:00:{i:02d}Z", "source": "API"}
            for i in range(40)
        ]
        readings.append({"equipmentId": "eq-1", "tenantId": "t", "metric": "temperature",
                         "value": 200.0, "unit": "°C", "recordedAt": "2026-09-09T08:01:00Z", "source": "API"})
        response = client.post("/api/v1/ai/detect", json={"tenantId": "t", "readings": readings})
        assert response.status_code == 200
        body = response.json()
        assert body["count"] >= 1
        assert body["anomalies"][0]["zScore"] >= 3.5


def test_chat_returns_sources():
    with TestClient(app) as client:
        response = client.post("/api/v1/assistant/chat",
                               json={"question": "seuil température compresseur", "tenantId": "t"})
        assert response.status_code == 200
        body = response.json()
        assert "answer" in body
        assert isinstance(body["sources"], list)


def test_anomalies_endpoint_unavailable_without_db():
    with TestClient(app) as client:
        response = client.get("/api/v1/ai/anomalies", params={"tenantId": "t"})
        assert response.status_code == 503
