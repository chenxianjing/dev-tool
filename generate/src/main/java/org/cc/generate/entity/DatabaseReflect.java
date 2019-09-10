package org.cc.generate.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DatabaseReflect {
	/**
	 * 数据库字段对应的java类型
	 */
	private Class<?> javaType;
	/**
	 * 数据库字段注释
	 */
	private String annotation;
	/**
	 * 数据库字段转驼峰
	 */
	private String fieldName;
	/**
	 * 数据字段名字
	 */
	private String conlumnName;
	
	private Boolean  autoincrement;
}
