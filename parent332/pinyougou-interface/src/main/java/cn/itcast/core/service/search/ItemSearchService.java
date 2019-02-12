package cn.itcast.core.service.search;

import java.util.Map;

public interface ItemSearchService {

    /**
     * 前台系统的检索
     * @param searchMap
     * @return
     */
    public Map<String, Object> search(Map<String, String> searchMap);
    /**
     * 将商品信息保存到索引库中
     * @param id
     */
    public void updateItemToSolr(Long id);
    /**
     * 从索引库中删除商品
     * @param id
     */
    public void deleteItemFromSolr(Long id);
}
