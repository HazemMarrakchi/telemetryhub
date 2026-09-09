from __future__ import annotations

import argparse
import logging
import os
import random
import time

from app.publisher import TelemetryPublisher
from app.signal import DEFAULT_METRICS, generate_batch

logger = logging.getLogger("simulator")


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        prog="telemetryhub-simulator",
        description="Simule un parc d'equipements industriels envoyant des mesures temps reel.",
    )
    parser.add_argument(
        "--bootstrap-servers",
        default=os.environ.get("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"),
    )
    parser.add_argument("--topic", default=os.environ.get("KAFKA_RAW_TOPIC", "telemetry.raw"))
    parser.add_argument("--tenant", default=os.environ.get("SIM_TENANT_ID", "11111111-1111-1111-1111-111111111111"))
    parser.add_argument("--equipment-count", type=int,
                        default=int(os.environ.get("SIM_EQUIPMENT_COUNT", "12")))
    parser.add_argument("--interval-seconds", type=float,
                        default=float(os.environ.get("SIM_INTERVAL_SECONDS", "5.0")))
    parser.add_argument("--anomaly-rate", type=float,
                        default=float(os.environ.get("SIM_ANOMALY_RATE", "0.02")),
                        help="Probabilite qu'une metrique subisse un spike (0..1)")
    parser.add_argument("--seed", type=int, default=int(os.environ.get("SIM_SEED", "42")))
    parser.add_argument("--once", action="store_true",
                        help="Emet un seul lot puis s'arrete (mode test)")
    return parser.parse_args(argv)


def build_equipment_ids(count: int, rng: random.Random) -> list[str]:
    return [f"eq-{i:04d}-{rng.randint(1000, 9999)}" for i in range(count)]


def run(args: argparse.Namespace) -> int:
    rng = random.Random(args.seed)
    equipment_ids = build_equipment_ids(args.equipment_count, rng)
    publisher = TelemetryPublisher(args.bootstrap_servers, args.topic)

    logger.info(
        "Simulation demarree: %d equipements, intervalle %.1fs, topic=%s, tenant=%s",
        len(equipment_ids), args.interval_seconds, args.topic, args.tenant,
    )

    try:
        while True:
            now = time.time()
            batches = [
                generate_batch(eq, args.tenant, now, rng, DEFAULT_METRICS, args.anomaly_rate)
                for eq in equipment_ids
            ]
            for batch in batches:
                publisher.send_batch(batch)
            publisher.flush()
            logger.debug("Lot publie: %d lectures", sum(len(b) for b in batches))

            if args.once:
                return 0
            time.sleep(args.interval_seconds)
    except KeyboardInterrupt:
        logger.info("Simulation arretee.")
    finally:
        publisher.close()
    return 0


def main(argv: list[str] | None = None) -> int:
    logging.basicConfig(
        level=os.environ.get("LOG_LEVEL", "INFO"),
        format="%(asctime)s %(levelname)-7s %(name)s - %(message)s",
    )
    return run(parse_args(argv))


if __name__ == "__main__":
    raise SystemExit(main())
