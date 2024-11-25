package com.hmall.trade.listener;


import com.hmall.trade.repository.po.Order;
import com.hmall.trade.service.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PayStatusListener {

    private final IOrderService orderService;

//    @RabbitListener(bindings = @QueueBinding(
//            value = @Queue(name = "trade.pay.success.queue", durable = "true"),
//            exchange = @Exchange(name = "pay.direct"),
//            key = "pay.success"
//    ))
    public void listenPaySuccess(Long orderId) {
        // 1. 根据订单id查询订单
        Order order= orderService.getById(orderId);
        // 2. 判断订单状态是否为未支付
        if (order == null || order.getStatus() != 1) {
            return;
        }
        // 3. 标记订单状态为已支付
        orderService.markOrderPaySuccess(orderId);
    }
}
