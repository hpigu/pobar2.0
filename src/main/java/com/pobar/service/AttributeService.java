package com.pobar.service;

import com.pobar.entity.DrinkAttributeOption;
import com.pobar.entity.DrinkAttributeType;

import java.util.List;
import java.util.Map;

public interface AttributeService {

    List<Map<String, Object>> listAllWithOptions();

    DrinkAttributeType createType(DrinkAttributeType type);

    DrinkAttributeType updateType(Integer id, DrinkAttributeType type);

    void deleteType(Integer id);

    DrinkAttributeOption createOption(Integer typeId, DrinkAttributeOption option);

    DrinkAttributeOption updateOption(Integer id, DrinkAttributeOption option);

    void deleteOption(Integer id);
}
