
 package com.rahulj.api;

import com.rahulj.api.http.IngestServlet;
import com.rahulj.api.kafka.KafkaEventProducer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

public class ApiMain {
  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
    String bootstrap = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");

    KafkaEventProducer producer = new KafkaEventProducer(bootstrap);

    Server server = new Server(port);
    ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
    ctx.setContextPath("/");
    ctx.addServlet(new org.eclipse.jetty.servlet.ServletHolder(new IngestServlet(producer)), "/events");

    server.setHandler(ctx);
    server.start();
    server.join();
  }
}
