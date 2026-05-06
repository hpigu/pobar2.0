package com.pobar.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pobar.entity.DrinkAttributeOption;
import com.pobar.entity.DrinkAttributeType;
import com.pobar.logging.Audit;
import com.pobar.mapper.DrinkAttributeOptionMapper;
import com.pobar.mapper.DrinkAttributeTypeMapper;
import com.pobar.service.AttributeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttributeServiceImpl implements AttributeService {

    private final DrinkAttributeTypeMapper typeMapper;
    private final DrinkAttributeOptionMapper optionMapper;

    @Override
    public List<Map<String, Object>> listAllWithOptions() {
        List<DrinkAttributeType> types = typeMapper.selectList(
                new LambdaQueryWrapper<DrinkAttributeType>()
                        .eq(DrinkAttributeType::getIsActive, 1)
                        .orderByAsc(DrinkAttributeType::getDisplayOrder));

        List<DrinkAttributeOption> options = optionMapper.selectList(
                new LambdaQueryWrapper<DrinkAttributeOption>()
                        .eq(DrinkAttributeOption::getIsActive, 1)
                        .orderByAsc(DrinkAttributeOption::getDisplayOrder));

        Map<Integer, List<DrinkAttributeOption>> grouped = options.stream()
                .collect(Collectors.groupingBy(DrinkAttributeOption::getAttributeTypeId));

        return types.stream().map(t -> Map.of(
                "id", t.getId(),
                "nameZh", t.getNameZh(),
                "nameEn", t.getNameEn() != null ? t.getNameEn() : "",
                "options", grouped.getOrDefault(t.getId(), List.of())
        )).collect(Collectors.toList());
    }

    @Override
    @Audit(action = "CREATE_ATTRIBUTE_TYPE", entityType = "ATTRIBUTE_TYPE")
    public DrinkAttributeType createType(DrinkAttributeType type) {
        type.setIsActive(1);
        typeMapper.insert(type);
        return type;
    }

    @Override
    @Audit(action = "UPDATE_ATTRIBUTE_TYPE", entityType = "ATTRIBUTE_TYPE")
    public DrinkAttributeType updateType(Integer id, DrinkAttributeType type) {
        type.setId(id);
        typeMapper.updateById(type);
        return type;
    }

    @Override
    @Audit(action = "DELETE_ATTRIBUTE_TYPE", entityType = "ATTRIBUTE_TYPE")
    public void deleteType(Integer id) {
        typeMapper.deleteById(id);
    }

    @Override
    @Audit(action = "CREATE_ATTRIBUTE_OPTION", entityType = "ATTRIBUTE_OPTION")
    public DrinkAttributeOption createOption(Integer typeId, DrinkAttributeOption option) {
        option.setAttributeTypeId(typeId);
        option.setIsActive(1);
        optionMapper.insert(option);
        return option;
    }

    @Override
    @Audit(action = "UPDATE_ATTRIBUTE_OPTION", entityType = "ATTRIBUTE_OPTION")
    public DrinkAttributeOption updateOption(Integer id, DrinkAttributeOption option) {
        option.setId(id);
        optionMapper.updateById(option);
        return option;
    }

    @Override
    @Audit(action = "DELETE_ATTRIBUTE_OPTION", entityType = "ATTRIBUTE_OPTION")
    public void deleteOption(Integer id) {
        optionMapper.deleteById(id);
    }
}
