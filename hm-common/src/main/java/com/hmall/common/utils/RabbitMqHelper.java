package com.hmall.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.UUID;

@Slf4j
public class RabbitMqHelper {
    private final RabbitTemplate rabbitTemplate;

    public RabbitMqHelper(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendMessage(String exchange, String routingKey, Object msg) {
        rabbitTemplate.convertAndSend(exchange, routingKey, msg);
    }

    public void sendDelayMessage(String exchange, String routingKey, Object msg, int delay) {
        rabbitTemplate.convertAndSend(exchange, routingKey, msg, message -> {
            message.getMessageProperties().setDelay(delay);
            return message;
        });
    }

    public void sendMessageWithConfirm(String exchange, String routingKey, Object msg, int maxRetries) {
        CorrelationData cd = new CorrelationData(UUID.randomUUID().toString());
        cd.getFuture().addCallback(
                new ListenableFutureCallback<CorrelationData.Confirm>() {
                    int retryCount;
                    @Override
                    public void onFailure(Throwable ex) {
                        log.error("处理 ack 回执失败", ex);
                    }

                    @Override
                    public void onSuccess(CorrelationData.Confirm result) {
                        if (result != null && !result.isAck()) {
                            log.error("发送消息失败，收到 nack, 已重试次数: {}", retryCount);
                            if (retryCount >= maxRetries) {
                                log.error("消息发送重试次数已用尽，发送失败");
                                return;
                            }
                            rabbitTemplate.convertAndSend(exchange, routingKey, msg, cd);
                            retryCount++;
                        }
                    }
                }
        );
        rabbitTemplate.convertAndSend(exchange, routingKey, msg, cd);
    }
}
