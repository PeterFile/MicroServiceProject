package com.hmall.item;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.item.repository.po.Item;
import com.hmall.item.repository.po.ItemDoc;
import com.hmall.item.service.IItemService;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

@SpringBootTest(properties = "spring.profiles.active=local")
public class ElasticDocumentTest {
    private RestHighLevelClient client;
    @Autowired
    private IItemService itemService;

    @Test
    void testConnection() {
        System.out.println("client = " + client);
    }

    @Test
    void testCreateIndexDoc() throws IOException {
        // 0. 准备文档数据
        Item item = itemService.getById(1017677L);
        ItemDoc itemDoc = BeanUtil.copyProperties(item, ItemDoc.class);
        // 1. 准备 Request 对象
        IndexRequest request = new IndexRequest("items").id(itemDoc.getId());
        // 2. 准备请求参数
        request.source(JSONUtil.toJsonStr(itemDoc), XContentType.JSON);
        // 3. 发送请求
        client.index(request, RequestOptions.DEFAULT);
    }

    @Test
    void testGetIndexDoc() throws IOException {
        // 1. 准备 Request 对象
        GetRequest request = new GetRequest("items", "1017677");
        // 2. 发送请求
        GetResponse response = client.get(request, RequestOptions.DEFAULT);
        String result = response.getSourceAsString();
        ItemDoc doc = JSONUtil.toBean(result, ItemDoc.class);
        System.out.println("doc = " + doc);
    }

    @Test
    void testUpdateIndexDoc() throws IOException {
        // 1. 准备 Request 对象
        UpdateRequest request = new UpdateRequest("items", "1017677");
        // 2. 发送请求
        request.doc("price", 25600);
        client.update(request, RequestOptions.DEFAULT);
    }

    @Test
    void testGetIndex() throws IOException {
        GetIndexRequest request = new GetIndexRequest("items");
        GetIndexResponse response = client.indices().get(request, RequestOptions.DEFAULT);
        System.out.println("返回结果:" + response.getDataStreams().toString());
    }

    @Test
    void testBulkDoc() throws IOException {
        int pageNo = 1, pageSize = 500;
        while (true) {
            Page<Item> page = itemService.lambdaQuery()
                    .eq(Item::getStatus, 1)
                    .page(Page.of(pageNo, pageSize));
            List<Item> records = page.getRecords();
            if (records == null || records.isEmpty()) return;
            BulkRequest request = new BulkRequest();
            // 2. 准备请求参数
            for (Item item : records) {
                request.add(new IndexRequest("items")
                        .id(item.getId().toString())
                        .source(JSONUtil.toJsonStr(BeanUtil.copyProperties(item, ItemDoc.class)), XContentType.JSON));
            }
            // 3. 发送请求
            client.bulk(request, RequestOptions.DEFAULT);
            pageNo++;
        }
    }

    @BeforeEach
    void setUp() {
        client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.85.129:9200")
        ));
    }

    @AfterEach
    void tearDown() throws IOException {
        if (client != null) {
            client.close();
        }
    }
}
