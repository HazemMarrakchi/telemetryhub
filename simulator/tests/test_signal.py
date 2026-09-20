import random

from app.signal import MetricProfile, generate_batch, generate_reading


def test_default_metric_set_has_expected_metrics():
    names = {m.name for m in [
        MetricProfile("temperature", "°C", 72.0, 2.5, 600.0, 0.4, 1.6),
    ]}
    assert "temperature" in names


def test_generate_reading_shape():
    reading = generate_reading(
        "eq-0001",
        "tenant-1",
        MetricProfile("pression", "bar", 6.0, 0.6, 900.0, 0.05, 1.8),
        now=1_700_000_000.0,
        rng=random.Random(0),
    )
    assert reading["equipmentId"] == "eq-0001"
    assert reading["tenantId"] == "tenant-1"
    assert reading["metric"] == "pression"
    assert reading["unit"] == "bar"
    assert reading["source"] == "SIM"
    assert "recordedAt" in reading
    assert reading["deviceFingerprint"] == "eq-0001-sim"


def test_normal_value_stays_close_to_base():
    profile = MetricProfile("temperature", "°C", 72.0, 2.5, 600.0, 0.4, 1.6)
    values = [
        generate_reading("eq-1", "t", profile, now=float(i), rng=random.Random(i))["value"]
        for i in range(200)
    ]
    assert all(66.0 < v < 78.0 for v in values)


def test_anomaly_creates_spike():
    profile = MetricProfile("temperature", "°C", 72.0, 2.5, 600.0, 0.4, 1.6)
    rng = random.Random(7)
    spiked = 0
    for _ in range(500):
        value = generate_reading("eq-1", "t", profile, now=float(_), rng=rng, anomaly_rate=0.2)["value"]
        if value > 100.0:
            spiked += 1
    assert spiked > 0


def test_batch_covers_all_metrics():
    batch = generate_batch("eq-1", "t", now=1_700_000_000.0, rng=random.Random(1))
    metrics = {b["metric"] for b in batch}
    assert {"temperature", "vibration", "pression", "debit", "puissance", "humidite"} == metrics
