package cn.itcast.core.service.pay;

import java.util.Map;

public interface PayService {

    /**
     * 生成支付的二维码
     * @return
     */
    public Map<String, String> createNative(String username) throws Exception;

    /**
     * 查询微信支付订单
     * @param out_trade_no
     * @return
     */
    public Map<String, String> queryPayStatus(String out_trade_no) throws Exception;
}
