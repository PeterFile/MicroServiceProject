package com.hmall.search.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmall.common.domain.PageDTO;
import com.hmall.search.repository.dto.ItemDTO;
import com.hmall.search.repository.po.Item;
import com.hmall.search.repository.query.ItemPageQuery;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 商品表 服务类
 * </p>
 *
 * @author 虎哥
 * @since 2023-05-05
 */
public interface IItemService extends IService<Item> {

    List<ItemDTO> queryItemByIds(Collection<Long> ids);

    PageDTO<ItemDTO> searchItems(ItemPageQuery query);

    Map<String, List<String>> filtersBool(ItemPageQuery query) throws IOException;
}
