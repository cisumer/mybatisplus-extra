package io.github.cisumer.mybatis.extra.exception;

public class TableInfoException extends RuntimeException{
	private Class<?> table;
	public TableInfoException(Class<?> clazz,Throwable ex){
		super(ex);
		this.table=clazz;
	}
	public String getMessage(){
		return String.format("Can`t parse [%s] to TableInfo!", table.getName());
	}
}
