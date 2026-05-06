package com.pobar.controller;

import com.pobar.common.Result;
import com.pobar.dto.menu.ProductQueryRequest;
import com.pobar.dto.menu.ProductSaveRequest;
import com.pobar.dto.menu.RecipeSaveRequest;
import com.pobar.entity.*;
import com.pobar.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    // ─── 分類（公開讀取，管理才需登入）─────────────────

    @GetMapping("/categories")
    public Result<List<Category>> listCategories() {
        return Result.ok(menuService.listCategories());
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Result<Category> saveCategory(@RequestBody Category category) {
        return Result.ok(menuService.saveCategory(category));
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Result<?> deleteCategory(@PathVariable Integer id) {
        menuService.deleteCategory(id);
        return Result.ok();
    }

    // ─── 品項（公開讀取）─────────────────────────────

    @GetMapping("/menu")
    public Result<List<Product>> listProducts(ProductQueryRequest query) {
        return Result.ok(menuService.listProducts(query));
    }

    @GetMapping("/menu/{id}")
    public Result<Product> getProduct(@PathVariable Integer id) {
        return Result.ok(menuService.getProduct(id));
    }

    @PostMapping("/menu")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','BARTENDER')")
    public Result<Product> createProduct(@Valid @RequestBody ProductSaveRequest request,
                                         Authentication auth) {
        Integer userId = (Integer) auth.getPrincipal();
        return Result.ok(menuService.createProduct(request, userId));
    }

    @PutMapping("/menu/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','BARTENDER')")
    public Result<Product> updateProduct(@PathVariable Integer id,
                                          @Valid @RequestBody ProductSaveRequest request) {
        return Result.ok(menuService.updateProduct(id, request));
    }

    @PutMapping("/menu/{id}/availability")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','BARTENDER')")
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
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','BARTENDER')")
    public Result<String> uploadImage(@PathVariable Integer id,
                                       @RequestParam MultipartFile file) throws IOException {
        String url = menuService.saveImage(id, file.getBytes(), file.getOriginalFilename());
        return Result.ok(url);
    }

    // ─── 酒譜（調酒師以上才能讀取）─────────────────────

    @GetMapping("/menu/{productId}/recipe")
    @PreAuthorize("hasAnyRole('BARTENDER','MANAGER','ADMIN')")
    public Result<Recipe> getRecipe(@PathVariable Integer productId) {
        return Result.ok(menuService.getRecipe(productId));
    }

    @PostMapping("/menu/{productId}/recipe")
    @PreAuthorize("hasAnyRole('BARTENDER','MANAGER','ADMIN')")
    public Result<Recipe> saveRecipe(@PathVariable Integer productId,
                                      @Valid @RequestBody RecipeSaveRequest request) {
        return Result.ok(menuService.saveRecipe(productId, request));
    }

    // ─── 食材 ─────────────────────────────────────────

    @GetMapping("/ingredients")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Result<List<Ingredient>> listIngredients() {
        return Result.ok(menuService.listIngredients());
    }

    @PostMapping("/ingredients")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Result<Ingredient> saveIngredient(@RequestBody Ingredient ingredient) {
        return Result.ok(menuService.saveIngredient(ingredient));
    }

    @PutMapping("/ingredients/{id}/availability")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Result<?> setIngredientAvailability(@PathVariable Integer id,
                                                @RequestParam boolean available) {
        menuService.setIngredientAvailability(id, available);
        return Result.ok();
    }

}
