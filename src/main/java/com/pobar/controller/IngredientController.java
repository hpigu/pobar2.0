package com.pobar.controller;

import com.pobar.common.Result;
import com.pobar.dto.ingredient.IngredientRequest;
import com.pobar.dto.ingredient.IngredientResponse;
import com.pobar.service.IngredientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ingredients")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    @GetMapping
    public Result<List<IngredientResponse>> listAll() {
        return Result.ok(ingredientService.listAll());
    }

    @PostMapping
    public Result<IngredientResponse> create(@Valid @RequestBody IngredientRequest request) {
        return Result.ok(ingredientService.create(request));
    }

    @PutMapping("/{id}")
    public Result<IngredientResponse> update(@PathVariable Integer id,
                                              @Valid @RequestBody IngredientRequest request) {
        return Result.ok(ingredientService.update(id, request));
    }

    @PatchMapping("/{id}/availability")
    public Result<?> setAvailability(@PathVariable Integer id,
                                     @RequestBody Map<String, Boolean> body) {
        ingredientService.setAvailable(id, Boolean.TRUE.equals(body.get("available")));
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Integer id) {
        ingredientService.delete(id);
        return Result.ok();
    }
}
