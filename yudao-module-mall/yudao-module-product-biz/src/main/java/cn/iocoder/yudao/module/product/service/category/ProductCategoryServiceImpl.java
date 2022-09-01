package cn.iocoder.yudao.module.product.service.category;

import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.product.controller.admin.category.vo.ProductCategoryCreateReqVO;
import cn.iocoder.yudao.module.product.controller.admin.category.vo.ProductCategoryListReqVO;
import cn.iocoder.yudao.module.product.controller.admin.category.vo.ProductCategoryUpdateReqVO;
import cn.iocoder.yudao.module.product.convert.category.ProductCategoryConvert;
import cn.iocoder.yudao.module.product.dal.dataobject.category.ProductCategoryDO;
import cn.iocoder.yudao.module.product.dal.mysql.category.ProductCategoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.*;

/**
 * 商品分类 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ProductCategoryServiceImpl implements ProductCategoryService {

    @Resource
    private ProductCategoryMapper productCategoryMapper;

    @Override
    public Long createProductCategory(ProductCategoryCreateReqVO createReqVO) {
        // 校验父分类存在
        validateParentProductCategory(createReqVO.getParentId());

        // 插入
        ProductCategoryDO category = ProductCategoryConvert.INSTANCE.convert(createReqVO);
        productCategoryMapper.insert(category);
        // 返回
        return category.getId();
    }

    @Override
    public void updateProductCategory(ProductCategoryUpdateReqVO updateReqVO) {
        // 校验分类是否存在
        validateProductCategoryExists(updateReqVO.getId());
        // 校验父分类存在
        validateParentProductCategory(updateReqVO.getParentId());

        // 更新
        ProductCategoryDO updateObj = ProductCategoryConvert.INSTANCE.convert(updateReqVO);
        productCategoryMapper.updateById(updateObj);
    }

    @Override
    public void deleteProductCategory(Long id) {
        // 校验分类是否存在
        validateProductCategoryExists(id);
        // 校验是否还有子分类
        if (productCategoryMapper.selectCountByParentId(id) > 0) {
            throw exception(PRODUCT_CATEGORY_EXISTS_CHILDREN);
        }

        // 删除
        productCategoryMapper.deleteById(id);
    }

    private void validateParentProductCategory(Long id) {
        // 如果是根分类，无需验证
        if (Objects.equals(id, ProductCategoryDO.PARENT_ID_NULL)) {
            return;
        }
        // 父分类不存在
        ProductCategoryDO category = productCategoryMapper.selectById(id);
        if (category == null) {
            throw exception(PRODUCT_CATEGORY_PARENT_NOT_EXISTS);
        }
        // 父分类不能是二级分类
        if (Objects.equals(id, ProductCategoryDO.PARENT_ID_NULL)) {
            throw exception(PRODUCT_CATEGORY_PARENT_NOT_FIRST_LEVEL);
        }
    }

    private void validateProductCategoryExists(Long id) {
        ProductCategoryDO category = productCategoryMapper.selectById(id);
        if (category == null) {
            throw exception(PRODUCT_CATEGORY_NOT_EXISTS);
        }
    }

    @Override
    public void validateProductCategory(Long id) {
        Integer level = categoryLevel(id, 1);
        if(level < 3){
          throw exception(PRODUCT_CATEGORY_LEVEL);
        }
    }

    // 校验分类级别
    private Integer categoryLevel(Long id, int level){
        ProductCategoryDO category = productCategoryMapper.selectById(id);
        if (category == null) {
            throw exception(PRODUCT_CATEGORY_NOT_EXISTS);
        }
        if (ObjectUtil.notEqual(category.getStatus(), CommonStatusEnum.ENABLE.getStatus())) {
            throw exception(PRODUCT_CATEGORY_DISABLED);
        }
        if(category.getParentId() == 0) {
            return level;
        }
        return categoryLevel(category.getParentId(), ++level);
    }

    @Override
    public ProductCategoryDO getProductCategory(Long id) {
        return productCategoryMapper.selectById(id);
    }

    @Override
    public List<ProductCategoryDO> getEnableProductCategoryList(Collection<Long> ids) {
        return productCategoryMapper.selectBatchIds(ids);
    }

    @Override
    public List<ProductCategoryDO> getEnableProductCategoryList(ProductCategoryListReqVO listReqVO) {
        return productCategoryMapper.selectList(listReqVO);
    }

    @Override
    public List<ProductCategoryDO> getEnableProductCategoryList() {
        return productCategoryMapper.selectListByStatus(CommonStatusEnum.ENABLE.getStatus());
    }

}