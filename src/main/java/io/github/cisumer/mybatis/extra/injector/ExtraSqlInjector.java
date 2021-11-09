package io.github.cisumer.mybatis.extra.injector;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.core.metadata.TableInfo;

public class ExtraSqlInjector extends DefaultSqlInjector {
	
	@Autowired(required=false)
	private List<AbstractMethod> methods;
	
	@Override
	public List<AbstractMethod> getMethodList(Class<?> mapperClass, TableInfo tableInfo) {
		List<AbstractMethod> methods=super.getMethodList(mapperClass, tableInfo);
		if(this.methods!=null && this.methods.size()>0)
			methods.addAll(this.methods);
		return methods;
	}
}
