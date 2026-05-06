package com.pobar.controller;

import com.pobar.common.Result;
import com.pobar.entity.DrinkAttributeOption;
import com.pobar.entity.DrinkAttributeType;
import com.pobar.service.AttributeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attributes")
@RequiredArgsConstructor
public class AttributeController {

    private final AttributeService attributeService;

    @GetMapping
    public Result<List<Map<String, Object>>> listAll() {
        return Result.ok(attributeService.listAllWithOptions());
    }

    @PostMapping("/types")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<DrinkAttributeType> createType(@RequestBody DrinkAttributeType type) {
        return Result.ok(attributeService.createType(type));
    }

    @PutMapping("/types/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<DrinkAttributeType> updateType(@PathVariable Integer id,
                                                  @RequestBody DrinkAttributeType type) {
        return Result.ok(attributeService.updateType(id, type));
    }

    @DeleteMapping("/types/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> deleteType(@PathVariable Integer id) {
        attributeService.deleteType(id);
        return Result.ok();
    }

    @PostMapping("/types/{typeId}/options")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<DrinkAttributeOption> createOption(@PathVariable Integer typeId,
                                                      @RequestBody DrinkAttributeOption option) {
        return Result.ok(attributeService.createOption(typeId, option));
    }

    @PutMapping("/options/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<DrinkAttributeOption> updateOption(@PathVariable Integer id,
                                                      @RequestBody DrinkAttributeOption option) {
        return Result.ok(attributeService.updateOption(id, option));
    }

    @DeleteMapping("/options/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> deleteOption(@PathVariable Integer id) {
        attributeService.deleteOption(id);
        return Result.ok();
    }
}
