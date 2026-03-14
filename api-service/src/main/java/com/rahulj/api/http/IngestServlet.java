 
 package com.rahulj.api.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rahulj.api.kafka.KafkaEventProducer;
import org.eclipse.jetty.http.HttpStatus;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public class IngestServlet extends HttpServlet {
  private final KafkaEventProducer producer;
  private final ObjectMapper om = new ObjectMapper();

  public IngestServlet(KafkaEventProducer producer) {
    this.producer = producer;
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    Map<String, Object> body = om.readValue(req.getInputStream(), Map.class);

    String eventId = (String) body.get("eventId");
    String imsi = (String) body.get("imsi");

    if (eventId == null || eventId.isBlank() || imsi == null || imsi.isBlank()) {
      resp.setStatus(HttpStatus.BAD_REQUEST_400);
      resp.getWriter().write("{\"ok\":false,\"error\":\"eventId and imsi are required\"}");
      return;
    }

    String json = om.writeValueAsString(body);

    // Key by IMSI => ordering per subscriber
    producer.send("events", imsi, json);

    resp.setStatus(HttpStatus.ACCEPTED_202);
    resp.getWriter().write("{\"ok\":true,\"topic\":\"events\"}");
  }
}
