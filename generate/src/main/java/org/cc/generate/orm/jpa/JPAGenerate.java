package org.cc.generate.orm.jpa;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.lang.model.element.Modifier;
import javax.swing.text.html.parser.Entity;
import javax.xml.crypto.Data;

import org.cc.generate.entity.DatabaseReflect;

import com.mysql.cj.api.xdevapi.Column;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;

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
	private static String databaseDriver;
	/**
	 * 数据库url
	 */
	private static String databaseUrl;
	/**
	 * 数据库用户名
	 */
	private static String databaseUserName;
	/**
	 * 数据库密码
	 */
	private static String databasePassword;
	/**
	 * 数据库名字
	 */
	private static String databaseName;
	/**
	 * 实体名字
	 */
	private static String entityName;
	/**
	 * 表名字
	 */
	private static String tableName;
	/**
	 * 表名字
	 */
	private static String classAuthor;
	/**
	 * 表名字
	 */
	private static String classVersion;

	private static final String filePath = "D://test";

	private final DateTimeFormatter dateTimeFormater = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	static {
		jdbcJavaTypes.put(Types.BIT, Boolean.class);
		jdbcJavaTypes.put(Types.TINYINT, Byte.class);
		jdbcJavaTypes.put(Types.SMALLINT, Short.class);
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
		jdbcJavaTypes.put(Types.TIMESTAMP, LocalDateTime.class);
		// 读取properties配置文件
		URL url = Thread.currentThread().getContextClassLoader().getResource("Generator.properties");
		try (InputStream in = new FileInputStream(url.getFile());) {
			Properties properties = new Properties();
			properties.load(in);
			entityName = properties.getProperty("entity.name");
			tableName = properties.getProperty("table.name");
			databaseDriver = properties.getProperty("database.driver");
			databaseUrl = properties.getProperty("database.url");
			databaseUserName = properties.getProperty("database.username");
			databasePassword = properties.getProperty("database.password");
			databaseName = properties.getProperty("database.name");
			classAuthor = properties.getProperty("class.author");
			classVersion = properties.getProperty("class.version");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			Class.forName(databaseDriver);
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		}
		ResultSet rs = null;
		ResultSet primaryRs = null;
		try (Connection conn = DriverManager.getConnection(databaseUrl, databaseUserName, databasePassword);) {
			DatabaseMetaData databaseMetaData = conn.getMetaData();
			rs = databaseMetaData.getColumns(databaseName, null, tableName, "%");
			primaryRs = databaseMetaData.getPrimaryKeys(databaseName, null, tableName);
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
	 * 生成实体
	 * 
	 * @param path
	 * @param className
	 * @return
	 */
	private boolean generateEntity() {
		Builder typeSpec = TypeSpec.classBuilder(entityName).addModifiers(Modifier.PUBLIC);
		conlumnList.forEach(e -> {
			com.squareup.javapoet.FieldSpec.Builder fieldBuilder = FieldSpec.builder(e.getJavaType(), e.getFieldName())
					.addJavadoc(e.getAnnotation()).addJavadoc("\n");
			fieldBuilder.addModifiers(Modifier.PRIVATE);
			AnnotationSpec annotationBuilder = AnnotationSpec.builder(Column.class)
					.addMember("name", "$S", e.getConlumnName()).build();
			fieldBuilder.addAnnotation(annotationBuilder);
			typeSpec.addField(fieldBuilder.build());
		});
		typeSpec.addJavadoc("实体<br>\n@author " + classAuthor + "\n@date " + dateTimeFormater.format(LocalDateTime.now())
				+ "\n@since " + classVersion + "\n");
		typeSpec.addAnnotation(Entity.class);
		AnnotationSpec tableAnnotationBuilder = AnnotationSpec.builder(Column.class)
				.addMember("name", "$S", tableName).build();
		typeSpec.addAnnotation(tableAnnotationBuilder);
		typeSpec.addAnnotation(Data.class);
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
	 * 生成实体
	 * 
	 * @param path
	 * @param className
	 * @return
	 */
	/*
	 * private boolean generateRepository() { Builder typeSpec =
	 * TypeSpec.classBuilder(entityName +
	 * "Repostitory").addModifiers(Modifier.PUBLIC);
	 * typeSpec.addSuperinterface(ParameterizedTypeName.get(JpaRepository.class,
	 * Long.class)); typeSpec.addJavadoc("实体<br>\n@author " + classAuthor +
	 * "\n@date " + dateTimeFormater.format(LocalDateTime.now()) + "\n@since " +
	 * classVersion + "\n"); TypeSpec generateClass = typeSpec.build(); JavaFile
	 * javaFile = JavaFile.builder("domain", generateClass).build(); try {
	 * javaFile.writeTo(Paths.get(filePath)); } catch (IOException e) {
	 * e.printStackTrace(); } return true; }
	 */

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
	 * 一键生成实体和mapper和xml文件
	 */
	public void oneTouch() {
		this.generateEntity();
	}

	public static void main(String[] args) {

		/**
		 * 一键生成实体和mapper和xml文件
		 */

		JPAGenerate propertiesAnalyze = new JPAGenerate();
		propertiesAnalyze.oneTouch();

	}
}
