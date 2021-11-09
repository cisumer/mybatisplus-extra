package io.github.cisumer.mybatis.extra;

import java.util.List;

import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusPropertiesCustomizer;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;

import io.github.cisumer.mybatis.extra.build.ResultMapParser;
import io.github.cisumer.mybatis.extra.fill.FillHandler;
import io.github.cisumer.mybatis.extra.fill.InsertFill;
import io.github.cisumer.mybatis.extra.fill.UpdateFill;
import io.github.cisumer.mybatis.extra.injector.ExtraSqlInjector;

/**
 * 基于SpringBoot 自动加载配置
 *
 */
@Configuration
public class ExtraMybatisPlusConfiguration {
	/**
	 * 扩展默认的SqlInjector
	 */
	@Bean
	public ExtraSqlInjector sqlInjector() {
		return new ExtraSqlInjector();
	}

	/**
	 * 加载自定义的注解@ResultMap
	 * 
	 * @return
	 */
		
	@Configuration
	@ConditionalOnProperty(name = "mybatis-plus.result-map.enabled", havingValue = "true", matchIfMissing = true)
	class ParseResultMapConfiguration implements MybatisPlusPropertiesCustomizer{
		@Bean
		ResultMapParser parser(){
			return new ResultMapParser();
		}
		
		@Override
		public void customize(MybatisPlusProperties properties) {
			parser().setProperties(properties).scan();
		}
	}
	/**
	 * 扩展支持多条件fill填充
	 * 
	 * @param insertFills
	 * @param updateFills
	 * @return
	 */
	@Configuration
	@AutoConfigureBefore(MybatisPlusAutoConfiguration.class)
	class MetaObjectHandlerConfiguration implements MetaObjectHandler{
		@Autowired 
		private List<FillHandler> fills;
		
		@Override
		public void insertFill(MetaObject metaObject) {
			fills.stream()
					.filter(fill -> InsertFill.class.isAssignableFrom(fill.getClass()) && fill.canFill(metaObject))
					.map(fill -> (InsertFill) fill).forEach(fill -> fill.insertFill(metaObject));
		}

		@Override
		public void updateFill(MetaObject metaObject) {
			fills.stream()
					.filter(fill -> UpdateFill.class.isAssignableFrom(fill.getClass()) && fill.canFill(metaObject))
					.map(fill -> (UpdateFill) fill).forEach(fill -> fill.updateFill(metaObject));
		}
	}
}
