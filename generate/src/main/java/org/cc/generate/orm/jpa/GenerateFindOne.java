package org.cc.generate.orm.jpa;

import javax.lang.model.element.Modifier;

import org.cc.generate.entity.DatabaseReflect;

import com.squareup.javapoet.MethodSpec;

public class GenerateFindOne {
	
	public com.squareup.javapoet.MethodSpec.Builder findOne(String entityName,DatabaseReflect primaryKey) {
		com.squareup.javapoet.MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("findOne");
		methodBuilder.addModifiers(Modifier.PRIVATE);
		methodBuilder.addParameter(primaryKey.getJavaType(), primaryKey.getFieldName());
		methodBuilder
				.addCode("Assert.notNull(%s,\"%s  must not be null\");", primaryKey.getFieldName());
		methodBuilder.addCode("%sOModel %sOModel = null;", entityName);
		methodBuilder.addCode("%sEntity %sEntity = %sRepository.getOne(%s);", entityName,entityName,entityName,primaryKey.getFieldName());
		methodBuilder.addCode("if(!Objects.isNull(aicEnterpriseBasicEntity)){", entityName);
		methodBuilder.addCode("%sOModel = new %sOModel();", entityName,entityName);
		methodBuilder.addCode("BeanUtils.copyProperties(%sEntity,%sOModel);", entityName,entityName);
		methodBuilder.addCode("}");
		methodBuilder.addCode("return %sOModel;", entityName);
		return methodBuilder;
	}
}
