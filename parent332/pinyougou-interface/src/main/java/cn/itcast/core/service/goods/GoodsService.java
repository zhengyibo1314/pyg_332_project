package cn.itcast.core.service.goods;

import cn.itcast.core.entity.PageResult;
import cn.itcast.core.pojo.good.Goods;
import cn.itcast.core.vo.GoodsVo;

public interface GoodsService {

    /**
     * 保存商品
     * @param goodsVo
     */
    public void add(GoodsVo goodsVo);
   //商家系统下的商品列表查询
    public PageResult searchForshop(Integer page, Integer rows, Goods goods);
    //商品回显
    public GoodsVo fidOne(Long id);
    //商品修改更新商品
    public void update(GoodsVo goodsVo);
    /**
     * 运营系统查询待审核的商品列表
     * @param page
     * @param rows
     * @param goods
     * @return
     */
    public PageResult searchForManager(Integer page, Integer rows, Goods goods);

    /**
     * 审核商品
     * @param ids
     * @param status
     */
    public void updateStatus(Long[] ids, String status);

    /**
     * 删除商品
     * @param ids
     */
    public void delete(Long[] ids);
}
