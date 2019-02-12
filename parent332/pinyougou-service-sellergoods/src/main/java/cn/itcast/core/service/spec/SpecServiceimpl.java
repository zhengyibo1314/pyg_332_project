package cn.itcast.core.service.spec;

import cn.itcast.core.dao.specification.SpecificationDao;
import cn.itcast.core.dao.specification.SpecificationOptionDao;
import cn.itcast.core.entity.PageResult;
import cn.itcast.core.pojo.good.BrandQuery;
import cn.itcast.core.pojo.specification.Specification;
import cn.itcast.core.pojo.specification.SpecificationOption;
import cn.itcast.core.pojo.specification.SpecificationOptionQuery;
import cn.itcast.core.pojo.specification.SpecificationQuery;
import cn.itcast.core.vo.SpecVo;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.annotation.Resources;
import java.util.List;
import java.util.Map;

@Service
public class SpecServiceimpl implements SpecService {
    @Resource
  private   SpecificationDao specificationDao;
    @Resource
 private    SpecificationOptionDao specificationOptionDao;
    @Override
    public PageResult search(Integer page, Integer rows, Specification specification) {
        //设置分页条件
        PageHelper.startPage(page,rows);
        //设置条件查询
        SpecificationQuery specificationQuery = new SpecificationQuery();
        SpecificationQuery.Criteria criteria = specificationQuery.createCriteria();

        if (specification.getSpecName()!=null && !"".equals(specification.getSpecName().trim())){
            criteria.andSpecNameLike("%"+specification.getSpecName().trim()+"%");
        }
        specificationQuery.setOrderByClause("id desc");
        //根据条件查询
         Page<Specification> p= (Page<Specification>) specificationDao.selectByExample(specificationQuery);
        //封装PageResult返回
          return new PageResult(p.getResult(),p.getTotal());
    }
    //新增规格
    @Transactional
    @Override
    public void add(SpecVo specVo) {
 //保存规格
        Specification specification = specVo.getSpecification();
        specificationDao.insertSelective(specification);//保存规格 所以要返回id 所以配置返回自增主键
         //保存规格项-有规格的id 就是外键
        List<SpecificationOption> specificationOptionList = specVo.getSpecificationOptionList();
        if (specificationOptionList !=null&&specificationOptionList.size()>0){
            for (SpecificationOption specificationOption : specificationOptionList) {
                //设置外键
                specificationOption.setSpecId(specification.getId());
               // specificationOptionDao.insertSelective(specificationOption);
            }
            specificationOptionDao.insertSelectives(specificationOptionList);
        }
    }

//规格回显
    @Override
    public SpecVo findOne(Long id) {
        //查询规格
        Specification specification = specificationDao.selectByPrimaryKey(id);
        //查询规格选项
        SpecificationOptionQuery specificationQuery = new SpecificationOptionQuery ();
        specificationQuery.createCriteria().andSpecIdEqualTo(id);
    List<SpecificationOption>specificationList = specificationOptionDao.selectByExample(specificationQuery);
      //封装数据
        SpecVo specVo = new SpecVo();
        specVo.setSpecification(specification);
        specVo.setSpecificationOptionList(specificationList);
        return specVo;
    }
//规格更新
    @Override
    public void update(SpecVo specVo) {
        //更新规格
        Specification specification = specVo.getSpecification();
        specificationDao.updateByPrimaryKey(specification);
        //更新规格项
        //先删除
        SpecificationOptionQuery optionQuery = new SpecificationOptionQuery();
        //获取规格id
        optionQuery.createCriteria().andSpecIdEqualTo(specification.getId());
        specificationOptionDao.deleteByExample(optionQuery);
        //后插入
        List<SpecificationOption> specificationOptionList = specVo.getSpecificationOptionList();
        if (specificationOptionList!=null&&specificationOptionList.size()>0){
            for (SpecificationOption specificationOption : specificationOptionList) {
                specificationOption.setSpecId(specification.getId());
            }

            specificationOptionDao.insertSelectives(specificationOptionList);
        }

    }

    @Override
    public void delete(Long[] ids) {
        if (ids!=null&&ids.length>0){
            for (Long id : ids) {
                //删除规格
                specificationDao.deleteByPrimaryKey(id);
                //规格选项
                SpecificationOptionQuery optionQuery = new SpecificationOptionQuery();
                optionQuery.createCriteria().andSpecIdEqualTo(id);
                specificationOptionDao.deleteByExample(optionQuery);

            }
        }
    }

    /**
     * 新增模板时初始化规格列表
     * @return
     */

    @Override
    public List<Map<String, String>> selectOptionList() {
        return specificationDao.selectOptionList();
    }
}
