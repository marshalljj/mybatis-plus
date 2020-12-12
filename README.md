# mybatis-plus
# mybatis-plus是什么
是否已经厌倦了mybatis-generator? 每当表结构需要变更时候就要进行mapper.xml重新生成, 如果存在自定义当sql的话那就更加痛苦了。
mybatis-plus就是用来代替mybatis-generator的一种方案，实现了通用的insert/update 语句生成功能, 降低表结构变更带来的影响。

# quickstart
1. 导入依赖
```
<dependency>
  <groupId>com.majian.mybatis</groupId>
  <artifactId>mybatis-plus</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```
2. 使用
- 在数据类中添加对应注解
    - @Table映射表名
    - @Column映射列名,不建议使用（未对select语句进行处理）。不使用时默认字段名转为下划线。
    - @Transient 表示该字段不需要持久化，生成insert,update语句时忽略该字段。
    - @Version 表示乐观锁版本号，生成update语句时会 进行 `set version_column=version_column+1 ... where version_column =#{versionField}`的
处理
    - @Id表示主键, update时会以此定位数据。

```java
 @Table("user")
 public class User {
      @Id
      private Integer id;
      private Integer age;
      @Column("real_name")
      private Integer realName;
      @Version
      private Integer version;
      @Transient
      private Date updateTime;
  }
```
- 继承通用mapper, 可使用 insert/update 方法
```java
  public interface UserMapper extends CommonMapper<User> {

  }
 
  public class UserDao {
      private UserMapper userMapper;
 
      public int insert(User user) {
          return userMapper.insert(user);
      }
 
  }
```

- 默认主键生成方式为`@Options(useGeneratedKeys = true, keyProperty="id")`,可通过@Overeride覆盖默认的主键生成策略,此时需要加上`@InsertProvider(type = CommonProvider.class, method = "genInsertSQL")`
``` java
public interface UserMapper extends CommonMapper<User> {
    @InsertProvider(type = CommonProvider.class, method = "genInsertSQL")
    @Options(useGeneratedKeys = true, keyProperty="xx")
    @Override
    int insert(T record);
}
      
```
