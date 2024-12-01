package com.hmall.item.es;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.hmall.item.constants.MQConstants;
import com.hmall.item.repository.po.Item;
import com.hmall.item.repository.po.ItemDoc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class ItemConsumer {
    private final RestHighLevelClient restHighLevelClient;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MQConstants.SYNC_ES_QUEUE_NAME, durable = "true"),
            exchange = @Exchange(name = MQConstants.SYNC_ES_EXCHANGE_NAME),
            key = MQConstants.SYNC_ES_CREATE_KEY
    ))
    public void SyncItemListener(Item item) {
        ItemDoc itemDoc = BeanUtil.copyProperties(item, ItemDoc.class);
        try {
            syncItemToEs(itemDoc);
        } catch (IOException e) {
            log.error("Error processing message from MQ", e);
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MQConstants.SYNC_ES_QUEUE_NAME, durable = "true"),
            exchange = @Exchange(name = MQConstants.SYNC_ES_EXCHANGE_NAME),
            key = MQConstants.SYNC_ES_DELETE_KEY
    ))
    public void DeleteItemListener(Long id) {
        try {
            deleteItemFormEs(id);
        } catch (IOException e) {
            log.error("Error processing message from MQ", e);
        }
    }

    public void syncItemToEs(ItemDoc item) throws IOException {
        String doc = JSONUtil.toJsonStr(item);

        IndexRequest request = new IndexRequest("items").id(item.getId());
        request.source(doc, XContentType.JSON);
        restHighLevelClient.index(request, RequestOptions.DEFAULT);
    }

    public void deleteItemFormEs(Long id) throws IOException {
        DeleteRequest request = new DeleteRequest("items", id.toString());
        restHighLevelClient.delete(request, RequestOptions.DEFAULT);
    }
}
