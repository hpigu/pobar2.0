package com.pobar.controller;

import com.pobar.common.Result;
import com.pobar.dto.menu.CategoryResponse;
import com.pobar.dto.menu.ProductQueryRequest;
import com.pobar.dto.menu.ProductResponse;
import com.pobar.dto.menu.ProductSaveRequest;
import com.pobar.dto.menu.RecipeDetailDto;
import com.pobar.dto.menu.RecipeResponse;
import com.pobar.dto.menu.RecipeSaveRequest;
import com.pobar.entity.Category;
import com.pobar.mapper.RecipeMapper;
import com.pobar.security.AuthUser;
import com.pobar.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;
    private final RecipeMapper recipeMapper;

    // ─── 分類（公開讀取，管理才需登入）─────────────────

    @GetMapping("/categories")
    public Result<List<CategoryResponse>> listCategories() {
        return Result.ok(menuService.listCategories().stream().map(CategoryResponse::from).toList());
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Result<CategoryResponse> createCategory(@RequestBody Category category) {
        category.setId(null);
        return Result.ok(CategoryResponse.from(menuService.saveCategory(category)));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Result<CategoryResponse> updateCategory(@PathVariable Integer id, @RequestBody Category category) {
        category.setId(id);
        return Result.ok(CategoryResponse.from(menuService.saveCategory(category)));
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Result<?> deleteCategory(@PathVariable Integer id) {
        menuService.deleteCategory(id);
        return Result.ok();
    }

    // ─── 品項（公開讀取）─────────────────────────────

    @GetMapping("/menu")
    public Result<List<ProductResponse>> listProducts(ProductQueryRequest query) {
        var products = menuService.listProducts(query);
        if (products.isEmpty()) return Result.ok(List.of());
        var ids = products.stream().map(p -> p.getId()).toList();
        Map<Integer, List<String>> ingredientsMap = new LinkedHashMap<>();
        for (Map<String, Object> row : recipeMapper.selectIngredientNamesByProductIds(ids)) {
            Integer pid = ((Number) row.get("productId")).intValue();
            String name = (String) row.get("name");
            ingredientsMap.computeIfAbsent(pid, k -> new ArrayList<>()).add(name);
        }
        return Result.ok(products.stream().map(p -> {
            ProductResponse r = ProductResponse.from(p);
            r.setIngredients(ingredientsMap.getOrDefault(p.getId(), List.of()));
            return r;
        }).toList());
    }

    @GetMapping("/menu/{id}")
    public Result<ProductResponse> getProduct(@PathVariable Integer id) {
        ProductResponse r = ProductResponse.from(menuService.getProduct(id));
        if (r != null) {
            r.setIngredients(recipeMapper.selectIngredientNamesByProductId(id));
        }
        return Result.ok(r);
    }

    @PostMapping("/menu")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Result<ProductResponse> createProduct(@Valid @RequestBody ProductSaveRequest request,
                                                 Authentication auth) {
        String account = ((AuthUser) auth.getPrincipal()).account();
        return Result.ok(ProductResponse.from(menuService.createProduct(request, account)));
    }

    @PutMapping("/menu/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Result<ProductResponse> updateProduct(@PathVariable Integer id,
                                                 @Valid @RequestBody ProductSaveRequest request) {
        return Result.ok(ProductResponse.from(menuService.updateProduct(id, request)));
    }

    @PutMapping("/menu/{id}/availability")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Result<?> toggleAvailability(@PathVariable Integer id,
                                         @RequestParam boolean available) {
        menuService.toggleAvailability(id, available);
        return Result.ok();
    }

    @DeleteMapping("/menu/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Result<?> deleteProduct(@PathVariable Integer id) {
        menuService.deleteProduct(id);
        return Result.ok();
    }

    @PostMapping("/menu/{id}/image")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Result<String> uploadImage(@PathVariable Integer id,
                                       @RequestParam MultipartFile file) throws IOException {
        String url = menuService.saveImage(id, file.getBytes(), file.getOriginalFilename());
        return Result.ok(url);
    }

    // ─── 酒譜（店長／管理員才能讀寫）─────────────────────

    @GetMapping("/menu/{productId}/recipe")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Result<RecipeResponse> getRecipe(@PathVariable Integer productId) {
        return Result.ok(RecipeResponse.from(menuService.getRecipe(productId)));
    }

    @PostMapping("/menu/{productId}/recipe")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Result<RecipeResponse> saveRecipe(@PathVariable Integer productId,
                                              @Valid @RequestBody RecipeSaveRequest request) {
        return Result.ok(RecipeResponse.from(menuService.saveRecipe(productId, request)));
    }

    @GetMapping("/menu/{productId}/recipe-detail")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Result<RecipeDetailDto> getRecipeDetail(@PathVariable Integer productId) {
        return Result.ok(menuService.getRecipeDetail(productId));
    }

}
