package cn.itcast.core.service.content;

import java.util.List;

import cn.itcast.core.entity.PageResult;
import cn.itcast.core.pojo.ad.ContentQuery;
import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;

import cn.itcast.core.dao.ad.ContentDao;
import cn.itcast.core.pojo.ad.Content;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;

@Service
public class ContentServiceImpl implements ContentService {

	@Resource
	private ContentDao contentDao;
	@Resource
	private RedisTemplate<String,Object> redisTemplate;
	@Override
	public List<Content> findAll() {
		List<Content> list = contentDao.selectByExample(null);
		return list;
	}

	@Override
	public PageResult findPage(Content content, Integer pageNum, Integer pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		Page<Content> page = (Page<Content>)contentDao.selectByExample(null);
		return new PageResult(page.getResult(), page.getTotal());
	}

	@Override
	public void add(Content content) {
		// 新增广告时需要更新缓存，清空之前的缓存
		clearCache(content.getCategoryId());
		contentDao.insertSelective(content);
	}

	@Override
	public void edit(Content content) {
		// 判断广告分类是否发生改变
		Long newCategoryId = content.getCategoryId();
		Long oldCategoryId = contentDao.selectByPrimaryKey(content.getId()).getCategoryId();
		if(newCategoryId != oldCategoryId){
			// 分类改变了，清空老的分类、新的id
			clearCache(newCategoryId);
			clearCache(oldCategoryId);
		}else{
			// 分类未改变
			clearCache(newCategoryId);
		}
		contentDao.updateByPrimaryKeySelective(content);
	}

	@Override
	public Content findOne(Long id) {
		Content content = contentDao.selectByPrimaryKey(id);
		return content;
	}

	@Override
	public void delAll(Long[] ids) {
		if(ids != null){
			for(Long id : ids){
				Content content = contentDao.selectByPrimaryKey(id);
				// 删除广告时需要更新缓存，清空之前的缓存
				clearCache(content.getCategoryId());
				contentDao.deleteByPrimaryKey(id);
			}
		}
	}
	/**
	 * 清空缓存
	 * @param categoryId
	 */
	private void clearCache(Long categoryId) {
		redisTemplate.boundHashOps("content").delete(categoryId);
	}

	/**
	 * 首页大广告的轮播图
	 * @param categoryId
	 * @return
	 */
    @Override
    public List<Content> findByCategoryId(Long categoryId) {
		//1判断缓存中是否有数据
		List<Content> list = (List<Content>) redisTemplate.boundHashOps("content").get(categoryId);
		synchronized (this) {
			list = (List<Content>) redisTemplate.boundHashOps("content").get(categoryId);
			if (list == null) {
				//2.缓存中没有数据 就去数据库查
				// 查询该分类下的广告列表
				// 根据sort_order排序并且查询可用的广告
				ContentQuery contentQuery = new ContentQuery();
				contentQuery.createCriteria().andCategoryIdEqualTo(categoryId).andStatusEqualTo("1");
				contentQuery.setOrderByClause("sort_order desc");
				 list = contentDao.selectByExample(contentQuery);
				//查完将数据方法哦缓存中
				redisTemplate.boundHashOps("content").put(categoryId, list);
			}
		}
			return list;


	}
}
