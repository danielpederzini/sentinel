package org.pdzsoftware.riskactionhandler.infrastructure.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.pdzsoftware.riskactionhandler.infrastructure.config.properties.KafkaConsumerProperties;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.TransactionScoredMessage;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(KafkaConsumerProperties.class)
public class KafkaConsumerConfig {
	@Bean
	public ConsumerFactory<String, TransactionScoredMessage> transactionConsumerFactory(KafkaConsumerProperties kafkaConsumerProperties) {
		Map<String, Object> consumerConfigs = new HashMap<>();
		consumerConfigs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConsumerProperties.getBootstrapServers());
		consumerConfigs.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaConsumerProperties.getConsumerGroupId());
		consumerConfigs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, kafkaConsumerProperties.getAutoOffsetReset());
		consumerConfigs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

		JacksonJsonDeserializer<TransactionScoredMessage> valueDeserializer = new JacksonJsonDeserializer<>(TransactionScoredMessage.class);
		valueDeserializer.setUseTypeHeaders(false);

		return new DefaultKafkaConsumerFactory<>(
				consumerConfigs,
				new StringDeserializer(),
				valueDeserializer
		);
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, TransactionScoredMessage> kafkaListenerContainerFactory(
			ConsumerFactory<String, TransactionScoredMessage> transactionConsumerFactory) {
		ConcurrentKafkaListenerContainerFactory<String, TransactionScoredMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(transactionConsumerFactory);
		return factory;
	}
}
