package cn.itcast.core.service.cart;

import cn.itcast.core.pojo.cart.Cart;
import cn.itcast.core.pojo.item.Item;

import java.util.List;

public interface CartService {

    /**
     * 根据库存id获取到商家id
     * @param id
     * @return
     */
    public Item findOne(Long id);

    /**
     * 填充购物车中的数据
     * @param cartList
     * @return
     */
    List<Cart> autoDataToCart(List<Cart> cartList);

 //这个是讲我们的本地我购物车同步到redis 中
    void saveCartToRedis(String username, List<Cart> newCartList);
//是从redis 中获取
    List<Cart> findCartListFromRedis(String username);
}
