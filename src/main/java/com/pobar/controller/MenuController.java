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
    public Result<Category> createCategory(@RequestBody Category category) {
        category.setId(null);
        return Result.ok(menuService.saveCategory(category));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Result<Category> updateCategory(@PathVariable Integer id, @RequestBody Category category) {
        category.setId(id);
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
        String account = String.valueOf(auth.getDetails());
        return Result.ok(menuService.createProduct(request, account));
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

}
