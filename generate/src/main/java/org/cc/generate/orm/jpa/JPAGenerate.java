package org.cc.generate.orm.jpa;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Modifier;
import javax.persistence.Column;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.swing.text.html.parser.Entity;

import org.cc.generate.entity.DatabaseReflect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

public class JPAGenerate {

	/**
	 * 数据库字段类型对应的java的类型
	 */
	private static Map<Integer, Class<?>> jdbcJavaTypes = new HashMap<>();

	/**
	 * 字段实体集合
	 */
	private static List<DatabaseReflect> conlumnList = new ArrayList<>();

	/**
	 * 主键
	 */
	private static List<DatabaseReflect> primaryKeyList = new ArrayList<>();
	/**
	 * 数据库驱动
	 */
	private static String databaseDriver = "com.mysql.cj.jdbc.Driver";
	/**
	 * 数据库url
	 */
	private static String databaseUrl = "";
	/**
	 * 数据库用户名
	 */
	private static String databaseUserName = "";
	/**
	 * 数据库密码
	 */
	private static String databasePassword = "";
	/**
	 * 数据库名字
	 */
	private static String databaseName = "";
	/**
	 * 实体名字
	 */
	private static String entityName = "TaxTaxpayer";
	/**
	 * 表名字
	 */
	private static String tableName = "t_tax_taxpayer";
	/**
	 * 作者
	 */
	private static String classAuthor = "chenxianjing";
	/**
	 * 版本号
	 */
	private static String classVersion = "1.0.0";
	/**
	 * 表描述信息
	 */
	private static String tableDescription = "";

	private static final String filePath = "D://test";

	private final DateTimeFormatter dateTimeFormater = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	static {
		jdbcJavaTypes.put(Types.BIT, Boolean.class);
		jdbcJavaTypes.put(Types.TINYINT, Integer.class);
		jdbcJavaTypes.put(Types.SMALLINT, Integer.class);
		jdbcJavaTypes.put(Types.INTEGER, Integer.class);
		jdbcJavaTypes.put(Types.BIGINT, Long.class);
		jdbcJavaTypes.put(Types.REAL, Float.class);
		jdbcJavaTypes.put(Types.FLOAT, Double.class);
		jdbcJavaTypes.put(Types.DOUBLE, Double.class);
		jdbcJavaTypes.put(Types.NUMERIC, BigDecimal.class);
		jdbcJavaTypes.put(Types.DECIMAL, BigDecimal.class);
		jdbcJavaTypes.put(Types.CHAR, String.class);
		jdbcJavaTypes.put(Types.VARCHAR, String.class);
		jdbcJavaTypes.put(Types.LONGVARCHAR, String.class);
		jdbcJavaTypes.put(Types.DATE, LocalDate.class);
		jdbcJavaTypes.put(Types.TIME, LocalTime.class);
		jdbcJavaTypes.put(Types.TIMESTAMP, Date.class);
		try {
			Class.forName(databaseDriver);
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		}
		ResultSet rs = null;
		ResultSet primaryRs = null;
		ResultSet tableRs = null;
		try (Connection conn = DriverManager.getConnection(databaseUrl, databaseUserName, databasePassword);) {
			DatabaseMetaData databaseMetaData = conn.getMetaData();
			rs = databaseMetaData.getColumns(databaseName, null, tableName, "%");
			primaryRs = databaseMetaData.getPrimaryKeys(databaseName, null, tableName);
			tableRs = databaseMetaData.getTables(tableName, null, null, new String[] { "TABLE" });
			tableDescription = tableRs.getString("REMARKS");
			String conlumnName = null;
			DatabaseReflect databaseReflect = null;
			while (rs.next()) {
				conlumnName = rs.getString("COLUMN_NAME");
				databaseReflect = new DatabaseReflect();
				databaseReflect.setConlumnName(conlumnName);
				databaseReflect.setFieldName(underlineToCamel(conlumnName));
				databaseReflect.setJavaType(jdbcJavaTypes.get(rs.getInt("DATA_TYPE")));
				databaseReflect.setAnnotation(rs.getString("REMARKS"));
				conlumnList.add(databaseReflect);
			}
			while (primaryRs.next()) {
				databaseReflect = new DatabaseReflect();
				conlumnName = primaryRs.getString("COLUMN_NAME");
				databaseReflect.setConlumnName(conlumnName);
				databaseReflect.setFieldName(underlineToCamel(conlumnName));
				primaryKeyList.add(databaseReflect);
			}
			if (primaryKeyList.isEmpty()) {
				primaryKeyList.add(conlumnList.get(0));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				rs.close();
				primaryRs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 把待下划线的字符串转化为驼峰格式的字符串
	 * 
	 * @param param
	 * @return
	 */
	public static String underlineToCamel(String param) {
		String[] params = param.split("_");
		int len = params.length;
		if (len > 1) {
			StringBuilder result = new StringBuilder();
			String str = null;
			result.append(params[0]);
			for (int i = 1; i < len; i++) {
				str = params[i];
				result.append(str.replaceFirst("^[a-z]", String.valueOf(str.charAt(0)).toUpperCase()));
			}
			return result.toString();
		} else {
			return param;
		}
	}

	/**
	 * 生成实体
	 * 
	 * @param path
	 * @param className
	 * @return
	 */
	private boolean generateEntity() {
		Builder typeSpec = TypeSpec.classBuilder(entityName + "Entity").addModifiers(Modifier.PUBLIC);
		conlumnList.forEach(e -> {
			com.squareup.javapoet.FieldSpec.Builder fieldBuilder = FieldSpec.builder(e.getJavaType(), e.getFieldName())
					.addJavadoc(e.getAnnotation()).addJavadoc("\n");
			fieldBuilder.addModifiers(Modifier.PRIVATE);
			AnnotationSpec annotationBuilder = AnnotationSpec.builder(Column.class)
					.addMember("name", "$S", e.getConlumnName()).build();
			fieldBuilder.addAnnotation(annotationBuilder);
			typeSpec.addField(fieldBuilder.build());
		});
		typeSpec.addJavadoc(tableDescription + "<br>\n@author " + classAuthor + "\n@date " + dateTimeFormater.format(LocalDateTime.now())
				+ "\n@since " + classVersion + "\n");
		AnnotationSpec tableAnnotationBuilder = AnnotationSpec.builder(Table.class).addMember("name", "$S", tableName)
				.build();
		typeSpec.addAnnotation(tableAnnotationBuilder);
		typeSpec.addAnnotation(AnnotationSpec.builder(Entity.class).build());
		typeSpec.addAnnotation(Data.class);
		//使用没有引用包的类
		ClassName dynamicInsert = ClassName.get("org.hibernate.annotations",
				"DynamicInsert");
		typeSpec.addAnnotation(dynamicInsert);
		TypeSpec generateClass = typeSpec.build();
		JavaFile javaFile = JavaFile.builder("domain", generateClass).build();
		try {
			javaFile.writeTo(Paths.get(filePath));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * 生成Repository
	 * 
	 * @param path
	 * @param className
	 * @return
	 */
	private boolean generateRepository() {
		Builder typeSpec = TypeSpec.interfaceBuilder(entityName + "Repository").addModifiers(Modifier.PUBLIC);
		ClassName jpaRepository = ClassName.get("org.springframework.data.jpa.repository", "JpaRepository");
		ClassName entityClass = ClassName.get("domain.", entityName + "Entity");
		ClassName jpaSpecificationExecutor = ClassName.get("org.springframework.data.jpa.repository",
				"JpaSpecificationExecutor");
		ClassName primary = ClassName.get("java.lang", primaryKeyList.get(0).getJavaType().getSimpleName());
		ParameterizedTypeName.get(jpaRepository, entityClass, primary);
		typeSpec.addSuperinterface(ParameterizedTypeName.get(jpaRepository, entityClass, primary));
		typeSpec.addSuperinterface(ParameterizedTypeName.get(jpaSpecificationExecutor, entityClass));
		// 删除方法
		String primaryString = primaryKeyList.get(0).getFieldName();
		String sql = "UPDATE %S p SET p.delFlag = 0 WHERE p.%S = :%S";
		com.squareup.javapoet.MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("removeOne");
		methodBuilder.addAnnotation(Modifying.class);
		AnnotationSpec queryAnnotationBuilder = AnnotationSpec.builder(Query.class)
				.addMember("value", "$", String.format(sql, entityName + "Entity", primaryString, primaryString))
				.build();
		methodBuilder.addAnnotation(queryAnnotationBuilder);
		com.squareup.javapoet.ParameterSpec.Builder paramBuilder = ParameterSpec
				.builder(primaryKeyList.get(0).getJavaType(), primaryString);
		paramBuilder.addAnnotation(AnnotationSpec.builder(Param.class).addMember("value", "$", primaryString).build());
		methodBuilder.addParameter(paramBuilder.build());
		typeSpec.addMethod(methodBuilder.build());
		// 根据主键获取未删除的数据
		sql = "select u from %S u where u.delFlag = 1 and u.%S = :%S";
		methodBuilder = MethodSpec.methodBuilder("getOne");
		methodBuilder.addAnnotation(Modifying.class);
		queryAnnotationBuilder = AnnotationSpec.builder(Query.class)
				.addMember("value", "$", String.format(sql, entityName + "Entity", primaryString, primaryString))
				.build();
		methodBuilder.addAnnotation(queryAnnotationBuilder);
		methodBuilder.addParameter(paramBuilder.build());
		typeSpec.addMethod(methodBuilder.build());

		typeSpec.addJavadoc(tableDescription + "repository<br>\n@author " + classAuthor + "\n@date "
				+ dateTimeFormater.format(LocalDateTime.now()) + "\n@since " + classVersion + "\n");
		TypeSpec generateClass = typeSpec.build();
		JavaFile javaFile = JavaFile.builder("repository", generateClass).build();
		try {
			javaFile.writeTo(Paths.get(filePath));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * 生成实体
	 * 
	 * @param path
	 * @param className
	 * @return
	 */
	private boolean generateIModel() {
		Builder typeSpec = TypeSpec.classBuilder(entityName + "IModel").addModifiers(Modifier.PUBLIC);
		conlumnList.forEach(e -> {
			com.squareup.javapoet.FieldSpec.Builder fieldBuilder = FieldSpec.builder(e.getJavaType(), e.getFieldName())
					.addJavadoc(e.getAnnotation()).addJavadoc("\n");
			fieldBuilder.addModifiers(Modifier.PRIVATE);
			ClassName apiModelProperty = ClassName.get("io.swagger.annotations",
					"ApiModelProperty");
			AnnotationSpec annotationBuilder = AnnotationSpec.builder(apiModelProperty)
					.addMember("value", "$S", e.getAnnotation()).build();
			fieldBuilder.addAnnotation(annotationBuilder);
			typeSpec.addField(fieldBuilder.build());
		});
		typeSpec.addJavadoc(tableDescription + "<br>\n@author " + classAuthor + "\n@date " + dateTimeFormater.format(LocalDateTime.now())
				+ "\n@since " + classVersion + "\n");
		ClassName apiModel = ClassName.get("io.swagger.annotations",
				"ApiModel");
		AnnotationSpec apiModelAnnotationBuilder = AnnotationSpec.builder(apiModel)
				.addMember("value", "$S", tableName).build();
		typeSpec.addAnnotation(apiModelAnnotationBuilder);
		typeSpec.addAnnotation(Data.class);
		TypeSpec generateClass = typeSpec.build();
		JavaFile javaFile = JavaFile.builder("in", generateClass).build();
		try {
			javaFile.writeTo(Paths.get(filePath));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * 生成实体
	 * 
	 * @param path
	 * @param className
	 * @return
	 */
	private boolean generateOModel() {
		Builder typeSpec = TypeSpec.classBuilder(entityName + "OModel").addModifiers(Modifier.PUBLIC);
		conlumnList.forEach(e -> {
			com.squareup.javapoet.FieldSpec.Builder fieldBuilder = FieldSpec.builder(e.getJavaType(), e.getFieldName())
					.addJavadoc(e.getAnnotation()).addJavadoc("\n");
			fieldBuilder.addModifiers(Modifier.PRIVATE);
			ClassName apiModelProperty = ClassName.get("io.swagger.annotations",
					"ApiModelProperty");
			AnnotationSpec annotationBuilder = AnnotationSpec.builder(apiModelProperty)
					.addMember("value", "$S", e.getAnnotation()).build();
			fieldBuilder.addAnnotation(annotationBuilder);
			typeSpec.addField(fieldBuilder.build());
		});
		typeSpec.addJavadoc(tableDescription + "<br>\n@author " + classAuthor + "\n@date " + dateTimeFormater.format(LocalDateTime.now())
				+ "\n@since " + classVersion + "\n");
		ClassName apiModel = ClassName.get("io.swagger.annotations",
				"ApiModel");
		AnnotationSpec apiModelAnnotationBuilder = AnnotationSpec.builder(apiModel)
				.addMember("value", "$S", tableName).build();
		typeSpec.addAnnotation(apiModelAnnotationBuilder);
		typeSpec.addAnnotation(Data.class);
		TypeSpec generateClass = typeSpec.build();
		JavaFile javaFile = JavaFile.builder("out", generateClass).build();
		try {
			javaFile.writeTo(Paths.get(filePath));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * 生成实体
	 * 
	 * @param path
	 * @param className
	 * @return
	 */
	private boolean generateDynamicRepository() {
		Builder typeSpec = TypeSpec.classBuilder("DynamicRepository<T>").addModifiers(Modifier.PUBLIC);
		com.squareup.javapoet.FieldSpec.Builder fieldBuilder = FieldSpec.builder(EntityManager.class, "entityManager");
		fieldBuilder.addModifiers(Modifier.PRIVATE);
		fieldBuilder.addAnnotation(PersistenceContext.class);
		typeSpec.addField(fieldBuilder.build());
		typeSpec.addJavadoc(tableDescription + "\n@author " + classAuthor + "\n@date " + dateTimeFormater.format(LocalDateTime.now())
				+ "\n@since " + classVersion + "\n");
		typeSpec.addAnnotation(Repository.class);
		typeSpec.addAnnotation(Slf4j.class);
		com.squareup.javapoet.MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("updateDynamicByPrimaryKey");
		methodBuilder.addCode("Class entityClass = t.getClass()");
		methodBuilder.addCode("CriteriaUpdate update = em.getCriteriaBuilder().createCriteriaUpdate(entityClass);");
		methodBuilder.addCode("Root<T> root = update.from(entityClass)");
		methodBuilder.addCode("Field[] fields = entityClass.getDeclaredFields()");
		methodBuilder.addCode("String primaryKeyName = null;");
		methodBuilder.addCode("try {");
		methodBuilder.addCode("for (Field field : fields) {");
		methodBuilder.addCode("field.setAccessible(true);");
		methodBuilder.addCode("Column column = field.getAnnotation(Column.class);");
		methodBuilder.addCode("Id id = field.getAnnotation(Id.class);");
		methodBuilder.addCode("if (!Objects.isNull(column) && Objects.isNull(id)) {");
		methodBuilder.addCode("Object param = field.get(t);");
		methodBuilder.addCode("if (!Objects.isNull(param)) {");
		methodBuilder.addCode("update.set(root.get(field.getName()), param);");
		methodBuilder.addCode("}");
		methodBuilder.addCode("}");
		methodBuilder.addCode("if(!Objects.isNull(id)){");
		methodBuilder.addCode("primaryKeyName = field.getName();");
		methodBuilder.addCode("}");
		methodBuilder.addCode("}");
		methodBuilder.addCode("if(StringUtils.isBlank(primaryKeyName)){");
		methodBuilder.addCode("log.error(\"根据主键更新表失败:主键不存在\");");
		methodBuilder.addCode("return 0;");
		methodBuilder.addCode("}");
		methodBuilder.addCode("Field primaryField = entityClass.getDeclaredField(primaryKeyName);");
		methodBuilder.addCode("update.where(criteriaBuilder.equal(root.get(primaryKeyName), primaryField.get(t)));");
		methodBuilder.addCode("Query query = em.createQuery(update);");
		methodBuilder.addCode("return query.executeUpdate();");
		methodBuilder.addCode("return query.executeUpdate();");
		methodBuilder.addCode("} catch (IllegalAccessException | NoSuchFieldException e) {");
		methodBuilder.addCode("log.error(\"反射获取属性失败\", e);");
		methodBuilder.addCode("}");
		methodBuilder.addCode("return 0;");
		TypeSpec generateClass = typeSpec.build();
		JavaFile javaFile = JavaFile.builder("repository", generateClass).build();
		try {
			javaFile.writeTo(Paths.get(filePath));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * 生成实体
	 * 
	 * @param path
	 * @param className
	 * @return
	 */
	private boolean generateService() {
		Builder typeSpec = TypeSpec.classBuilder(entityName + "Service").addModifiers(Modifier.PUBLIC);
		ClassName repository = ClassName.get("repository", entityName + "Repository");
		ClassName dynamicRepository = ClassName.get("repository", "DynamicRepository");
		com.squareup.javapoet.FieldSpec.Builder fieldBuilder = FieldSpec.builder(repository, entityName + "Repository");
		fieldBuilder.addModifiers(Modifier.PRIVATE);
		fieldBuilder.addAnnotation(AnnotationSpec.builder(Autowired.class).build());
		typeSpec.addField(fieldBuilder.build());
		fieldBuilder = FieldSpec.builder(dynamicRepository, "dynamicRepository");
		fieldBuilder.addModifiers(Modifier.PRIVATE);
		fieldBuilder.addAnnotation(AnnotationSpec.builder(Autowired.class).build());
		typeSpec.addField(fieldBuilder.build());
		typeSpec.addJavadoc(tableDescription + "<br>\n@author " + classAuthor + "\n@date " + dateTimeFormater.format(LocalDateTime.now())
				+ "\n@since " + classVersion + "\n");
		typeSpec.addAnnotation(Service.class);
		AnnotationSpec tableAnnotationBuilder = AnnotationSpec.builder(ApiModel.class)
				.addMember("value", "$S", tableName).build();
		typeSpec.addAnnotation(tableAnnotationBuilder);
		typeSpec.addAnnotation(Data.class);
		typeSpec.addMethod(new GenerateRemove().remove(entityName, primaryKeyList.get(0)).build());
		GenerateSave generateSave = new GenerateSave();
		typeSpec.addMethod(generateSave.update(entityName, primaryKeyList.get(0).getFieldName()).build());
		typeSpec.addMethod(generateSave.save(entityName, primaryKeyList.get(0).getFieldName()).build());
		typeSpec.addMethod(new GenerateFindOne().findOne(entityName, primaryKeyList.get(0)).build());
		TypeSpec generateClass = typeSpec.build();
		JavaFile javaFile = JavaFile.builder("service", generateClass).build();
		try {
			javaFile.writeTo(Paths.get(filePath));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * 一键生成实体和mapper和xml文件
	 */
	public void oneTouch() {
		this.generateEntity();
		this.generateIModel();
		this.generateOModel();
		this.generateRepository();
		this.generateDynamicRepository();
		this.generateService();
	}

	public static void main(String[] args) {

		/**
		 * 一键生成实体和mapper和xml文件
		 */

		JPAGenerate propertiesAnalyze = new JPAGenerate();
		propertiesAnalyze.oneTouch();

	}
}
