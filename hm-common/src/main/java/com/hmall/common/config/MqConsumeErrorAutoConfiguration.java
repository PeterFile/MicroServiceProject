package com.hmall.common.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.rabbitmq.listener.simple.retry.enabled", havingValue = "true")
public class MqConsumeErrorAutoConfiguration {

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public DirectExchange errorExchange() {
        return ExchangeBuilder
                .directExchange("error.direct")
                .durable(true)
                .build();
    }

    @Bean
    public Queue errorQueue() {
        return new Queue(applicationName + "error.queue");
    }

    @Bean
    public Binding errorBingQueue(@Qualifier("errorQueue") Queue queue, @Qualifier("errorExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(applicationName);
    }

    @Bean
    @ConditionalOnBean(RabbitTemplate.class)
    public MessageRecoverer RePublishMessageRecover(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, "error.direct", applicationName);
    }
}
