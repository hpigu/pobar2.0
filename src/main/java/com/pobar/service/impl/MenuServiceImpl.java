package com.pobar.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pobar.dto.menu.ProductQueryRequest;
import com.pobar.dto.menu.ProductSaveRequest;
import com.pobar.dto.menu.RecipeSaveRequest;
import com.pobar.entity.*;
import com.pobar.exception.BusinessException;
import com.pobar.logging.Audit;
import com.pobar.mapper.*;
import com.pobar.service.MenuService;
import com.pobar.util.XssUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final DrinkAttributeTypeMapper attributeTypeMapper;
    private final DrinkAttributeOptionMapper attributeOptionMapper;
    private final IngredientMapper ingredientMapper;
    private final RecipeMapper recipeMapper;
    private final RecipeIngredientMapper recipeIngredientMapper;
    private final ProductAttributeMapper productAttributeMapper;

    @Value("${file.upload-dir}")
    private String uploadDir;

    // ─── 分類 ───────────────────────────────────

    @Override
    public List<Category> listCategories() {
        return categoryMapper.selectList(
            new LambdaQueryWrapper<Category>()
                .eq(Category::getIsActive, 1)
                .orderByAsc(Category::getDisplayOrder)
        );
    }

    @Override
    @Audit(action = "SAVE_CATEGORY", entityType = "CATEGORY")
    public Category saveCategory(Category category) {
        category.setNameZh(XssUtil.sanitize(category.getNameZh()));
        category.setNameEn(XssUtil.sanitize(category.getNameEn()));
        if (category.getId() == null) {
            categoryMapper.insert(category);
        } else {
            categoryMapper.updateById(category);
        }
        return category;
    }

    @Override
    @Audit(action = "DELETE_CATEGORY", entityType = "CATEGORY")
    public void deleteCategory(Integer id) {
        long count = productMapper.selectCount(
            new LambdaQueryWrapper<Product>()
                .eq(Product::getCategoryId, id)
                .eq(Product::getIsActive, 1)
        );
        if (count > 0) {
            throw new BusinessException("此分類下還有品項，無法刪除");
        }
        categoryMapper.deleteById(id);
    }

    // ─── 品項 ───────────────────────────────────

    @Override
    public List<Product> listProducts(ProductQueryRequest query) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
            .eq(Product::getIsActive, 1);

        if (query.getType() != null) {
            wrapper.eq(Product::getType, query.getType());
        }
        if (query.getCategoryId() != null) {
            wrapper.eq(Product::getCategoryId, query.getCategoryId());
        }
        if (Boolean.TRUE.equals(query.getAvailable())) {
            wrapper.eq(Product::getIsAvailable, 1);
        }

        List<Product> products = productMapper.selectList(wrapper);

        // 多選屬性篩選（前端傳來選項 ID，取交集）
        if (query.getAttributeOptionIds() != null && !query.getAttributeOptionIds().isEmpty()) {
            products = products.stream()
                .filter(p -> hasAllAttributes(p.getId(), query.getAttributeOptionIds()))
                .collect(Collectors.toList());
        }

        return products;
    }

    @Override
    public Product getProduct(Integer id) {
        Product product = productMapper.selectById(id);
        if (product == null || product.getIsActive() == 0) {
            throw new BusinessException(404, "品項不存在");
        }
        return product;
    }

    @Override
    @Transactional
    @Audit(action = "CREATE_PRODUCT", entityType = "PRODUCT")
    public Product createProduct(ProductSaveRequest request, Integer operatorId) {
        Product product = buildProduct(request);
        product.setCreatedBy(operatorId);
        product.setIsActive(1);
        product.setIsAvailable(1);
        productMapper.insert(product);
        saveProductAttributes(product.getId(), request.getAttributeOptionIds());
        return product;
    }

    @Override
    @Transactional
    @Audit(action = "UPDATE_PRODUCT", entityType = "PRODUCT")
    public Product updateProduct(Integer id, ProductSaveRequest request) {
        Product product = getProduct(id);
        Product updated = buildProduct(request);
        updated.setId(id);
        productMapper.updateById(updated);
        saveProductAttributes(id, request.getAttributeOptionIds());
        return updated;
    }

    @Override
    @Audit(action = "TOGGLE_PRODUCT_AVAILABILITY", entityType = "PRODUCT")
    public void toggleAvailability(Integer id, boolean available) {
        Product product = getProduct(id);
        product.setIsAvailable(available ? 1 : 0);
        productMapper.updateById(product);
    }

    @Override
    @Audit(action = "DELETE_PRODUCT", entityType = "PRODUCT")
    public void deleteProduct(Integer id) {
        Product product = getProduct(id);
        product.setIsActive(0);
        productMapper.updateById(product);
    }

    @Override
    public String saveImage(Integer productId, byte[] imageBytes, String originalFileName) {
        String ext = getExtension(originalFileName);
        if (!ext.equals("jpg") && !ext.equals("jpeg") && !ext.equals("png")) {
            throw new BusinessException("只接受 jpg 或 png 圖片");
        }
        if (imageBytes.length > 5 * 1024 * 1024) {
            throw new BusinessException("圖片不得超過 5MB");
        }

        String fileName = UUID.randomUUID() + "." + ext;
        Path path = Paths.get(uploadDir, fileName);

        try {
            Files.createDirectories(path.getParent());
            Files.write(path, imageBytes);
        } catch (IOException e) {
            throw new BusinessException("圖片儲存失敗");
        }

        String imageUrl = "/uploads/images/" + fileName;
        Product product = getProduct(productId);
        product.setImageUrl(imageUrl);
        productMapper.updateById(product);
        return imageUrl;
    }

    // ─── 食材 ───────────────────────────────────

    @Override
    public List<Ingredient> listIngredients() {
        return ingredientMapper.selectList(null);
    }

    @Override
    @Audit(action = "SAVE_INGREDIENT", entityType = "INGREDIENT")
    public Ingredient saveIngredient(Ingredient ingredient) {
        ingredient.setName(XssUtil.sanitize(ingredient.getName()));
        if (ingredient.getId() == null) {
            ingredient.setIsAvailable(1);
            ingredientMapper.insert(ingredient);
        } else {
            ingredientMapper.updateById(ingredient);
        }
        return ingredient;
    }

    @Override
    @Transactional
    @Audit(action = "SET_INGREDIENT_AVAILABILITY", entityType = "INGREDIENT")
    public void setIngredientAvailability(Integer id, boolean available) {
        Ingredient ingredient = ingredientMapper.selectById(id);
        if (ingredient == null) throw new BusinessException(404, "食材不存在");
        ingredient.setIsAvailable(available ? 1 : 0);
        ingredientMapper.updateById(ingredient);
        // 連動下架/上架所有使用此食材的酒品
        productMapper.updateAvailabilityByIngredient(id, available ? 1 : 0);
    }

    // ─── 酒譜 ───────────────────────────────────

    @Override
    public Recipe getRecipe(Integer productId) {
        Recipe recipe = recipeMapper.selectByProductId(productId);
        if (recipe == null) throw new BusinessException(404, "此品項尚未設定酒譜");
        return recipe;
    }

    @Override
    @Transactional
    @Audit(action = "SAVE_RECIPE", entityType = "PRODUCT")
    public Recipe saveRecipe(Integer productId, RecipeSaveRequest request) {
        getProduct(productId); // 確認品項存在

        Recipe recipe = recipeMapper.selectByProductId(productId);
        if (recipe == null) {
            recipe = new Recipe();
            recipe.setProductId(productId);
            recipe.setPreparationNotes(XssUtil.sanitize(request.getPreparationNotes()));
            recipeMapper.insert(recipe);
        } else {
            recipe.setPreparationNotes(XssUtil.sanitize(request.getPreparationNotes()));
            recipeMapper.updateById(recipe);
            recipeIngredientMapper.deleteByRecipeId(recipe.getId());
        }

        int order = 0;
        for (RecipeSaveRequest.IngredientItem item : request.getIngredients()) {
            RecipeIngredient ri = new RecipeIngredient();
            ri.setRecipeId(recipe.getId());
            ri.setIngredientId(item.getIngredientId());
            ri.setQuantity(item.getQuantity());
            ri.setUnit(item.getUnit());
            ri.setDisplayOrder(item.getDisplayOrder() != null ? item.getDisplayOrder() : order++);
            recipeIngredientMapper.insert(ri);
        }

        return recipe;
    }

    // ─── 私用方法 ─────────────────────────────────

    private Product buildProduct(ProductSaveRequest request) {
        Product product = new Product();
        product.setNameZh(XssUtil.sanitize(request.getNameZh()));
        product.setNameEn(XssUtil.sanitize(request.getNameEn()));
        product.setDescriptionZh(XssUtil.sanitize(request.getDescriptionZh()));
        product.setDescriptionEn(XssUtil.sanitize(request.getDescriptionEn()));
        product.setCategoryId(request.getCategoryId());
        product.setPrice(request.getPrice());
        product.setType(request.getType());
        product.setAvailableStartTime(request.getAvailableStartTime());
        product.setAvailableEndTime(request.getAvailableEndTime());
        product.setAvailableFromDate(request.getAvailableFromDate());
        product.setAvailableToDate(request.getAvailableToDate());
        return product;
    }

    private void saveProductAttributes(Integer productId, List<Integer> optionIds) {
        productAttributeMapper.deleteByProductId(productId);
        if (optionIds == null || optionIds.isEmpty()) return;
        for (Integer optionId : optionIds) {
            productAttributeMapper.insert(productId, optionId);
        }
    }

    private boolean hasAllAttributes(Integer productId, List<Integer> optionIds) {
        int matched = productAttributeMapper.countMatchingOptions(productId, optionIds);
        return matched == optionIds.size();
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
