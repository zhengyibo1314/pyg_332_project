package cn.itcast.core.service.spec;

import cn.itcast.core.entity.PageResult;
import cn.itcast.core.pojo.specification.Specification;
import cn.itcast.core.vo.SpecVo;

import java.util.List;
import java.util.Map;

public interface SpecService {
    //完成规格分页全查
    public PageResult search(Integer page, Integer rows, Specification specification);
    //新增规格
    public void add(SpecVo specVo);
    //规格回显
    public SpecVo findOne(Long id);
    //更新规格
    public void update(SpecVo specVo);
    //删除
    public void delete(Long[] ids);
    /**
     * 新增模板时初始化规格列表
     * @return
     */
    public List<Map<String, String>> selectOptionList();

}
