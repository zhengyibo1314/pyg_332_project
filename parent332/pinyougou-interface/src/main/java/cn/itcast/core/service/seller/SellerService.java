package cn.itcast.core.service.seller;

import cn.itcast.core.entity.PageResult;
import cn.itcast.core.pojo.seller.Seller;

public interface SellerService {

    /**
     * 商家入驻申请
     * @param seller
     */
    public void add(Seller seller);

    /**
     * 待审核的商家的列表查询
     * @param page
     * @param rows
     * @param seller
     * @return
     */
    public PageResult search(Integer page, Integer rows, Seller seller);

    /**
     * 商家详情
     * @param sellerId
     * @return
     */
    public Seller findOne(String sellerId);

    /**
     * 审核商家
     * @param sellerId //商家名字 和商家状态  这都是前端的传过来的参数
     * @param status
     */
    public void updateStatus(String sellerId, String status);
}
