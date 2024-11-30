package com.hamll.search;

import com.hmall.common.domain.PageDTO;
import com.hmall.search.SearchApplication;
import com.hmall.search.repository.dto.ItemDTO;
import com.hmall.search.repository.query.ItemPageQuery;
import com.hmall.search.service.IItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = "spring.profiles.active=local", classes = SearchApplication.class)
class ItemServiceTest {

    @Autowired
    private IItemService itemService;

    @Test
    void testSearchItemsWithFilters() throws IOException {
        // 构造一个模拟的查询条件
        ItemPageQuery query = new ItemPageQuery();
        query.setKey("手机");
        query.setBrand("华为");
        query.setCategory("手机");
        query.setMinPrice(1000);
        query.setMaxPrice(5000);
        query.setPageNo(1);
        query.setPageSize(10);

        // 执行查询
        PageDTO<ItemDTO> result = itemService.searchItems(query);

        // 验证返回的结果
        assertNotNull(result);
//        assertEquals(1, result.getList().size());
//        assertEquals(1, result.getTotal());

        ItemDTO itemDTO = result.getList().get(0);
//        assertEquals("317578", itemDTO.getId());
        assertEquals("华为手机", itemDTO.getName());
    }


    @Test
    void testSearchItemsNoResults() throws IOException {
        // 设置查询条件
        ItemPageQuery query = new ItemPageQuery();
        query.setKey("未知商品");
        query.setPageNo(1);
        query.setPageSize(10);


        // 执行查询
        PageDTO<ItemDTO> result = itemService.searchItems(query);

        // 验证返回的结果
        assertNotNull(result);
        assertEquals(0, result.getList().size());
        assertEquals(0, result.getTotal());
    }

}
