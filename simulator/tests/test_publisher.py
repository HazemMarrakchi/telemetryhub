from app.publisher import TelemetryPublisher


class FakeProducer:
    def __init__(self):
        self.sent = []

    def send(self, topic, key, value):
        self.sent.append((topic, key, value))

    def flush(self):
        pass

    def close(self):
        pass


def test_publisher_sends_batch_with_equipment_key():
    producer = FakeProducer()
    publisher = TelemetryPublisher(
        bootstrap_servers="unused",
        topic="telemetry.raw",
        producer_factory=lambda: producer,
    )

    readings = [
        {"equipmentId": "eq-0001", "tenantId": "t", "metric": "temperature",
         "value": 71.5, "unit": "°C", "recordedAt": "2026-09-09T08:00:00Z",
         "source": "SIM", "deviceFingerprint": "f1"},
        {"equipmentId": "eq-0001", "tenantId": "t", "metric": "vibration",
         "value": 3.4, "unit": "mm/s", "recordedAt": "2026-09-09T08:00:00Z",
         "source": "SIM", "deviceFingerprint": "f1"},
    ]

    publisher.send_batch(readings)

    assert len(producer.sent) == 2
    for topic, key, value in producer.sent:
        assert topic == "telemetry.raw"
        assert key == "eq-0001"
        assert value["metric"] in {"temperature", "vibration"}


def test_flush_and_close_are_safe():
    publisher = TelemetryPublisher(
        bootstrap_servers="unused",
        topic="telemetry.raw",
        producer_factory=FakeProducer,
    )
    publisher.send_batch([
        {"equipmentId": "eq-1", "tenantId": "t", "metric": "debit",
         "value": 84.0, "unit": "m3/h", "recordedAt": "2026-09-09T08:00:00Z",
         "source": "SIM", "deviceFingerprint": "f1"}
    ])
    publisher.flush()
    publisher.close()
