package com.hmall.search.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmall.common.domain.PageDTO;
import com.hmall.common.utils.BeanUtils;
import com.hmall.search.mapper.ItemMapper;
import com.hmall.search.repository.dto.ItemDTO;
import com.hmall.search.repository.po.Item;
import com.hmall.search.repository.po.ItemDoc;
import com.hmall.search.repository.query.ItemPageQuery;
import com.hmall.search.service.IItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 商品表 服务实现类
 * </p>
 *
 * @author 虎哥
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ItemServiceImpl extends ServiceImpl<ItemMapper, Item> implements IItemService {

    private final RestHighLevelClient restHighLevelClient;
    private final ObjectMapper objectMapper;

    @Override
    public List<ItemDTO> queryItemByIds(Collection<Long> ids) {
        return BeanUtils.copyList(listByIds(ids), ItemDTO.class);
    }

    @Override
    public PageDTO<ItemDTO> searchItems(ItemPageQuery query) {
        log.info("传回参数：{}", query);
        // 1.构建查询条件
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        if (StrUtil.isNotBlank(query.getKey())) {
            boolQuery.must(QueryBuilders.matchQuery("name", query.getKey()));
        }
        if (StrUtil.isNotBlank(query.getBrand())) {
            boolQuery.filter(QueryBuilders.termQuery("brand", query.getBrand()));
        }
        if (StrUtil.isNotBlank(query.getCategory())) {
            boolQuery.filter(QueryBuilders.termQuery("category", query.getCategory()));
        }

        if (query.getMinPrice() != null && query.getMaxPrice() != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("price").gte(query.getMinPrice()).lte(query.getMaxPrice()));
        } else if (query.getMinPrice() != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("price").gte(query.getMinPrice()));
        } else if (query.getMaxPrice() != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("price").lte(query.getMaxPrice()));
        }

        // 2.创建分页查询请求
        SearchRequest searchRequest = new SearchRequest("items");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(boolQuery);
        // 页码
        sourceBuilder.from((query.getPageNo() - 1) * query.getPageSize());
        sourceBuilder.size(query.getPageSize());
        searchRequest.source(sourceBuilder);

        log.info("构建的查询条件：{}", searchRequest);
//        searchRequest.source().query(boolQuery);
//        searchRequest.source().from((query.getPageNo() - 1) * query.getPageSize());
//        searchRequest.source().size(query.getPageSize());
        SearchResponse response = null;

        try {
            response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException("Elasticsearch query fail", e);
        }

        log.info("获取到的返回值: {}", response);
        List<ItemDTO> itemDTOS = new ArrayList<>();
        for (SearchHit hit : response.getHits()) {
            try {
                ItemDoc itemDoc = JSONUtil.toBean(hit.getSourceAsString(), ItemDoc.class);
                itemDTOS.add(new ItemDTO(itemDoc));
            } catch (Exception e) {
                throw new RuntimeException("Fail to parse Elasticsearch hit", e);
            }
        }

        PageDTO<ItemDTO> pageDTO = new PageDTO<>();
        pageDTO.setList(itemDTOS);
        pageDTO.setTotal(response.getHits().getTotalHits().value);
        return pageDTO;
    }
}
