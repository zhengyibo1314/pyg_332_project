package cn.itcast.core.service.template;

import cn.itcast.core.dao.specification.SpecificationOptionDao;
import cn.itcast.core.dao.template.TypeTemplateDao;
import cn.itcast.core.entity.PageResult;
import cn.itcast.core.pojo.specification.SpecificationOption;
import cn.itcast.core.pojo.specification.SpecificationOptionQuery;
import cn.itcast.core.pojo.template.TypeTemplate;
import cn.itcast.core.pojo.template.TypeTemplateQuery;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.PageHelper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Service
public class TypeTemplateServiceimpl  implements TypeTemplateService{
    @Resource
    private TypeTemplateDao typeTemplateDao;
    @Resource
    private SpecificationOptionDao specificationOptionDao;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public PageResult search(Integer page, Integer rows, TypeTemplate typeTemplate) {

        // 点击列表查询的时候将数据写到缓存中
        List<TypeTemplate> list = typeTemplateDao.selectByExample(null);
        if(list != null && list.size() > 0){
            for (TypeTemplate template : list) {
                // 将品牌结果集放入缓存
                String brandIds = template.getBrandIds();
                List<Map> brandList = JSON.parseArray(brandIds, Map.class);
                redisTemplate.boundHashOps("brandList").put(template.getId(), brandList);
                // 将规格结果集放入缓存
                List<Map> specList = findBySpecList(template.getId());
                redisTemplate.boundHashOps("specList").put(template.getId(), specList);
            }
        }

        // 1、设置分页条件
        PageHelper.startPage(page, rows);
        // 2、设置查询条件
        TypeTemplateQuery typeTemplateQuery = new TypeTemplateQuery();
        if(typeTemplate.getName() != null && !"".equals(typeTemplate.getName().trim())){
            typeTemplateQuery.createCriteria().andNameLike("%" + typeTemplate.getName().trim() + "%");
        }
        // 3、根据条件查询
        Page<TypeTemplate> p = (Page<TypeTemplate>) typeTemplateDao.selectByExample(typeTemplateQuery);
        // 4、将结果封装到PageResult中
        return new PageResult(p.getResult(), p.getTotal());
    }
  @Transactional
    @Override
    public void add(TypeTemplate typeTemplate) {
        typeTemplateDao.insertSelective(typeTemplate);
    }

    @Override
    public TypeTemplate findOne(Long id) {
        return typeTemplateDao.selectByPrimaryKey(id);
    }

    @Override
    public void update(TypeTemplate typeTemplate) {
        typeTemplateDao.updateByPrimaryKey(typeTemplate);
    }

    @Override
    public void delete(Long[] ids) {
        for (Long id : ids) {
            typeTemplateDao.deleteByPrimaryKey(id);
        }
    }
    /**
     * 新增模板时初始化规格列表
     * @return
     */

    @Override
    public List<Map> findBySpecList(Long id) {
        TypeTemplate typeTemplate = typeTemplateDao.selectByPrimaryKey(id);
        // specIds：json串
        // 栗子：[{"id":27,"text":"网络"},{"id":32,"text":"机身内存"}]
        String specIds = typeTemplate.getSpecIds();
        // 将json串转成对象：阿里fastjson
        List<Map> specList = JSON.parseArray(specIds, Map.class);//如果直接返回是吧规格返回但是返回只有text 规格名字 还有规格选项所以要以规格的id 去查规格选项
        // 通过规格获取到规格选项 这是tb_type_template里面的 soec_ids 这个是规格 里面的id 是对应的规格选项在tb_specification_option
        if(specList != null && specList.size() > 0){
            for (Map map : specList) {
                Long specId = Long.parseLong(map.get("id").toString());
                SpecificationOptionQuery optionQuery = new SpecificationOptionQuery();
                optionQuery.createCriteria().andSpecIdEqualTo(specId);
                List<SpecificationOption> options = specificationOptionDao.selectByExample(optionQuery);
                map.put("options", options);//这个就是我拿到啦所有的规格选项要返回页面因为map 这些都是从map中获取的所以要添加到map 中
                //因为前段规定好的所以我要"options" 对应的值就是一个集合规格选项 到前段在遍历 要封装的map 中一起传到页面
            }
        }
        // 最终specList：[{"id":27,"text":"网络","options":options},{"id":32,"text":"机身内存"}]
        return specList;
    }

}
