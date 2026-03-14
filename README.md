
 # event-ingestion-platform

A clean-room demo of a production-style ingestion pattern:

- **API Service** exposes `POST /events` and publishes to **Kafka**
- **Consumer Service** processes events and writes to **Postgres**
- Processing is **at-least-once** (offset committed after DB write)
- Correctness is ensured with an **idempotent consumer** (unique `event_id` + `ON CONFLICT DO NOTHING`)
- Failures go through **retry with backoff**, then **DLQ** (`events.dlq`)
- Metrics exposed via Spring Boot Actuator (Prometheus format)

## Architecture
```mermaid
flowchart LR
  C[Client] -->|POST /events| API[api-service]
  API -->|produce: topic=events (key=imsi)| K[(Kafka)]
  K -->|consume (group=events-consumer)| CON[consumer-service]
  CON -->|idempotent upsert| PG[(Postgres)]
  CON -->|on permanent failure| DLQ[(Kafka topic: events.dlq)]
```

## Run locally
### Prerequisites
- Docker + Docker Compose

### Start everything
```bash
docker compose up --build
```

### Send a sample event
```bash
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{"eventId":"e1","imsi":"123456789012345","payload":{"type":"ULR","status":"ACK"},"ts":"2026-03-14T12:00:00Z"}'
```

### Metrics
- api-service: http://localhost:8080/actuator/prometheus
- consumer-service: http://localhost:8081/actuator/prometheus

## Design notes
### At-least-once + duplicates
We commit Kafka offsets **after** DB write. If the consumer crashes after writing but before committing, the same message can be re-delivered => duplicates.

We prevent duplicate side-effects by using:
- `event_id` as a primary key
- `INSERT ... ON CONFLICT DO NOTHING`

### DLQ
Poison messages (bad payloads) or repeated DB failures are sent to `events.dlq` so the main pipeline stays healthy.
