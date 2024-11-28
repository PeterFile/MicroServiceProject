package com.hmall.cart.listener;


import com.hmall.cart.constants.MQConstants;
import com.hmall.cart.service.impl.CartServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@RequiredArgsConstructor
public class OrderStatusListener {

    private final CartServiceImpl cartService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MQConstants.CART_QUEUE_NAME, durable = "true"),
            exchange = @Exchange(name = MQConstants.CART_EXCHANGE_NAME, type = ExchangeTypes.TOPIC),
            key = MQConstants.CART_ORDER_KEY
    ))
    public void listenerOrderGoPay(Collection<Long> itemIds) {
        if (itemIds != null && !itemIds.isEmpty()) {
            cartService.removeByItemIds(itemIds);
        }
    }
}
