package io.github.cisumer.mybatis.extra.fill;

import org.apache.ibatis.reflection.MetaObject;

public interface InsertFill extends FillHandler{
    /**
     * 插入元对象字段填充（用于插入时对公共字段的填充）
     *
     * @param metaObject 元对象
     */
    void insertFill(MetaObject metaObject);
}
