package com.pobar.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pobar.entity.DrinkAttributeOption;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DrinkAttributeOptionMapper extends BaseMapper<DrinkAttributeOption> {

    @Select("SELECT * FROM drink_attribute_option WHERE attribute_type_id = #{typeId} AND is_active = 1 ORDER BY display_order")
    List<DrinkAttributeOption> selectByTypeId(Integer typeId);
}
