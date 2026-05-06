package com.pobar.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProductAttributeMapper {

    @Delete("DELETE FROM product_attribute WHERE product_id = #{productId}")
    int deleteByProductId(Integer productId);

    @Insert("""
            INSERT INTO product_attribute (product_id, attribute_option_id)
            VALUES (#{productId}, #{optionId})
            """)
    int insert(@Param("productId") Integer productId, @Param("optionId") Integer optionId);

    @Select("""
            SELECT attribute_option_id FROM product_attribute
            WHERE product_id = #{productId}
            """)
    List<Integer> selectOptionIdsByProductId(Integer productId);

    @Select("""
            SELECT COUNT(1) FROM product_attribute
            WHERE product_id = #{productId}
              AND attribute_option_id IN
            <foreach item="id" collection="optionIds" open="(" separator="," close=")">
                #{id}
            </foreach>
            """)
    @Lang(org.apache.ibatis.scripting.xmltags.XMLLanguageDriver.class)
    int countMatchingOptions(@Param("productId") Integer productId,
                              @Param("optionIds") List<Integer> optionIds);
}
