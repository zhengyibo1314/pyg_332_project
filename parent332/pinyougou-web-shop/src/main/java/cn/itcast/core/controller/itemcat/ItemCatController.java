package cn.itcast.core.controller.itemcat;

import cn.itcast.core.pojo.item.ItemCat;
import cn.itcast.core.service.itemcat.ItemCatService;
import com.alibaba.dubbo.config.annotation.Reference;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/itemCat")
public class ItemCatController {

    @Reference
    private ItemCatService itemCatService;


    /**
     * 商品分类的列表查询
     *这个是显示点一下会显示下个三级目录
     * @param parentId
     * @return
     */
    @RequestMapping("/findByParentId.do")
    public List<ItemCat> findByParentId(Long parentId) {
        return itemCatService.findByParentId(parentId);
    }

    /**
     * 通过三级分类加载出模板id
     * @param id
     * @return
     */
    @RequestMapping("/findOne.do")
    public ItemCat findOne(Long id) {
        return itemCatService.findOne(id);
    }


    //查询所有分类 并显示分类名字
    @RequestMapping("/findAll.do")
    public List<ItemCat>findAll(){
        return itemCatService.findAll();
    }

}
