package com.hmall.item;

import cn.hutool.json.JSONUtil;
import com.hmall.common.utils.CollUtils;
import com.hmall.item.repository.po.ItemDoc;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

//@SpringBootTest(properties = "spring.profiles.active=local")
public class ElasticSearchTest {
    private RestHighLevelClient client;

    @Test
    void testMatchAll() throws IOException {
        // 1. 创建 request 对象
        SearchRequest request = new SearchRequest("items");
        // 2. 配置 request 对象
        request.source()
                .query(QueryBuilders.matchAllQuery());
        // 3. 发送请求
        SearchResponse search = client.search(request, RequestOptions.DEFAULT);
        // 4. 解析结果
        parseResponseResult(search);
    }

    @Test
    void testBoolAll() throws IOException {
        // 1. 创建 request 对象
        SearchRequest request = new SearchRequest("items");
        BoolQueryBuilder builder = new BoolQueryBuilder();
        // 2. 配置 request 对象
        request.source()
                .query(QueryBuilders.boolQuery()
                        .must(QueryBuilders.matchQuery("name", "脱脂牛奶"))
                        .filter(QueryBuilders.termQuery("brand", "德亚"))
                        .filter(QueryBuilders.rangeQuery("price").lt(30000)));
        // 3. 发送请求
        SearchResponse search = client.search(request, RequestOptions.DEFAULT);
        parseResponseResult(search);
    }

    @Test
    void testQuerySortAndPage() throws IOException {
        // 0. 模拟分页参数
        int pageNo = 2, pageSize = 5;
        // 1. 创建 request 对象
        SearchRequest request = new SearchRequest("items");
        // 2. 配置 request 对象
        request.source()
                .query(QueryBuilders.matchAllQuery());
        // 2.1 分页
        request.source().from((pageNo - 1) * pageSize).size(pageSize);
        request.source().sort("sold", SortOrder.DESC)
                .sort("price", SortOrder.ASC);
        // 3. 发送请求
        SearchResponse search = client.search(request, RequestOptions.DEFAULT);
        // 4. 解析结果
        parseResponseResult(search);
    }

    @Test
    void testQueryHighLight() throws IOException {
        // 1. 创建 request 对象
        SearchRequest request = new SearchRequest("items");
        // 2. 配置 request 对象
        request.source()
                .query(QueryBuilders.matchQuery("name", "脱脂牛奶"));
        request.source().highlighter(SearchSourceBuilder.highlight()
                .field("name"));
        // 3. 发送请求
        SearchResponse search = client.search(request, RequestOptions.DEFAULT);
        // 4. 解析结果
        parseResponseResult(search);
    }

    @Test
    void testQueryAgg() throws IOException {
        // 1. 创建 request 对象
        SearchRequest request = new SearchRequest("items");
        request.source().size(0);
        String brandAggName = "brandAgg";
        request.source().aggregation(
                AggregationBuilders.terms(brandAggName).field("brand").size(10)
        );
        // 3. 发送请求
        SearchResponse search = client.search(request, RequestOptions.DEFAULT);
        Aggregations aggregations = search.getAggregations();
        Terms aggregation = aggregations.get(brandAggName);
        List<? extends Terms.Bucket> buckets = aggregation.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            System.out.println("brand: " + bucket.getKeyAsString());
            System.out.println("count: " + bucket.getDocCount());
        }
    }

    private static void parseResponseResult(SearchResponse search) {
        // 解析结果
        SearchHits searchHits = search.getHits();
        //  总条数
        long total = searchHits.getTotalHits().value;
        System.out.println("total = " + total);
        // 命中的数据
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            // 获取 source 结果
            String source = hit.getSourceAsString();
            // 转为 ItemDoc
            ItemDoc doc = JSONUtil.toBean(source, ItemDoc.class);
            Map<String, HighlightField> hlfs = hit.getHighlightFields();
            if (CollUtils.isNotEmpty(hlfs)) {
                HighlightField hf = hlfs.get("name");
                String hfName = hf.getFragments()[0].string();
                doc.setName(hfName);
            }
            System.out.println("doc = " + doc);
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
