package io.github.cisumer.mybatis.extra.exception;

public class NotUniqueException extends RuntimeException{
	public String id;
	
	public NotUniqueException(String id){
		this.id=id;
	}
	
	public String getMessage(){
		return "["+id+"] is not Unique!";
	}
}
