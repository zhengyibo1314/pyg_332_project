package cn.itcast.core.service.goods;

import cn.itcast.core.dao.good.BrandDao;
import cn.itcast.core.dao.good.GoodsDao;
import cn.itcast.core.dao.good.GoodsDescDao;
import cn.itcast.core.dao.item.ItemCatDao;
import cn.itcast.core.dao.item.ItemDao;
import cn.itcast.core.dao.seller.SellerDao;
import cn.itcast.core.entity.PageResult;
import cn.itcast.core.pojo.good.BrandQuery;
import cn.itcast.core.pojo.good.Goods;
import cn.itcast.core.pojo.good.GoodsDesc;
import cn.itcast.core.pojo.good.GoodsQuery;
import cn.itcast.core.pojo.item.Item;
import cn.itcast.core.pojo.item.ItemQuery;
import cn.itcast.core.vo.GoodsVo;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.jms.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class GoodsServiceImpl implements GoodsService {

    @Resource
    private GoodsDao goodsDao;

    @Resource
    private GoodsDescDao goodsDescDao;

    @Resource
    private ItemDao itemDao;
    @Resource
    private ItemCatDao itemCatDao;

    @Resource
    private BrandDao brandDao;

    @Resource
    private SellerDao sellerDao;
    @Resource
    private SolrTemplate solrTemplate;
    // 要把商品id 放到mq中 操作 mq
    @Resource
    private JmsTemplate jmsTemplate;
    @Resource
    private Destination topicPageAndSolrDestination;
    @Resource
    private Destination queueSolrDeleteDestination;
    /**
     * 保存商品
     * @param goodsVo
     */
    @Transactional
    @Override
    public void add(GoodsVo goodsVo) {
        // 保存商品信息
        Goods goods = goodsVo.getGoods();
        goods.setAuditStatus("0");  // 商品待审核的状态
        goodsDao.insertSelective(goods);    // 返回自增主键id
        // 保存商品详细信息
        GoodsDesc goodsDesc = goodsVo.getGoodsDesc();
        goodsDesc.setGoodsId(goods.getId());
        goodsDescDao.insertSelective(goodsDesc);
        // 保存商品对应的库存信息
        //是否启用规格
        if ("1".equals(goods.getIsEnableSpec())){
            //启用规格 一个spu 对应的是 多个sku
            List<Item> itemList = goodsVo.getItemList();
            if (itemList!=null&&itemList.size()>0){
                for (Item item : itemList) {
                    //商品的标题 spu + spu的 副标题   + 还没有规格名称 规格名称
                String tite=goods.getGoodsName()+""+ goods.getCaption();
                //查询打断点   查看数据中的测试数据
                    //spec{"机身内存:16g,网络:3g}
                    String spec = item.getSpec();
                    Map<String,String> specMap = JSON.parseObject(spec, Map.class);
                    Set<Map.Entry<String, String>> entries = specMap.entrySet();
                    for (Map.Entry<String, String> entry : entries) {
                        tite+=""+entry.getValue();
                    }
                   item.setTitle(tite);
                    setItemAttribute(goods, goodsDesc, item);
                    itemDao.insertSelective(item);
                }
            }else {
                // 不启用规格：一个spu对应一个sku
                Item item = new Item();
                item.setTitle(goods.getGoodsName() + " " + goods.getCaption()); // 标题
                item.setPrice(goods.getPrice());    // 商品价格
                item.setStatus("1");    // 是否启用该商品
                item.setNum(9999);      // 库存量
                item.setIsDefault("1"); // 是否默认
                item.setSpec("{}");
                setItemAttribute(goods, goodsDesc, item);
                itemDao.insertSelective(item);
            }

        }

    }
 //商品系统下的 列表查询
    @Override
    public PageResult searchForshop(Integer page, Integer rows, Goods goods) {
        // 分页条件
        PageHelper.startPage(page, rows);
        // 查询条件：当前商家id
        GoodsQuery goodsQuery = new GoodsQuery();
        GoodsQuery.Criteria criteria = goodsQuery.createCriteria();

        if(goods.getGoodsName() != null && !"".equals(goods.getGoodsName().trim())){
            criteria.andGoodsNameLike("%" + goods.getGoodsName().trim() + "%");
        }
        criteria.andSellerIdEqualTo(goods.getSellerId());
        // 根据条件查询
        Page<Goods> p = (Page<Goods>) goodsDao.selectByExample(goodsQuery);
        return new PageResult(p.getResult(), p.getTotal());
    }

    @Override
    public GoodsVo fidOne(Long id) {
        GoodsVo goodsVo = new GoodsVo();
        //商品信息
        Goods goods = goodsDao.selectByPrimaryKey(id);
        goodsVo.setGoods(goods);
        //商品详细
        GoodsDesc goodsDesc = goodsDescDao.selectByPrimaryKey(id);
         goodsVo.setGoodsDesc(goodsDesc);
     //库存信息
        ItemQuery itemQuery = new ItemQuery();
        itemQuery.createCriteria().andGoodsIdEqualTo(id);
        List<Item> items = itemDao.selectByExample(itemQuery);
       goodsVo.setItemList(items);
       return goodsVo;

    }
    @Transactional
    @Override
    public void update(GoodsVo goodsVo) {
        //更新商品
        Goods goods = goodsVo.getGoods();
        goods.setAuditStatus("0");
        goodsDao.updateByPrimaryKeySelective(goods);
        //更新商品详情
        GoodsDesc goodsDesc = goodsVo.getGoodsDesc();
        goodsDescDao.updateByPrimaryKeySelective(goodsDesc);
        //更新商品对应的库存
        //先删除
        ItemQuery itemQuery = new ItemQuery();
        itemQuery.createCriteria().andGoodsIdEqualTo(goods.getId());
        itemDao.deleteByExample(itemQuery);
        //再插入
        if ("1".equals(goods.getIsEnableSpec())){
            //启用规格 一个spu 对应的是 多个sku
            List<Item> itemList = goodsVo.getItemList();
            if (itemList!=null&&itemList.size()>0){
                for (Item item : itemList) {
                    //商品的标题 spu + spu的 副标题   + 还没有规格名称 规格名称
                    String tite=goods.getGoodsName()+""+ goods.getCaption();
                    //查询打断点   查看数据中的测试数据
                    //spec{"机身内存:16g,网络:3g}
                    String spec = item.getSpec();
                    Map<String,String> specMap = JSON.parseObject(spec, Map.class);
                    Set<Map.Entry<String, String>> entries = specMap.entrySet();
                    for (Map.Entry<String, String> entry : entries) {
                        tite+=""+entry.getValue();
                    }
                    item.setTitle(tite);
                    setItemAttribute(goods, goodsDesc, item);
                    itemDao.insertSelective(item);
                }
            }else {
                // 不启用规格：一个spu对应一个sku
                Item item = new Item();
                item.setTitle(goods.getGoodsName() + " " + goods.getCaption()); // 标题
                item.setPrice(goods.getPrice());    // 商品价格
                item.setStatus("1");    // 是否启用该商品
                item.setNum(9999);      // 库存量
                item.setIsDefault("1"); // 是否默认
                item.setSpec("{}");
                setItemAttribute(goods, goodsDesc, item);
                itemDao.insertSelective(item);
            }

        }



    }
    /**
     * 运营系统查询待审核的商品列表
     * @param page
     * @param rows
     * @param goods
     * @return
     */
    @Override
    public PageResult searchForManager(Integer page, Integer rows, Goods goods) {
        // 设置分页条件
        PageHelper.startPage(page, rows);
        // 设置查询条件：待审核并且是未删除
        GoodsQuery goodsQuery = new GoodsQuery();
        GoodsQuery.Criteria criteria = goodsQuery.createCriteria();
        if(goods.getAuditStatus() != null && !"".equals(goods.getAuditStatus().trim())){
            criteria.andAuditStatusEqualTo(goods.getAuditStatus().trim());
        }
        criteria.andIsDeleteIsNull();   // null：未删除   1：已删除  MySQL查询：建议is null
        goodsQuery.setOrderByClause("id desc");
        // 根据条件查询
        Page<Goods> p = (Page<Goods>) goodsDao.selectByExample(goodsQuery);
        // 封装结果
        return new PageResult(p.getResult(), p.getTotal());
    }
    /**
     * 审核商品
     * @param ids
     * @param status
     */

    @Override
    public void updateStatus(Long[] ids, String status) {
        if(ids != null && ids.length > 0){
            Goods goods = new Goods();
            goods.setAuditStatus(status);
            for (final Long id : ids) {
                goods.setId(id);
                // 1、更新商品的审核状态
                goodsDao.updateByPrimaryKeySelective(goods);
                // 如果审核通过
                if("1".equals(status)){
                    // 将商品进行上架
                    //今天把所有商品保存索引 为了搜索 以后更改
                    //dataImportToSolrForItem();
                    //  2、真正的实现将审核通过后的商品对应的库存
                  //  updateItemToSolr(id);
                    //  3、生成该商品详情的静态页面
                    //需要将商品id 发送到mq中(1参数为//这里面是放目的地,2是)
                    jmsTemplate.send(topicPageAndSolrDestination, new MessageCreator() {
                        @Override
                        public Message createMessage(Session session) throws JMSException {
                            //讲商品id封装到 消息体进行发送
                            TextMessage textMessage = session.createTextMessage(String.valueOf(id));
                            return textMessage;//把这个id 发送到 mq中
                        }
                    });
                }
            }
        }
    }
//将 商品的对应信息保存到索引库
    private void updateItemToSolr(Long id) {
        // 查询该商品对应的库存信息
        ItemQuery itemQuery = new ItemQuery();
        itemQuery.createCriteria().andGoodsIdEqualTo(id).andIsDefaultEqualTo("1").andStatusEqualTo("1"); //这里面有默认的就是价格最低的就是IsDefault这个字段意思是  我当我有多个商品 这个是默认显示的 还有 可用的
        List<Item> items = itemDao.selectByExample(itemQuery);
        if(items != null && items.size() > 0){
            // 处理动态字段
            for (Item item : items) {
                // 栗子：{"机身内存":"16G","网络":"联通3G"}
                String spec = item.getSpec();
                // 拼接的动态字段：item_spec_机身内存 、 item_spec_网络
                Map<String, String> specMap = JSON.parseObject(spec, Map.class);
                item.setSpecMap(specMap);
            }
            solrTemplate.saveBeans(items);
            solrTemplate.commit();
        }
    }

    /**
     * 将全部库存的数据保存到索引库中
     */

    private void dataImportToSolrForItem() {
        ItemQuery itemQuery = new ItemQuery();
        itemQuery.createCriteria().andStatusEqualTo("1"); // 将正常的库存信息保存到索引库中
        List<Item> items = itemDao.selectByExample(itemQuery);
        if(items != null && items.size() > 0){
            // 处理动态字段
            for (Item item : items) {
                // 栗子：{"机身内存":"16G","网络":"联通3G"}
                String spec = item.getSpec();
                // 拼接的动态字段：item_spec_机身内存 、 item_spec_网络
                Map<String, String> specMap = JSON.parseObject(spec, Map.class);
                item.setSpecMap(specMap);
            }
            solrTemplate.saveBeans(items);
            solrTemplate.commit();
        }
    }

    /**
     * 删除商品  //记住删除 只删除 状态为0的  你只要审核过 都唯一 没有啦 查询只查为0的
     * @param ids
     */
    @Override
    public void delete(Long[] ids) {
        if(ids != null && ids.length > 0){
            Goods goods = new Goods();
            goods.setIsDelete("1"); // 1：删除的状态
            for (final Long id : ids) {
                goods.setId(id);
                goodsDao.updateByPrimaryKeySelective(goods);
                //  2、将商品进行下架:将商品信息从索引中删除  要根据商品id  删除
//                SimpleQuery query = new SimpleQuery("item_goodsid:"+id);
//                solrTemplate.delete(query);
//                solrTemplate.commit();
                jmsTemplate.send(queueSolrDeleteDestination, new MessageCreator() {
                    @Override
                    public Message createMessage(Session session) throws JMSException {
                        //你得将消息封装到消息体里面
                        TextMessage textMessage = session.createTextMessage(String.valueOf(id));
                        return textMessage;
                    }
                });
                //3、删除静态页：可选
            }
        }
    }








    // 设置item的公共的属性
    private void setItemAttribute(Goods goods, GoodsDesc goodsDesc, Item item) {
        // 商品图片
        // 例子：
        // [{"color":"粉色","url":"http://192.168.25.133/group1/M00/00/00/wKgZhVmOXq2AFIs5AAgawLS1G5Y004.jpg"},
        // {"color":"黑色","url":"http://192.168.25.133/group1/M00/00/00/wKgZhVmOXrWAcIsOAAETwD7A1Is874.jpg"}]
        String itemImages = goodsDesc.getItemImages();
        List<Map> images = JSON.parseArray(itemImages, Map.class);
        if(images != null && images.size() > 0){
            String image = images.get(0).get("url").toString();
            item.setImage(image);
        }
        // 商品三级分类id
        item.setCategoryid(goods.getCategory3Id());
        item.setCreateTime(new Date());
        item.setUpdateTime(new Date());
        item.setGoodsId(goods.getId()); // 商品id
        item.setSellerId(goods.getSellerId());  // 商家id
        item.setCategory(itemCatDao.selectByPrimaryKey(goods.getCategory3Id()).getName()); // 分类名称
        item.setBrand(brandDao.selectByPrimaryKey(goods.getBrandId()).getName());    // 品牌名称
        item.setSeller(sellerDao.selectByPrimaryKey(goods.getSellerId()).getNickName());   // 商家店铺名称
    }
}
