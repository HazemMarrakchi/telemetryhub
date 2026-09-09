from app.anomaly import Anomaly, ZScoreDetector, _severity


def _reading(value: float, metric: str = "temperature") -> dict:
    return {
        "equipmentId": "eq-0001",
        "tenantId": "tenant-1",
        "metric": metric,
        "value": value,
        "unit": "°C",
        "recordedAt": "2026-09-09T08:00:00.000Z",
        "source": "SIM",
        "deviceFingerprint": "fp-1",
    }


def test_stable_series_produces_no_anomaly():
    detector = ZScoreDetector(window_size=40, z_threshold=3.5)
    anomalies = []
    for i in range(120):
        reading = _reading(72.0 + (i % 3))
        anomaly = detector.analyze(reading)
        if anomaly:
            anomalies.append(anomaly)
    assert anomalies == []


def test_spike_is_detected():
    detector = ZScoreDetector(window_size=40, z_threshold=3.5)
    for i in range(80):
        detector.analyze(_reading(72.0 if i == 0 else 72.0 + (i % 2)))
    anomaly = detector.analyze(_reading(150.0))
    assert anomaly is not None
    assert anomaly.metric == "temperature"
    assert anomaly.z_score >= 3.5


def test_anomaly_payload_shape():
    detector = ZScoreDetector(window_size=20, z_threshold=3.5)
    for i in range(40):
        detector.analyze(_reading(72.0 + (i % 2) * 0.1))
    anomaly = detector.analyze(_reading(40.0))
    assert isinstance(anomaly, Anomaly)
    payload = anomaly.to_dict()
    assert {"equipmentId", "tenantId", "metric", "value", "zScore",
            "severity", "recordedAt"} <= set(payload)


def test_severity_levels():
    assert _severity(3.9) == "INFO"
    assert _severity(4.8) == "WARNING"
    assert _severity(7.0) == "CRITICAL"


def test_reset_clears_state():
    detector = ZScoreDetector(window_size=20, z_threshold=3.5)
    for _ in range(40):
        detector.analyze(_reading(72.0))
    detector.reset()
    assert detector._buffers == {}
