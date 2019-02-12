package cn.itcast.core.service.search;

import cn.itcast.core.dao.item.ItemDao;
import cn.itcast.core.pojo.item.Item;
import cn.itcast.core.pojo.item.ItemQuery;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.*;

import javax.annotation.Resource;
import java.util.*;

@Service
public class ItemSearchServiceImpl implements ItemSearchService {

    @Resource
    private SolrTemplate solrTemplate;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private ItemDao itemDao;

    /**
     * 前台系统的检索
     * @param searchMap
     * @return
     */
    @Override
    public Map<String, Object> search(Map<String, String> searchMap) {
     //这个是去空格
        String keywords = searchMap.get("keywords");
        if(keywords != null && !"".equals(keywords)){
            keywords = keywords.replace(" ", "");
            searchMap.put("keywords", keywords);
        }
        //封装所有的结果集就是一个大map
        Map<String, Object> resultMap = new HashMap<>();
        //1根据关键字检索并且进行分页
        //  Map<String, Object> map=searchForPage(searchMap);
        // 1、根据关键字检索并且进行分页 并且检索的内容高亮
        Map<String, Object> map = searchForHighlightForPage(searchMap);
        resultMap.putAll(map);
        //2.获取商品的分类列表
        List<String> categoryList=searchForGroupPage(searchMap); //分类名称集合

        if (categoryList !=null && categoryList.size()>0){
            resultMap.put("categoryList",categoryList);
            // 3、获取商品的品牌、规格列表
            Map<String, Object> brandsAndSpecsMap = searchBrandsAndSpecsByCategoryName(categoryList.get(0));//分类列表的第一个这里就是通过分类就能获取品牌和规格列表 这是传的分类名称
            resultMap.putAll(brandsAndSpecsMap);
        }


        return resultMap;
    }
    /**
     * 将商品信息保存到索引库中
     * @param id
     */

    @Override
    public void updateItemToSolr(Long id) {
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
//从索引库中删除
    @Override
    public void deleteItemFromSolr(Long id) {
          SimpleQuery query = new SimpleQuery("item_goodsid:"+id);
             solrTemplate.delete(query);
             solrTemplate.commit();
    }

    // 获取品牌结果集以及规格结果集   ?
    private Map<String,Object> searchBrandsAndSpecsByCategoryName(String categoryName) {
        Map<String,Object> map = new HashMap<>();
        // 通过分类获取模板的id
      //  redisTemplate.boundHashOps("itemCat").put(itemCat.getName(), itemCat.getTypeId());
        Object typeId = redisTemplate.boundHashOps("itemCat").get(categoryName);
        // 通过模板id获取品牌结果集、规格结果集
        List<Map> brandList = (List<Map>) redisTemplate.boundHashOps("brandList").get(typeId);
        map.put("brandList", brandList);
        // 通过模板id获取品牌结果集、规格结果集
        List<Map> specList = (List<Map>) redisTemplate.boundHashOps("specList").get(typeId);
        map.put("specList", specList);
        return map;
    }

    //获取商品分类(分组)
    private List<String> searchForGroupPage(Map<String, String> searchMap) {
        //同样设置检索的条件
        Criteria criteria = new Criteria("item_keywords");   //根据哪个字段检索
        String keywords = searchMap.get("keywords");
        if(keywords != null && !"".equals(keywords)){ //这里面说的是 看item_keywords过来的所有值中criteria中包含keywords(比如手机)
            criteria.is(keywords);
        }
        SimpleQuery query=new SimpleQuery(criteria);
        //设置分组条件  group by field
        GroupOptions groupOptions=new GroupOptions();
        groupOptions.addGroupByField("item_category");//根据分类  分组
        query.setGroupOptions(groupOptions);
        List<String>categoryList=new ArrayList<>();//封装到分组结果
        //根据条件检索
        GroupPage<Item> groupPage = solrTemplate.queryForGroupPage(query, Item.class);
        //这时候你要考虑我查出得封装到list 中返回
        GroupResult<Item> groupResult = groupPage.getGroupResult("item_category");//我要拿到那个字段下的分组结果
        Page<GroupEntry<Item>> groupEntries = groupResult.getGroupEntries();
        for (GroupEntry<Item> groupEntry : groupEntries) {
            String value = groupEntry.getGroupValue();//在这里就是打断点groupResult.getGroupEntries能拿到数据然后遍历获取value
            categoryList.add(value);
        }
        return categoryList;//返回的所有的分类名称

    }

    // 检索的关键字高亮
    private Map<String,Object> searchForHighlightForPage(Map<String, String> searchMap) {
        // 1、封装检索的关键字
        Criteria criteria = new Criteria("item_keywords"); // 根据哪个字段检索
        String keywords = searchMap.get("keywords");
        if(keywords != null && !"".equals(keywords)){
            criteria.is(keywords);
        }

        SimpleHighlightQuery query = new SimpleHighlightQuery(criteria); //这个是借口实现类 都属于query
        // 2、封装分页条件
        Integer pageNo = Integer.valueOf(searchMap.get("pageNo"));
        Integer pageSize = Integer.valueOf(searchMap.get("pageSize"));
        Integer offset = (pageNo - 1) * pageSize;
        query.setOffset(offset);  // 其始行 //条件封装
        query.setRows(pageSize);    // 每页显示的条数
        // 3、封装高亮条件
        HighlightOptions highlightOptions = new HighlightOptions();
        highlightOptions.setSimplePrefix("<font color='red'>"); // 开始标签
        highlightOptions.setSimplePostfix("</font>");           // 结束标签
        highlightOptions.addField("item_title");                // 对该字段中包含的关键字高亮 标题 后端的
        query.setHighlightOptions(highlightOptions); //条件封装

        //根据条件过滤
        if (searchMap.get("category")!=null&&!"".equals(searchMap.get("category"))){
            Criteria cri = new Criteria("item_category");
            cri.is(searchMap.get("category"));
            FilterQuery filterQuery = new SimpleFilterQuery(cri);
            query.addFilterQuery(filterQuery);

        }
        // 根据品牌过滤
        if(searchMap.get("brand") != null && !"".equals(searchMap.get("brand"))){
            Criteria cri = new Criteria("item_brand");
            cri.is(searchMap.get("brand"));
            FilterQuery filterQuery = new SimpleFilterQuery(cri);
            query.addFilterQuery(filterQuery);
        }
        // 根据价格过滤
        if(searchMap.get("price") != null && !"".equals(searchMap.get("price"))){
            String[] prices = searchMap.get("price").split("-"); //获取前端传过来的价格用-隔开
            Criteria cri = new Criteria("item_price"); //这个是要以那个字段检索 如果有就给我查出来
            if(searchMap.get("price").contains("*")){ // xxx以上
                cri.greaterThan(prices[0]);
            }else{ // 区间段
                cri.between(prices[0], prices[1], true, true);//
            }
            FilterQuery filterQuery = new SimpleFilterQuery(cri);
            query.addFilterQuery(filterQuery);
        }
        // 根据商品的规格过滤
        // {"内存大小":"16G","网络":"联通4G"}
        String spec = searchMap.get("spec");
        if(spec != null && !"".equals(spec)){
            // item_spec_*：需要拼接动态字段
            Map<String, String> specMap = JSON.parseObject(spec, Map.class);
            Set<Map.Entry<String, String>> entries = specMap.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                Criteria cri = new Criteria("item_spec_" + entry.getKey());
                cri.is(entry.getValue());
                SimpleFilterQuery filterQuery = new SimpleFilterQuery(cri);
                query.addFilterQuery(filterQuery);
            }
        }
        // 5、根据商品价格以及新品进行排序
        // sortField：排序字段 ,sort：排序规则
        String sort = searchMap.get("sort");//先取出规则
        if(sort != null && !"".equals(sort)){
            if("DESC".equals(sort)){//这里面是根据页面传过来的 如果是DESC两个参数一个排序规则一个字段
                // 降序
                Sort s = new Sort(Sort.Direction.DESC, "item_" + searchMap.get("sortField"));//这里面就是如果穿过来的是Desc 就是降序 然后里面传参就是一个规则一个封装到这个字段根据那个字段排序
                query.addSort(s);
            }else{
                Sort s = new Sort(Sort.Direction.ASC, "item_" + searchMap.get("sortField"));
                query.addSort(s);
            }
        }

        // 根据条件检索
        HighlightPage<Item> highlightPage = solrTemplate.queryForHighlightPage(query, Item.class);
        // 获取高亮的结果重新赋值给item的title属性
        List<HighlightEntry<Item>> highlighted = highlightPage.getHighlighted(); //从新获取赋值 highlightPage现在没有高亮 高亮现在 在这里面highlighted所以说要获取highlighted
        if(highlighted != null && highlighted.size() > 0){
            for (HighlightEntry<Item> highlightEntry : highlighted) {
                Item item = highlightEntry.getEntity();// 普通结果 这个是先拿到普通结果
                List<HighlightEntry.Highlight> highlights = highlightEntry.getHighlights();//Highlights这个是高亮的
                if(highlights != null && highlights.size() > 0){
                    String title = highlights.get(0).getSnipplets().get(0); // 高亮结果这里就是拿到高亮遍历 获取highlights第一个元素中的Snipplets第一个元素
                    item.setTitle(title);//给标题直接赋值
                }
            }
        }
        Map<String,Object> map = new HashMap<>();
        map.put("totalPages", highlightPage.getTotalPages());  // 总页数
        map.put("total", highlightPage.getTotalElements());    // 总条数
        map.put("rows", highlightPage.getContent());           // 结果集
        return map;
    }

    //根据关键字检索并且进行分页
    private Map<String,Object> searchForPage(Map<String, String> searchMap) {
        //1封装检索的关键字
        Criteria criteria=new Criteria("item_keywords");// 根据哪个字段检索 keywords是关键字
        String keywords = searchMap.get("keywords");
        if(keywords != null && !"".equals(keywords)){
            criteria.is(keywords);//说规定好这个item_keywords字段啦 对象中是keywords关键字的去查出来
        }
        SimpleQuery query = new SimpleQuery(criteria);
        // 2、封装分页条件
        Integer pageNo = Integer.valueOf(searchMap.get("pageNo"));//这个是当前页
        Integer pageSize = Integer.valueOf(searchMap.get("pageSize"));//每页显示条数
        Integer offset = (pageNo - 1) * pageSize;//从哪里开始查
        query.setOffset(offset);  // 其始行
        query.setRows(pageSize);    // 每页显示的条数

        // 3、根据条件检索
        ScoredPage<Item> scoredPage = solrTemplate.queryForPage(query, Item.class);//这个就是我把条件都封装好穿到这里去索引库里查 返回对象是Item 然后就把返回结果封装map
        //这时候就看页面需要啥 最后返回结果集 就是符合条件的 都会封装到"rows", scoredPage.getContent()
        //讲返回来数据封装到返回对象的map中
        Map<String,Object> map = new HashMap<>();
        map.put("totalPages", scoredPage.getTotalPages());  // 总页数
        map.put("total", scoredPage.getTotalElements());    // 总条数
        map.put("rows", scoredPage.getContent());           // 结果集
        return map;
    }

}
