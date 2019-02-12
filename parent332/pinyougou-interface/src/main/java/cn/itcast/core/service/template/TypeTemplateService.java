package cn.itcast.core.service.template;

import cn.itcast.core.entity.PageResult;
import cn.itcast.core.pojo.template.TypeTemplate;

import java.util.List;
import java.util.Map;

public interface TypeTemplateService {
    //模块列表查询
    public PageResult search(Integer page, Integer rows, TypeTemplate typeTemplate);
    //添加模板
    public void add(TypeTemplate typeTemplate);


    /**
     * 通过模板加载对应的品牌以及扩展属性
     * @param id
     * @return
     */
    public TypeTemplate findOne(Long id);
    //修改
    public void update(TypeTemplate typeTemplate);
    //批量删除
    public void delete(Long[] ids);
    /**
     * 通过模板加载对应的规格以及规格选项
     * @param id
     * @return
     */
    public List<Map> findBySpecList(Long id);

}
