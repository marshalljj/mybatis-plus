package com.majian.mybatis.plus;

import org.apache.ibatis.annotations.UpdateProvider;

public interface CommonMapper<T> {
    @UpdateProvider(type = CommonProvider.class, method = "genInsertSql")
    int insert(T record);

    @UpdateProvider(type = CommonProvider.class, method = "genUpdateSql")
    int update(T record);
}
