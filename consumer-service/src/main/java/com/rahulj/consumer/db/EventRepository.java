
 package com.rahulj.consumer.db;

import java.sql.*;

public class EventRepository {
  private final String url;
  private final String user;
  private final String pass;

  public EventRepository(String url, String user, String pass) {
    this.url = url;
    this.user = user;
    this.pass = pass;
  }

  public void insertIdempotent(String eventId, String imsi, String payloadJson) throws SQLException {
    try (Connection c = DriverManager.getConnection(url, user, pass)) {
      try (PreparedStatement ps = c.prepareStatement(
        "INSERT INTO events(event_id, imsi, payload_json) VALUES (?, ?, ?) ON CONFLICT (event_id) DO NOTHING"
      )) {
        ps.setString(1, eventId);
        ps.setString(2, imsi);
        ps.setString(3, payloadJson);
        ps.executeUpdate();
      }
    }
  }
}
