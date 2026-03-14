CREATE TABLE IF NOT EXISTS events (
  event_id TEXT PRIMARY KEY,
  imsi TEXT NOT NULL,
  payload_json TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_events_imsi ON events(imsi); 
