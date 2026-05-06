package com.pobar.service.impl;

import com.pobar.dto.ingredient.IngredientRequest;
import com.pobar.dto.ingredient.IngredientResponse;
import com.pobar.entity.Ingredient;
import com.pobar.exception.BusinessException;
import com.pobar.logging.Audit;
import com.pobar.mapper.IngredientMapper;
import com.pobar.mapper.ProductMapper;
import com.pobar.service.IngredientService;
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
    @Audit(action = "CREATE_INGREDIENT", entityType = "Ingredient")
    public IngredientResponse create(IngredientRequest request) {
        Ingredient ingredient = fromRequest(request, new Ingredient());
        ingredientMapper.insert(ingredient);
        return toResponse(ingredient);
    }

    @Override
    @Audit(action = "UPDATE_INGREDIENT", entityType = "Ingredient")
    public IngredientResponse update(Integer id, IngredientRequest request) {
        Ingredient ingredient = requireExists(id);
        fromRequest(request, ingredient);
        ingredientMapper.updateById(ingredient);
        return toResponse(ingredient);
    }

    @Override
    @Transactional
    @Audit(action = "SET_INGREDIENT_AVAILABILITY", entityType = "Ingredient")
    public void setAvailable(Integer id, boolean available) {
        Ingredient ingredient = requireExists(id);
        ingredient.setIsAvailable(available ? 1 : 0);
        ingredientMapper.updateById(ingredient);
        // 食材缺貨時，串聯下架使用該食材的所有品項
        productMapper.updateAvailabilityByIngredient(id, available ? 1 : 0);
    }

    @Override
    @Audit(action = "DELETE_INGREDIENT", entityType = "Ingredient")
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
        target.setName(req.getName());
        target.setUnit(req.getUnit());
        if (req.getIsAvailable() != null) {
            target.setIsAvailable(req.getIsAvailable() ? 1 : 0);
        } else if (target.getIsAvailable() == null) {
            target.setIsAvailable(1);
        }
        return target;
    }

    private IngredientResponse toResponse(Ingredient i) {
        IngredientResponse r = new IngredientResponse();
        r.setId(i.getId());
        r.setName(i.getName());
        r.setUnit(i.getUnit());
        r.setIsAvailable(i.getIsAvailable() != null && i.getIsAvailable() == 1);
        r.setCreatedAt(i.getCreatedAt());
        return r;
    }
}
