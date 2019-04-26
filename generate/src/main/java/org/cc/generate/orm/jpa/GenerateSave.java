package org.cc.generate.orm.jpa;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.MethodSpec;

public class GenerateSave {
	public com.squareup.javapoet.MethodSpec.Builder update(String entityName,String primaryName) {
		com.squareup.javapoet.MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("update");
		methodBuilder.addModifiers(Modifier.PRIVATE);

		try {
			methodBuilder.addParameter(Class.forName("in." + entityName + "Entity"), entityName + "Entity");
			methodBuilder.returns(Class.forName("domain." + entityName + "Entity"));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		methodBuilder
				.addCode(" Assert.notNull(" + entityName + "Entity," + "\"" + entityName + "Entity must not null\"");
		methodBuilder.addCode("dynamicRepository.updateDynamicByPrimaryKey(%sEntity)", entityName);
		methodBuilder.addCode("return aicEnterpriseBasicRepository.findOne(%sEntity.get%s());", entityName,primaryName);
		return methodBuilder;
	}
	
	public com.squareup.javapoet.MethodSpec.Builder save(String entityName,String primaryName) {
		com.squareup.javapoet.MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("save");
		methodBuilder.addModifiers(Modifier.PRIVATE);

		try {
			methodBuilder.addParameter(Class.forName("in." + entityName + "IModel"), entityName + "IModel");
			methodBuilder.returns(Class.forName("domain." + entityName + "Entity"));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		methodBuilder
				.addCode(" Assert.notNull(" + entityName + "IModel," + "\"" + entityName + "IModel must not null\"");
		methodBuilder.addCode("%sEntity %sEntity = new %sEntity();", entityName,entityName,entityName);
		methodBuilder.addCode("BeanUtils.copyProperties(%sIModel,%sEntity);", entityName,entityName);
		methodBuilder.addCode("if(Objects.isNull(%sEntity.get%s())){", entityName,primaryName);
		methodBuilder.addCode("return %sRepository.save(%sEntity);", entityName,entityName);
		methodBuilder.addCode(" }else{");
		methodBuilder.addCode("return this.update(%sEntity);",entityName);
		methodBuilder.addCode("}");
		return methodBuilder;
	}
}