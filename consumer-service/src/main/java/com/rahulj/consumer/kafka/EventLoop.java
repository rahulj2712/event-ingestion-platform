
 package com.rahulj.consumer.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rahulj.consumer.db.EventRepository;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.*;

public class EventLoop {
  private final ObjectMapper om = new ObjectMapper();
  private final KafkaConsumer<String, String> consumer;
  private final EventRepository repo;
  private final DlqProducer dlq;

  public EventLoop(String bootstrap, EventRepository repo, DlqProducer dlq) {
    this.repo = repo;
    this.dlq = dlq;

    Properties props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "events-consumer");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    this.consumer = new KafkaConsumer<>(props);
  }

  public void run() {
    consumer.subscribe(List.of("events"));

    while (true) {
      ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
      for (ConsumerRecord<String, String> r : records) {
        handleRecord(r);
      }
    }
  }

  private void handleRecord(ConsumerRecord<String, String> r) {
    String key = r.key();
    String value = r.value();

    int attempts = 0;
    while (true) {
      attempts++;
      try {
        JsonNode node = om.readTree(value);
        String eventId = node.get("eventId").asText();
        String imsi = node.get("imsi").asText();

        repo.insertIdempotent(eventId, imsi, value);

        // Commit offset AFTER successful DB write => at-least-once
        TopicPartition tp = new TopicPartition(r.topic(), r.partition());
        OffsetAndMetadata omd = new OffsetAndMetadata(r.offset() + 1);
        consumer.commitSync(Map.of(tp, omd));
        return;
      } catch (Exception e) {
        if (attempts >= 3) {
          dlq.send("events.dlq", key, value);

          // Ack/commit even for DLQ to avoid blocking partition
          TopicPartition tp = new TopicPartition(r.topic(), r.partition());
          OffsetAndMetadata omd = new OffsetAndMetadata(r.offset() + 1);
          consumer.commitSync(Map.of(tp, omd));
          return;
        }
        try {
          Thread.sleep(200L * attempts);
        } catch (InterruptedException ignored) {}
      }
    }
  }
}
