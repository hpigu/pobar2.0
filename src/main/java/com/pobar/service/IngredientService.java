package com.pobar.service;

import com.pobar.dto.ingredient.IngredientRequest;
import com.pobar.dto.ingredient.IngredientResponse;

import java.util.List;

public interface IngredientService {

    List<IngredientResponse> listAll();

    IngredientResponse create(IngredientRequest request);

    IngredientResponse update(Integer id, IngredientRequest request);

    void setAvailable(Integer id, boolean available);

    void delete(Integer id);
}
