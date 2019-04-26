package org.cc.generate.orm.mybatis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
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

import org.cc.generate.entity.DatabaseReflect;
import org.cc.generate.util.StringUtils;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;

public class Generate {

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
	 * 实体包路径
	 */
	private static String entityPackagePath;
	/**
	 * 实体路径路径
	 */
	private static String entityFilePath;
	/**
	 * mapper包路径
	 */
	private static String mapperPackagePath;
	/**
	 * mapper文件路径
	 */
	private static String mapperFilePath;
	/**
	 * xml路径
	 */
	private static String xmlPath;
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
			entityPackagePath = properties.getProperty("entity.package.path");
			entityFilePath = properties.getProperty("entity.file.path");
			mapperPackagePath = properties.getProperty("mapper.package.path");
			mapperFilePath = properties.getProperty("mapper.file.path");
			xmlPath = properties.getProperty("xml.path");
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
			typeSpec.addField(fieldBuilder.build());
		});
		typeSpec.addJavadoc("实体<br>\n@author " + classAuthor + "\n@date " + dateTimeFormater.format(LocalDateTime.now())
				+ "\n@since " + classVersion + "\n");
		TypeSpec generateClass = typeSpec.build();
		JavaFile javaFile = JavaFile.builder(entityPackagePath, generateClass).build();
		try {
			javaFile.writeTo(Paths.get(entityFilePath));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	private boolean generateMapper() {
		ClassName entity = ClassName.get(entityPackagePath, entityName);
		ClassName list = ClassName.get("java.util", "List");
		TypeName listEntity = ParameterizedTypeName.get(list, entity);
		String paramName = StringUtils.firstLetterToLowwer(entityName);
		MethodSpec queryMethod = MethodSpec.methodBuilder("listAll").returns(listEntity).addJavadoc("查询所有数据\n")
				.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT).build();
		MethodSpec deleteMethod = MethodSpec.methodBuilder("delete" + entityName)
				.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT).returns(int.class).addJavadoc("根据主键删除数据\n")
				.addParameter(entity, paramName).build();
		MethodSpec updateMethod = MethodSpec.methodBuilder("update" + entityName)
				.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT).returns(int.class).addJavadoc("修改\n")
				.addParameter(entity, paramName).build();
		MethodSpec addMethod = MethodSpec.methodBuilder("save" + entityName)
				.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT).returns(int.class).addJavadoc("新增\n")
				.addParameter(entity, paramName).build();
		Builder typeSpec = TypeSpec.interfaceBuilder(entityName + "Mapper").addModifiers(Modifier.PUBLIC);
		typeSpec.addMethod(queryMethod).addMethod(deleteMethod).addMethod(updateMethod).addMethod(addMethod);
		typeSpec.addJavadoc("mapper<br>\n@author " + classAuthor + "\n@date "
				+ dateTimeFormater.format(LocalDateTime.now()) + "\n@since " + classVersion + "\n");
		TypeSpec generateClass = typeSpec.build();
		JavaFile javaFile = JavaFile.builder(mapperPackagePath, generateClass).build();
		try {
			javaFile.writeTo(Paths.get(mapperFilePath));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
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

	private boolean generateXml() {
		File folder = new File(xmlPath);
		if (!folder.exists()) {
			folder.mkdirs();
		}
		final String entityPath = entityPackagePath + "." + entityName;
		File mapperXmlFile = new File(xmlPath, entityName + "Mapper.xml");
		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(mapperXmlFile)));) {
			bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			bw.newLine();
			bw.write(
					"<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">");
			bw.newLine();
			bw.write("<mapper namespace=\"" + entityPath + "Mapper\">");
			bw.newLine();
			bw.newLine();
			buildSQL(bw, entityPath, tableName, entityName);
			bw.write("</mapper>");
			bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	private void buildSQL(BufferedWriter bw, String entityPath, String tableName, String entityName)
			throws IOException {
		// 通用结果列
		bw.write("\t<!-- 通用查询结果列-->");
		bw.newLine();
		bw.write("\t<sql id=\"Base_Column_List\">");
		bw.newLine();
		bw.write("\t\t");
		List<String> list = new ArrayList<>(conlumnList.size());
		conlumnList.forEach(entity -> {
			list.add(entity.getConlumnName());
		});
		bw.write(String.join(",", list));
		bw.newLine();
		bw.write("\t</sql>");
		bw.newLine();
		bw.newLine();
		// 查询（根据主键ID查询）
		bw.write("\t<!-- 按非空的字段查询 -->");
		bw.newLine();
		bw.write("\t<select id=\"listAll\" resultType=\"" + entityPath + "\" parameterType=\"" + entityPath + "\">");
		bw.newLine();
		bw.write("\t\t SELECT");
		bw.newLine();
		bw.write("\t\t\t\t <include refid=\"Base_Column_List\" />");
		bw.newLine();
		bw.write("\t\t  FROM " + tableName);
		bw.newLine();
		bw.write("\t\t <where> ");
		bw.newLine();
		commonSQL(bw, "where");
		bw.write("\t\t </where>");
		bw.newLine();
		bw.write("\t </select>");
		bw.newLine();
		bw.newLine();
		// 查询完

		// 删除（根据主键ID删除）
		bw.write("\t<!--删除：根据主键ID删除-->");
		bw.newLine();
		bw.write("\t<delete id=\"delete" + entityName + "\" parameterType=\"" + entityPath + "\">");
		bw.newLine();
		bw.write("\t\t DELETE FROM " + tableName);
		bw.newLine();
		bw.write("\t\t <where> ");
		bw.newLine();
		commonSQL(bw, "where");
		bw.write("\t\t </where>");
		bw.newLine();
		bw.write("\t</delete>");
		bw.newLine();
		bw.newLine();
		// 删除完

		// 添加insert完

		// --------------- insert方法（匹配有值的字段）
		bw.write("\t<!-- 添加 （匹配有值的字段）-->");
		bw.newLine();
		bw.write("\t<insert id=\"save" + entityName + "\" parameterType=\"" + entityPath + "\">");
		bw.newLine();
		bw.write("\t\t INSERT INTO " + tableName);
		bw.newLine();
		bw.write("\t\t <trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\">");
		bw.newLine();
		commonSQL(bw, "insertColumn");
		bw.write("\t\t </trim>");
		bw.newLine();
		bw.write("\t\t VALUES");
		bw.newLine();
		bw.write("\t\t <trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\">");
		bw.newLine();
		commonSQL(bw, "insertValue");
		bw.write("\t\t </trim>");
		bw.newLine();
		bw.write("\t </insert>");
		bw.newLine();
		bw.newLine();
		// --------------- 完毕

		// 修改update方法
		bw.write("\t<!-- 根据主键修 改-->");
		bw.newLine();
		bw.write("\t<update id=\"update" + entityName + "\" parameterType=\"" + entityPath + "\">");
		bw.newLine();
		bw.write("\t\t UPDATE " + tableName);
		bw.newLine();
		bw.write(" \t\t <set> ");
		bw.newLine();
		commonSQL(bw, "set");
		bw.write(" \t\t </set>");
		bw.newLine();
		bw.write(" \t\t <where> ");
		bw.newLine();
		primaryKeyList.forEach(entity -> {
			try {
				bw.write(" \t\t\t\t AND " + entity.getConlumnName() + " = #{" + entity.getFieldName() + "}");
				bw.newLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		bw.write(" \t\t </where> ");
		bw.newLine();
		bw.write("\t</update>");
		bw.newLine();
		bw.newLine();
		// update方法完毕
	}

	private void commonSQL(BufferedWriter bw, String sqlType) {
		conlumnList.forEach(entity -> {
			String str = null;
			String fieldName = entity.getFieldName();
			String columnName = entity.getConlumnName();
			try {
				bw.write("\t\t\t <if test=\"" + fieldName + " !=null\"> ");
				bw.newLine();
				if ("set".equals(sqlType)) {
					str = "\t\t\t\t" + columnName + " = #{" + fieldName + "},";
				} else if ("where".equals(sqlType)) {
					str = "\t\t\t\t AND " + columnName + " = #{" + fieldName + "}";
				} else if ("insertColumn".equals(sqlType)) {
					str = "\t\t\t\t " + columnName + ",";
				} else if ("insertValue".equals(sqlType)) {
					str = "\t\t\t\t #{" + fieldName + "},";
				}
				bw.write(str);
				bw.newLine();
				bw.write("\t\t\t </if> ");
				bw.newLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	/**
	 * 一键生成实体和mapper和xml文件
	 */
	public void oneTouch() {
		this.generateEntity();
		this.generateMapper();
		this.generateXml();
	}

	public static void main(String[] args) {

		/**
		 * 一键生成实体和mapper和xml文件
		 */

		Generate propertiesAnalyze = new Generate();
		propertiesAnalyze.oneTouch();

	}
}
