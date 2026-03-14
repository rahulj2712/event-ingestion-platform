
 package com.rahulj.consumer;

import com.rahulj.consumer.db.EventRepository;
import com.rahulj.consumer.kafka.DlqProducer;
import com.rahulj.consumer.kafka.EventLoop;

public class ConsumerMain {
  public static void main(String[] args) throws Exception {
    String bootstrap = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");

    String jdbcUrl = System.getenv().getOrDefault("JDBC_URL", "jdbc:postgresql://localhost:5432/eventsdb");
    String jdbcUser = System.getenv().getOrDefault("JDBC_USER", "app");
    String jdbcPass = System.getenv().getOrDefault("JDBC_PASS", "app");

    EventRepository repo = new EventRepository(jdbcUrl, jdbcUser, jdbcPass);
    DlqProducer dlq = new DlqProducer(bootstrap);

    new EventLoop(bootstrap, repo, dlq).run();
  }
}
