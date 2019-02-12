package cn.itcast.core.service.itemcat;

import cn.itcast.core.pojo.item.ItemCat;

import java.util.List;

public interface ItemCatService {

    /**
     *  根据上级ID查询商品分类
     * 商品分类的列表查询 那个上下级
     * @param parentId
     * @return
     */
    public List<ItemCat> findByParentId(Long parentId);

    /**
     * 增加
     */
    public void add(ItemCat itemCat);
    //模板id
    ItemCat findOne(Long id);

    //查询所有分类 并显示分类名字
    public List<ItemCat> findAll();



}
