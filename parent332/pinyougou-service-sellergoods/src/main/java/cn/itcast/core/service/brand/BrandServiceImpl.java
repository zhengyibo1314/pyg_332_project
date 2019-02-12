package cn.itcast.core.service.brand;

import cn.itcast.core.dao.good.BrandDao;
import cn.itcast.core.entity.PageResult;
import cn.itcast.core.pojo.good.Brand;
import cn.itcast.core.pojo.good.BrandQuery;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Service
public class BrandServiceImpl implements BrandService {

//    @Autowired
    // 好处：
    // 1、提高框架的性能
    // 2、降低与框架间的耦合度
    @Resource
    private BrandDao brandDao;

    /**
     * 查询所有品牌
     * @return
     */
    @Override
    public List<Brand> findAll() {
        //我要根据条件差 全查没有条件 那就是空
        List<Brand> brands = brandDao.selectByExample(null);
        return brands;
    }

    //品牌分页查询
    @Override
    public PageResult findPage(Integer pageNo, Integer pageSize) { //分页类PageHelper
        //1设置分页条件- 这里我们交给插件做
        PageHelper.startPage(pageNo, pageSize);
        // 2、根据条件查询 他返回的是一个个品牌对象
        Page<Brand> page = (Page<Brand>) brandDao.selectByExample(null);
        // 3、创建PageResult对象并且封装结果 就是把主力分装好的数据给咱自己的分页类PageResult 然后对应上返回页面
        return new PageResult(page.getResult(), page.getTotal());
        }

    @Override
    public PageResult search(Integer pageNo, Integer pageSize, Brand brand) {
        // 1、设置分页条件-分页插件
        PageHelper.startPage(pageNo, pageSize);
        // 2、设置查询条件
        BrandQuery brandQuery = new BrandQuery();
        // 封装查询条件：

        BrandQuery.Criteria criteria = brandQuery.createCriteria();
        if(brand.getName() != null && !"".equals(brand.getName().trim())){
            criteria.andNameLike("%" + brand.getName().trim() + "%");
        }
        if(brand.getFirstChar() != null && !"".equals(brand.getFirstChar().trim())){
            criteria.andFirstCharEqualTo(brand.getFirstChar().trim());
        }
        // 设置根据字段排序
        brandQuery.setOrderByClause("id desc");
        // 3、根据条件查询
        Page<Brand> page = (Page<Brand>) brandDao.selectByExample(brandQuery);
        // 4、创建PageResult对象并且封装结果
        return new PageResult(page.getResult(), page.getTotal());
    }
//新增品牌
    //加上事务
    @Transactional
    @Override
    public void add(Brand brand) {
        brandDao.insertSelective(brand);

    }

    @Override
    public Brand findOne(Long id) {
        return brandDao.selectByPrimaryKey(id);
    }


   // 更新
    @Transactional
    @Override
    public void update(Brand brand) {
         brandDao.updateByPrimaryKeySelective(brand);
    }
    @Transactional
    @Override
    public void delete(Long[] ids) {
        if(ids != null && ids.length > 0){
//            for (Long id : ids) {
//                brandDao.deleteByPrimaryKey(id);
//            }
            //批量删除
        brandDao.deleteByPrimaryKeys(ids);
      }

    }
//全查品牌
    @Override
    public List<Map<String, String>> selectOptionList() {
        return brandDao.selectOptionList();
    }

}
