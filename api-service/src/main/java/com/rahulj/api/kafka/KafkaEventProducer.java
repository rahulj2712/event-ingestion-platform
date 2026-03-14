
 package com.rahulj.api.kafka;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class KafkaEventProducer {
  private final KafkaProducer<String, String> producer;

  public KafkaEventProducer(String bootstrapServers) {
    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ProducerConfig.ACKS_CONFIG, "all");
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    this.producer = new KafkaProducer<>(props);
  }

  public void send(String topic, String key, String value) {
    producer.send(new ProducerRecord<>(topic, key, value), (md, ex) -> {
      if (ex != null) ex.printStackTrace();
    });
  }
}
