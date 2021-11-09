# mybatisplus-extra
基于MybatisPlus 3.4.3.2进行了一些扩展，依赖com:baomidoumy:batis-plus-boot-starter
1. 引入依赖（暂未上传至中央仓库）
```
	<dependency>
		<groupId>io.github.cisumer</groupId>
  		<artifactId>mybatisplus-extra</artifactId>
  		<version>0.1</version>
	</dependency>
```
2. 提供注解@ResultMap @QueryFor用于支持在实体类上实现@Result注解的功能
```
	@TableName("SYS_USER")
	@ResultMap("user")
	public class User{
		@TableField(exist=false)
		@QueryFor(column="uid",property="u_id")
		private Set<Role> roles;
	}
	
	@Select("select * from SYS_USER WHERE uid=#{uid}")
	@ResultMap(ResultMapParser.RESULTMAP_NAMESPACE+"Bid")
	User getUser(String uid);
```
Mapper接口中的@ResultMap为Mybatis的，实体上的@ResultMap为本项目提供的。
3. 增加FillHandler，并分别提供InsertFill和UpdateFill接口
```
	@TableField(fill=FieldFill.INSERT,jdbcType=JdbcType.TIMESTAMP)
	private Date createTime;
	
	@Component
	public class CreateTimeInsertFill implements InsertFill{
		@Override
		public void insertFill(MetaObject metaObject) {
			metaObject.setValue("createTime", new Date());
		}	
		@Override
		public boolean canFill(MetaObject object) {
			return object.hasSetter("createTime") && object.getGetterType("createTime") ==Date.class;
		}
	}
```
4. 使用ExtraSqlInjector扩展DefaultSqlInjector，可扩展自定义的AbstractMethod实现
