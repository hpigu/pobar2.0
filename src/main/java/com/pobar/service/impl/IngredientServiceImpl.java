package com.pobar.service.impl;

import com.pobar.dto.ingredient.IngredientRequest;
import com.pobar.dto.ingredient.IngredientResponse;
import com.pobar.entity.Ingredient;
import com.pobar.exception.BusinessException;
import com.pobar.logging.Audit;
import com.pobar.mapper.IngredientMapper;
import com.pobar.mapper.ProductMapper;
import com.pobar.service.IngredientService;
import com.pobar.util.XssUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IngredientServiceImpl implements IngredientService {

    private final IngredientMapper ingredientMapper;
    private final ProductMapper productMapper;

    @Override
    public List<IngredientResponse> listAll() {
        return ingredientMapper.selectList(null).stream().map(this::toResponse).toList();
    }

    @Override
    @Audit(action = "CREATE_INGREDIENT", entityType = "Ingredient",
            entityIdExpr = "#result?.id",
            detailExpr = "'name=' + #request.name + ', unit=' + #request.unit + ', category=' + #request.category")
    public IngredientResponse create(IngredientRequest request) {
        Ingredient ingredient = fromRequest(request, new Ingredient());
        ingredientMapper.insert(ingredient);
        return toResponse(ingredient);
    }

    @Override
    @Audit(action = "UPDATE_INGREDIENT", entityType = "Ingredient",
            entityIdExpr = "#id",
            detailExpr = "'name=' + #request.name + ', unit=' + #request.unit + ', category=' + #request.category + ', isAvailable=' + #request.isAvailable")
    public IngredientResponse update(Integer id, IngredientRequest request) {
        Ingredient ingredient = requireExists(id);
        fromRequest(request, ingredient);
        ingredientMapper.updateById(ingredient);
        return toResponse(ingredient);
    }

    @Override
    @Transactional
    @Audit(action = "SET_INGREDIENT_AVAILABILITY", entityType = "Ingredient",
            entityIdExpr = "#id",
            detailExpr = "'available=' + #available")
    public void setAvailable(Integer id, boolean available) {
        Ingredient ingredient = requireExists(id);
        ingredient.setIsAvailable(available);
        ingredientMapper.updateById(ingredient);
        // 食材缺貨時，串聯下架使用該食材的所有品項
        productMapper.updateAvailabilityByIngredient(id, available);
    }

    @Override
    @Audit(action = "DELETE_INGREDIENT", entityType = "Ingredient",
            entityIdExpr = "#id")
    public void delete(Integer id) {
        requireExists(id);
        ingredientMapper.deleteById(id);
    }

    private Ingredient requireExists(Integer id) {
        Ingredient ingredient = ingredientMapper.selectById(id);
        if (ingredient == null) throw new BusinessException(404, "找不到此食材");
        return ingredient;
    }

    private Ingredient fromRequest(IngredientRequest req, Ingredient target) {
        target.setName(XssUtil.sanitize(req.getName()));
        target.setUnit(XssUtil.sanitize(req.getUnit()));
        String category = req.getCategory();
        target.setCategory(category == null || category.isBlank() ? "OTHER" : category);
        if (req.getIsAvailable() != null) {
            target.setIsAvailable(req.getIsAvailable());
        } else if (target.getIsAvailable() == null) {
            target.setIsAvailable(true);
        }
        return target;
    }

    private IngredientResponse toResponse(Ingredient i) {
        IngredientResponse r = new IngredientResponse();
        r.setId(i.getId());
        r.setName(i.getName());
        r.setUnit(i.getUnit());
        r.setCategory(i.getCategory() == null ? "OTHER" : i.getCategory());
        r.setIsAvailable(i.getIsAvailable());
        r.setCreatedAt(i.getCreatedAt());
        return r;
    }
}
