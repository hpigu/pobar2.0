package com.pobar.service;

import com.pobar.dto.menu.ProductQueryRequest;
import com.pobar.dto.menu.ProductSaveRequest;
import com.pobar.dto.menu.RecipeSaveRequest;
import com.pobar.entity.Category;
import com.pobar.entity.Ingredient;
import com.pobar.entity.Product;
import com.pobar.entity.Recipe;

import java.util.List;

public interface MenuService {

    // 分類
    List<Category> listCategories();
    Category saveCategory(Category category);
    void deleteCategory(Integer id);

    // 品項
    List<Product> listProducts(ProductQueryRequest query);
    Product getProduct(Integer id);
    Product createProduct(ProductSaveRequest request, Integer operatorId);
    Product updateProduct(Integer id, ProductSaveRequest request);
    void toggleAvailability(Integer id, boolean available);
    void deleteProduct(Integer id);
    String saveImage(Integer productId, byte[] imageBytes, String originalFileName);

    // 酒譜
    Recipe getRecipe(Integer productId);
    Recipe saveRecipe(Integer productId, RecipeSaveRequest request);
}
