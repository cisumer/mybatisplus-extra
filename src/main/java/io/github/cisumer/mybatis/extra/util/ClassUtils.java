package io.github.cisumer.mybatis.extra.util;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.SystemPropertyUtils;

public abstract class ClassUtils {
	private static ResourcePatternResolver resolver= new PathMatchingResourcePatternResolver();
	private static MetadataReaderFactory metaReader = new CachingMetadataReaderFactory();
	
	public static Set<Class<?>> scanClasses(String... locations){
		Set<Class<?>> classes=new HashSet<Class<?>>();
		if(locations!=null){
				Stream.of(locations)
					.map(SystemPropertyUtils::resolvePlaceholders)
					.map(org.springframework.util.ClassUtils::convertClassNameToResourcePath)
					.map(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX::concat)
					.map(loc->loc.concat("/**/*.class"))
					.forEach(loc->{
						try {
							Resource[] resources = resolver.getResources(loc);
							if(resources!=null){
								Stream.of(resources).forEach(resource->{
									try {
										MetadataReader reader=metaReader.getMetadataReader(resource);
										classes.add(resolver.getClassLoader()
												.loadClass(reader.getClassMetadata().getClassName()));
									} catch (Exception e) {
										LoggerFactory.getLogger(ClassUtils.class).warn("资源[{}]加载失败！",resource.toString());
									}									
								});
							}
						} catch (IOException e) {
							LoggerFactory.getLogger(ClassUtils.class).warn("路径[{}]扫描失败！",loc);
						}
					});
		}		
		return classes;
	}
}
