package io.github.cisumer.mybatis.extra.build;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap.Builder;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.defaults.RawSqlSource;
import org.apache.ibatis.session.Configuration;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ReflectionUtils;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;

import io.github.cisumer.mybatis.extra.annotations.QueryFor;
import io.github.cisumer.mybatis.extra.annotations.ResultMap;
import io.github.cisumer.mybatis.extra.exception.NotUniqueException;
import io.github.cisumer.mybatis.extra.util.ClassUtils;

/**
 * 解析自定义的ResultMap注解，支持在实体类进行resultmap映射
 * @author github.com/cisumer
 *
 */
public class ResultMapParser {
	public static final String RESULTMAP_NAMESPACE="io.github.cisumer.mybatis.resultMap.";
	public static final String STATEMENT_NAMESPACE="io.github.cisumer.mybatis.queryFor.";
	
	
	@Value("${mybatis-plus.result-map.packages:}") 
	private String[] locations;
	private Configuration configuration;
	
	public ResultMapParser(Configuration configuration){
		this.configuration=configuration;
		ResultMappingUtil.setConfiguration(configuration);
	}
	
	/**
	 * 扫描类路径
	 * @param locations
	 * @throws  
	 */
	public void scan(){
		LogFactory.getLog("io.github.cisumer.mybatisplus-extra")
				.debug("解析自定义ResultMap注解:[" + String.join(",", locations) + "]");
		
		Set<Class<?>> mapperSet = ClassUtils.scanClasses(locations);
		for (Class<?> mapperClass : mapperSet) {
			parseResultMap(mapperClass);
		}
	}
	
	private void parseResultMap(Class<?> resultType){
		ResultMap anno=resultType.getAnnotation(ResultMap.class);
		if(anno==null)return;
		LoggerFactory.getLogger(ResultMapParser.class).debug("加载ResultMap:{}",resultType.toString());
		//设置ID
		String id=anno.id();
		if(StringUtils.isEmpty(id)){
			id=resultType.getSimpleName();
		}
		id=RESULTMAP_NAMESPACE+id;
		//如果已经注册抛出异常
		if(configuration.hasResultMap(id))
			throw new NotUniqueException(id);
		configuration.addResultMap(new Builder(configuration,id,resultType,applyResultMappings(resultType)).build());
	}

    private List<ResultMapping> applyResultMappings(Class<?> resultType) {
    	List<ResultMapping> mappings=new ArrayList<ResultMapping>();
    	//获取Class的ResultMappings
    	mappings.addAll(ResultMappingUtil.getResultMappings(resultType));

    	//获取一对一
    	List<Field> linkedFields=new ArrayList<Field>();
    	ReflectionUtils.doWithFields(resultType, linkedFields::add, this::hasQueryFor);
    	linkedFields.stream().map(field->{
    		QueryFor anno=field.getAnnotation(QueryFor.class);
    		String queryId=anno.select();
    		String column=anno.column();
    		List<ResultMapping> composites=null;
    		if(StringUtils.isNotBlank(column))
    			composites= ResultMappingUtil.parseCompositeColumnName(column);
    		else 
    			column=anno.property();
			String property=ResultMappingUtil.parseColumn(anno.property());
    		if(StringUtils.isBlank(queryId))
    			queryId=STATEMENT_NAMESPACE+resultType.getSimpleName()+".select"+field.getName();
    		if(!configuration.hasStatement(queryId,false)){//没有查询方法则创建一个
    			String tableName=ResultMappingUtil.tableName(field,resultType);
    			if(StringUtils.isBlank(tableName)){
    				LoggerFactory.getLogger(ResultMapParser.class).warn("无法解析{}的字段{}!",resultType.getName(),field.getName());
    				return null;
    			}
    	    	SqlSource sqlSource=new RawSqlSource(configuration, 
    	    			String.format("select * from %s where %s=#{id}",tableName,property), 
    	    			String.class);
    	    	MappedStatement stat=new MappedStatement.Builder(configuration,queryId,sqlSource,SqlCommandType.SELECT)
    	    			//添加一个内部的resultmap
    	    			.resultMaps(Arrays.asList(new Builder(configuration,queryId+"-Inline",ResultMappingUtil.getFieldClass(field, resultType),new ArrayList<>(),null).build()))
    	    			.build();
    			//添加一个Statement查询字表信息
    	    	configuration.addMappedStatement(stat);
    		}
    		return new ResultMapping.Builder(
    				configuration,
    				field.getName(),
    				ResultMappingUtil.parseColumn(anno.column()),
    				field.getType())
    				.nestedQueryId(queryId)
    				.columnPrefix(anno.columnPrefix())
    				.composites(composites)
    				.flags(new ArrayList<>())
    				.lazy(configuration.isLazyLoadingEnabled()).build();
    		
    	}).filter(Objects::nonNull).forEach(mappings::add);
    	return mappings;
    }
    
    private boolean hasQueryFor(AnnotatedElement elem){
    	return AnnotatedElementUtils.hasAnnotation(elem, QueryFor.class);
    }
}
