package io.github.cisumer.mybatis.extra.fill;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.apache.ibatis.reflection.MetaObject;

import com.baomidou.mybatisplus.core.handlers.StrictFill;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;

public interface FillHandler {
	
	boolean canFill(MetaObject object);
	
    /**
     * 通用填充
     *
     * @param fieldName  java bean property name
     * @param fieldVal   java bean property value
     * @param metaObject meta object parameter
     */
    default FillHandler setFieldValByName(String fieldName, Object fieldVal, MetaObject metaObject) {
        if (Objects.nonNull(fieldVal) && metaObject.hasSetter(fieldName)) {
            metaObject.setValue(fieldName, fieldVal);
        }
        return this;
    }

    /**
     * get value from java bean by propertyName
     *
     * @param fieldName  java bean property name
     * @param metaObject parameter wrapper
     * @return 字段值
     */
    default Object getFieldValByName(String fieldName, MetaObject metaObject) {
        return metaObject.hasGetter(fieldName) ? metaObject.getValue(fieldName) : null;
    }

    /**
     * find the tableInfo cache by metaObject </p>
     * 获取 TableInfo 缓存
     *
     * @param metaObject meta object parameter
     * @return TableInfo
     * @since 3.3.0
     */
    default TableInfo findTableInfo(MetaObject metaObject) {
        return TableInfoHelper.getTableInfo(metaObject.getOriginalObject().getClass());
    }

    /**
     * @param metaObject metaObject meta object parameter
     * @return this
     * @since 3.3.0
     */
    default <T, E extends T> FillHandler strictInsertFill(MetaObject metaObject, String fieldName, Class<T> fieldType, E fieldVal) {
        return strictInsertFill(findTableInfo(metaObject), metaObject, Collections.singletonList(StrictFill.of(fieldName, fieldType, fieldVal)));
    }

    /**
     * @param metaObject metaObject meta object parameter
     * @return this
     * @since 3.3.0
     */
    default <T, E extends T> FillHandler strictInsertFill(MetaObject metaObject, String fieldName, Supplier<E> fieldVal, Class<T> fieldType) {
        return strictInsertFill(findTableInfo(metaObject), metaObject, Collections.singletonList(StrictFill.of(fieldName, fieldVal, fieldType)));
    }

    /**
     * @param metaObject metaObject meta object parameter
     * @return this
     * @since 3.3.0
     */
    default FillHandler strictInsertFill(TableInfo tableInfo, MetaObject metaObject, List<StrictFill<?, ?>> strictFills) {
        return strictFill(true, tableInfo, metaObject, strictFills);
    }

    /**
     * @param metaObject metaObject meta object parameter
     * @return this
     * @since 3.3.0
     */
    default <T, E extends T> FillHandler strictUpdateFill(MetaObject metaObject, String fieldName, Supplier<E> fieldVal, Class<T> fieldType) {
        return strictUpdateFill(findTableInfo(metaObject), metaObject, Collections.singletonList(StrictFill.of(fieldName, fieldVal, fieldType)));
    }

    /**
     * @param metaObject metaObject meta object parameter
     * @return this
     * @since 3.3.0
     */
    default <T, E extends T> FillHandler strictUpdateFill(MetaObject metaObject, String fieldName, Class<T> fieldType, E fieldVal) {
        return strictUpdateFill(findTableInfo(metaObject), metaObject, Collections.singletonList(StrictFill.of(fieldName, fieldType, fieldVal)));
    }

    /**
     * @param metaObject metaObject meta object parameter
     * @return this
     * @since 3.3.0
     */
    default FillHandler strictUpdateFill(TableInfo tableInfo, MetaObject metaObject, List<StrictFill<?, ?>> strictFills) {
        return strictFill(false, tableInfo, metaObject, strictFills);
    }

    /**
     * 严格填充,只针对非主键的字段,只有该表注解了fill 并且 字段名和字段属性 能匹配到才会进行填充(null 值不填充)
     *
     * @param insertFill  是否验证在 insert 时填充
     * @param tableInfo   cache 缓存
     * @param metaObject  metaObject meta object parameter
     * @param strictFills 填充信息
     * @return this
     * @since 3.3.0
     */
    default FillHandler strictFill(boolean insertFill, TableInfo tableInfo, MetaObject metaObject, List<StrictFill<?, ?>> strictFills) {
        if ((insertFill && tableInfo.isWithInsertFill()) || (!insertFill && tableInfo.isWithUpdateFill())) {
            strictFills.forEach(i -> {
                final String fieldName = i.getFieldName();
                final Class<?> fieldType = i.getFieldType();
                tableInfo.getFieldList().stream()
                    .filter(j -> j.getProperty().equals(fieldName) && fieldType.equals(j.getPropertyType()) &&
                        ((insertFill && j.isWithInsertFill()) || (!insertFill && j.isWithUpdateFill()))).findFirst()
                    .ifPresent(j -> strictFillStrategy(metaObject, fieldName, i.getFieldVal()));
            });
        }
        return this;
    }

    /**
     * 填充策略,默认有值不覆盖,如果提供的值为null也不填充
     *
     * @param metaObject metaObject meta object parameter
     * @param fieldName  java bean property name
     * @param fieldVal   java bean property value of Supplier
     * @return this
     * @since 3.3.0
     */
    default FillHandler fillStrategy(MetaObject metaObject, String fieldName, Object fieldVal) {
        if (getFieldValByName(fieldName, metaObject) == null) {
            setFieldValByName(fieldName, fieldVal, metaObject);
        }
        return this;
    }

    /**
     * 严格模式填充策略,默认有值不覆盖,如果提供的值为null也不填充
     *
     * @param metaObject metaObject meta object parameter
     * @param fieldName  java bean property name
     * @param fieldVal   java bean property value of Supplier
     * @return this
     * @since 3.3.0
     */
    default FillHandler strictFillStrategy(MetaObject metaObject, String fieldName, Supplier<?> fieldVal) {
        if (metaObject.getValue(fieldName) == null) {
            Object obj = fieldVal.get();
            if (Objects.nonNull(obj)) {
                metaObject.setValue(fieldName, obj);
            }
        }
        return this;
    }
    
}
