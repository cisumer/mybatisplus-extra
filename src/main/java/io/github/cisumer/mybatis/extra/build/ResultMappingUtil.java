package io.github.cisumer.mybatis.extra.build;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.reflection.Reflector;
import org.apache.ibatis.reflection.TypeParameterResolver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.apache.ibatis.type.UnknownTypeHandler;
import org.springframework.core.annotation.AnnotatedElementUtils;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;

import io.github.cisumer.mybatis.extra.annotations.QueryFor;

/**
 * 重写一些TableInfoHelper的方法，用于公开获取对应的ResultMapping
 * @author github.com/cisumer
 *
 */
public final class ResultMappingUtil {
	private static Configuration configuration;
	protected static GlobalConfig globalConfig;
	protected static GlobalConfig.DbConfig dbConfig;
	
	
	protected static void setConfiguration(Configuration configuration){
		ResultMappingUtil.configuration=configuration;
		globalConfig=GlobalConfigUtils.getGlobalConfig(configuration);
        dbConfig = globalConfig.getDbConfig();
	}
	
	/**
     * <p>
     * 获取Class对应的ResultMapping
     * </p>
     *
     * @param clazz        实体类
     * @param globalConfig 全局配置
     */
    public static List<ResultMapping> getResultMappings(Class<?> clazz) {        
        Reflector reflector = configuration.getReflectorFactory().findForClass(clazz);
        List<Field> list = TableInfoHelper.getAllFields(clazz);
        TableName table = clazz.getAnnotation(TableName.class);    
        //忽略的字段
        List<String> excludePropertyList = Collections.emptyList();
        if(table!=null){
	        String[] excludeProperty=table.excludeProperty();
	        if(excludeProperty!=null)
	        	excludePropertyList.addAll(Arrays.asList(excludeProperty));
        }
        List<ResultMapping> mappings=new ArrayList<ResultMapping>();
        
        for (Field field : list) {
            if (excludePropertyList.contains(field.getName()) || AnnotatedElementUtils.hasAnnotation(field, QueryFor.class)) {
                continue;
            }
            TableId tableId = field.getAnnotation(TableId.class);
            /* 主键ID 初始化 */
            if (tableId!=null) {
                mappings.add(buildTableId(configuration,field,tableId));
                continue;
            }
            /* 有 @TableField 注解的字段初始化 */
            mappings.add(getResultMapping(configuration,field,reflector));
        }
        return mappings;
    }

    
    @SuppressWarnings("unchecked")
	public static ResultMapping getResultMapping(Configuration configuration,Field field,Reflector reflector) {
        final TableField tableField = field.getAnnotation(TableField.class);
        String property=field.getName();
        String column="";
        Class<?> propertyType=reflector.getGetterType(property);
        JdbcType jdbcType=JdbcType.UNDEFINED;
        Class<? extends TypeHandler<?>> typeHandler=null;
        if(tableField != null){
        	column=tableField.value();        	
        	jdbcType=tableField.jdbcType();
        	typeHandler=(Class<? extends TypeHandler<?>>) tableField.typeHandler();
        	if (tableField.javaType()) {
                String javaType = null;
                TypeAliasRegistry registry = configuration.getTypeAliasRegistry();
                Map<String, Class<?>> typeAliases = registry.getTypeAliases();
                for (Map.Entry<String, Class<?>> entry : typeAliases.entrySet()) {
                    if (entry.getValue().equals(propertyType)) {
                        javaType = entry.getKey();
                        break;
                    }
                }
                if (javaType == null) {
                    javaType = propertyType.getName();
                    registry.registerAlias(javaType, propertyType);
                }
            }
        }

        if (StringUtils.isBlank(column)) {
            column = parseColumn(property);            
        }
        ResultMapping.Builder builder = new ResultMapping.Builder(configuration, property,
            StringUtils.getTargetColumn(column), propertyType);
        TypeHandlerRegistry registry = configuration.getTypeHandlerRegistry();
        if (jdbcType != null && jdbcType != JdbcType.UNDEFINED) {
            builder.jdbcType(jdbcType);
        }
        if (typeHandler != null && typeHandler != UnknownTypeHandler.class) {
            TypeHandler<?> typeHandlerInst = registry.getMappingTypeHandler(typeHandler);
            if (typeHandlerInst == null) {
            	typeHandlerInst = registry.getInstance(propertyType, typeHandler);
                // todo 这会有影响 registry.register(typeHandler);
            }
            builder.typeHandler(typeHandlerInst);
        }
        return builder.build();
    }
    public static Class<?> getFieldClass(Field field,Class<?> resultType){
		Type fieldType = TypeParameterResolver.resolveFieldType(field, resultType);
		Class<?> fieldClass=null;
		if(fieldType instanceof Class){
			fieldClass= ((Class<?>)fieldType);
		}else if(fieldType instanceof ParameterizedType){
			fieldClass=(Class<?>)((ParameterizedType) fieldType).getActualTypeArguments()[0];
		}
		if(fieldClass==null){
			return null;
		}
		return fieldClass;
    }
    protected static String tableName(Field field,Class<?> resultType) {
    	String tableName=null;
    	Class<?> fieldClass=getFieldClass(field, resultType);
		//解析目标表名
		TableName table = fieldClass.getAnnotation(TableName.class);
		String tablePrefix=ResultMappingUtil.dbConfig.getTablePrefix();				
		String schema=ResultMappingUtil.dbConfig.getSchema();
		if(table!=null && StringUtils.isNotBlank(table.value())){
			tableName=table.value();
		}else{
			tableName=ResultMappingUtil.parseColumn(fieldClass.getSimpleName());
		}
        if (table!=null && StringUtils.isNotBlank(table.schema())) {
            schema = table.schema();
        }
        if (StringUtils.isNotBlank(tablePrefix) && table==null || (table!=null && table.keepGlobalPrefix())) {
        	tableName = tablePrefix + tableName;
        }
        if (StringUtils.isNotBlank(schema)) {
            tableName = schema + StringPool.DOT + tableName;
        }
		return tableName;
	}
    /**
     * <p>
     * 主键属性初始化
     * </p>
     *
     * @param dbConfig  全局配置信息
     * @param tableInfo 表信息
     * @param field     字段
     * @param tableId   注解
     */
    private static ResultMapping buildTableId(Configuration config,  Field field,TableId id) {
        final String property = field.getName();        
        /* 字段 */
        String column = property;
        if (StringUtils.isNotBlank(id.value())) {
            column = id.value();
        } else {
            // 开启字段下划线申明
        	column=parseColumn(column);
        }
        final Class<?> keyType = field.getType();

		return new ResultMapping.Builder(config, property,StringUtils.getTargetColumn(column), keyType)
                .flags(Collections.singletonList(ResultFlag.ID)).build();
    }
        
    public static List<ResultMapping> parseCompositeColumnName(String columnName) {
        List<ResultMapping> composites = new ArrayList<>();
        if (columnName != null && (columnName.indexOf('=') > -1 || columnName.indexOf(',') > -1)) {
          StringTokenizer parser = new StringTokenizer(columnName, "{}=, ", false);
          while (parser.hasMoreTokens()) {
            String property = parser.nextToken();
            String column = parser.nextToken();
            ResultMapping complexResultMapping = new ResultMapping.Builder(
                configuration, property, column, configuration.getTypeHandlerRegistry().getUnknownTypeHandler()).build();
            composites.add(complexResultMapping);
          }
        }
        return composites;
    }
    /**
     * 全局判断是否驼峰是否大写
     * @param column
     * @return
     */
    public static String parseColumn(String column){
    	if(StringUtils.isBlank(column))
    		return null;
        // 开启字段下划线申明
        if (configuration.isMapUnderscoreToCamelCase()) {
            column = StringUtils.camelToUnderline(column);
        }
        // 全局大写命名
        if (dbConfig.isCapitalMode()) {
            column = column.toUpperCase();
        }
        return column;
    }
}
