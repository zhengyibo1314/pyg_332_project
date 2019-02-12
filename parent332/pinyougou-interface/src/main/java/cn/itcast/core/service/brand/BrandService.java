package cn.itcast.core.service.brand;

import cn.itcast.core.entity.PageResult;
import cn.itcast.core.pojo.good.Brand;

import java.util.List;
import java.util.Map;

public interface BrandService {

    /**
     * 查询所有品牌
     * @return
     */
    public List<Brand> findAll();


    /**
     * 品牌列表分页查询
     * @param pageNo
     * @param pageSize
     * @return
     */
    public PageResult findPage(Integer pageNo, Integer pageSize);

    /**
     * 品牌列表条件查询
     * @param pageNo
     * @param pageSize
     * @param brand
     * @return
     */
    public PageResult search(Integer pageNo, Integer pageSize, Brand brand);


    public void add(Brand brand);

    //品牌回显
    public Brand findOne(Long id);

   //更新操作
    public void update(Brand brand);
    //批量删除
    public void delete(Long[] ids);
   //新增规格值时始化品牌列表
     public List<Map<String,String>>selectOptionList();

}
