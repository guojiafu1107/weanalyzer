package com.weanalyzer.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic articleAnalysisTopic() {
        return TopicBuilder.name("article-analysis")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic batchAnalysisTopic() {
        return TopicBuilder.name("batch-analysis")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
