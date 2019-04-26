package org.cc.generate.orm.jpa;

import javax.lang.model.element.Modifier;

import org.cc.generate.entity.DatabaseReflect;

import com.squareup.javapoet.MethodSpec;

public class GenerateRemove {
	
	public com.squareup.javapoet.MethodSpec.Builder remove(String entityName,DatabaseReflect primaryKey) {
		com.squareup.javapoet.MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("update");
		methodBuilder.addModifiers(Modifier.PRIVATE);
		methodBuilder.addParameter(primaryKey.getJavaType(), primaryKey.getFieldName());
		methodBuilder
				.addCode(" Assert.notNull(%s,\"%s must not null\");", primaryKey.getFieldName());
		methodBuilder.addCode("%sRepository.removeOne(%s)", entityName, primaryKey.getFieldName());
		return methodBuilder;
	}
}
