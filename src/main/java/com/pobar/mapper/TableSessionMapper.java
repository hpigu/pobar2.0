package com.pobar.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pobar.entity.TableSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TableSessionMapper extends BaseMapper<TableSession> {

    @Select("SELECT * FROM table_session WHERE qr_token = #{token}")
    TableSession selectByToken(String token);
}
