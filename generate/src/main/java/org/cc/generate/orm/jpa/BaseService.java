package org.cc.generate.orm.jpa;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.persistence.Id;
import javax.transaction.Transactional;

import org.cc.generate.util.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * abstract class for generic CRUD operations on a service for a specific type.
 *
 * @author chenxianjing
 * @desciption 基础增删改查 如果使用假删除，数据库字段定义为del_flag,默认值设为1，1表示未删除，0表示删除 E对应Entity
 *             I对应IModel不是分页IModel O对应OModel
 * @date 2019-05-07 09:24
 * @since 2.0.0
 */
@Slf4j
public class BaseService<E> {

	@Autowired
	private DynamicRepository<E> dynamicRepository;
	/**
	 * JPA的reository对象，项目中必须自己创建的Repository类，必须以取名如下:<br>
	 * 例:实体名为:UserEntity Repository名为:UserRepository
	 */
	private volatile Object repository;
	// 主键信息，目前只支持单主键
	private List<Field> primaryList = new CopyOnWriteArrayList<>();
	// 获取实际泛型的数组
	private final Type[] genericTypeArray = ((ParameterizedType) getClass().getGenericSuperclass())
			.getActualTypeArguments();
	// 泛型E的class
	private final Class<E> entityClass = (Class<E>) genericTypeArray[0];
	// 假删除——已删除
	public final static int DELETE = 0;
	// 假删除——未删除
	public final static int UN_DELETE = 1;

	{
		Field[] fields = entityClass.getDeclaredFields();
		if (primaryList.isEmpty()) {
			for (Field field : fields) {
				field.setAccessible(true);
				if (field.isAnnotationPresent(Id.class)) {
					primaryList.add(field);
				}
			}
		}
	}

	/**
	 * 获取repository实例化对象
	 *
	 * @return
	 */
	private Object getRepository() {
		// 加锁版单例模式
		if (Objects.isNull(repository)) {
			synchronized (this) {
				if (Objects.isNull(repository)) {
					String[] packageName = entityClass.toString().split("\\.");
					String repositoryName = packageName[packageName.length - 1].replaceAll("Entity", "");
					repository = SpringContextUtil.getBean(repositoryName.substring(0, 1).toLowerCase()
							+ repositoryName.substring(1, repositoryName.length()) + "Repository");
				}
			}
		}
		return repository;
	}

	/**
	 * 修改接口
	 *
	 * @param i
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public E save(E param) {
		getRepository();
		Assert.notNull(param, "param must not null");
		// 设置主键为空
		ReflectionUtils.setField(primaryList.get(0), param, null);
		return (E) ReflectionUtils.invokeMethod(ReflectionUtils.findMethod(repository.getClass(), "save", Object.class),
				repository, param);
	}

	/**
	 * 修改接口
	 *
	 * @param i
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public E update(E param) {
		getRepository();
		Assert.notNull(param, "param must not null");
		int j = dynamicRepository.updateDynamicByPrimaryKey(param);
		if (j > 0) {
			return  (E) ReflectionUtils.invokeMethod(
					ReflectionUtils.findMethod(repository.getClass(), "findOne", Serializable.class), repository,
					ReflectionUtils.getField(primaryList.get(0), param));
		}
		log.error("更新失败，数据库异常或者数据不存在");
		return null;
	}

	/**
	 * 获取单条信息
	 *
	 * @param id 主键ID
	 * @return
	 */
	public E findOne(Long id) {
		getRepository();
		Assert.notNull(id, "主键 must not be null");
		E param = null;
		E e;
		try {
			param = entityClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e1) {
			log.error("查询失败，返回参数实体生成失败", e1);
		}
		Field primaryField = primaryList.get(0);
		ReflectionUtils.makeAccessible(primaryField);
		ReflectionUtils.setField(primaryField, param, id);
		// 假删除字段
		Field delFlag = ReflectionUtils.findField(entityClass, "delFlag");
		if (!Objects.isNull(delFlag)) {
			ReflectionUtils.makeAccessible(delFlag);
			ReflectionUtils.setField(delFlag, param, UN_DELETE);
		}
		return dynamicRepository.getOneDynamic(param);
	}

	/**
	 * 逻辑删除单个信息
	 *
	 * @param id 主键ID
	 * @return
	 */
	@Transactional
	public int delete(Long id) {
		getRepository();
		Assert.notNull(id, "id must not null");
		E param = null;
		try {
			param = entityClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e1) {
			log.error("删除失败，获取主键失败", e1);
		}
		Field primaryField = primaryList.get(0);
		ReflectionUtils.makeAccessible(primaryField);
		ReflectionUtils.setField(primaryField, param, id);
		Field delFlag = ReflectionUtils.findField(entityClass, "delFlag");
		// 如果实体存在假删除字段，就把假删除字段更新为0，表示删除;如果没有，则使用真删除
		if (Objects.isNull(delFlag)) {
			ReflectionUtils.invokeMethod(
					ReflectionUtils.findMethod(repository.getClass(), "delete", Serializable.class), repository, id);
			return 1;
		} else {
			ReflectionUtils.makeAccessible(delFlag);
			ReflectionUtils.setField(delFlag, param, DELETE);
			return dynamicRepository.updateDynamicByPrimaryKey(param);
		}
	}

	/**
	 * 分页查询信息
	 *
	 * @param p 分页model Model的子类
	 * @return SearchResultModel<O>
	 */
	/*
	 * public <P extends Model> SearchResultModel<O> page(P p) { return
	 * this.pageEach(p, null); }
	 */

	/**
	 * 分页查询信息
	 *
	 * @param p 分页model Model的子类
	 * @return SearchResultModel<O>
	 */
	/*
	 * public <P extends Model> SearchResultModel<O> pageEach(P p, Consumer<O>
	 * action) { getRepository(); Assert.notNull(p, "参数 must not be null");
	 * SearchResultModel<O> searchResultModel = new SearchResultModel<>(); List<O>
	 * resultList = Collections.emptyList(); O result; Page<E> page; //
	 * 假删除字段，1表示未删除，0表示删除，如果存在假删除字段，表示使用假删除 Field delFlag =
	 * ReflectionUtils.findField(p.getClass(), "delFlag"); if
	 * (!Objects.isNull(delFlag)) { ReflectionUtils.makeAccessible(delFlag);
	 * ReflectionUtils.setField(delFlag, p, UN_DELETE); } Specification<E>
	 * specification = SpecificationUtils.generateSpecification(p, entityClass);
	 * PageRequest pageRequest = new PageRequest(p.getOffset() - 1, p.getLimit());
	 * page = (Page<E>) ReflectionUtils.invokeMethod(
	 * ReflectionUtils.findMethod(repository.getClass(), "findAll",
	 * Specification.class, Pageable.class), repository, specification,
	 * pageRequest); if (page != null) { searchResultModel =
	 * SearchResultModelUtils.createSearchResultModel(page); if
	 * (searchResultModel.getTotalElements() > 0) { resultList = new
	 * ArrayList<>((int) searchResultModel.getTotalElements()); for (E c : page) {
	 * try { result = oModelClass.newInstance(); BeanUtils.copyProperties(c,
	 * result); if (!Objects.isNull(action)) { action.accept(result); }
	 * resultList.add(result); } catch (InstantiationException |
	 * IllegalAccessException e) { log.error("查询失败，oModel实例化失败", e); } }
	 * searchResultModel.setElements(resultList); } }
	 * searchResultModel.setElements(resultList); return searchResultModel; }
	 */

	/**
	 * 根据id批量查询
	 *
	 * @param ids
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<E> getMore(List<Long> ids) {
		Assert.notNull(ids, "ids must not null");
		getRepository();
		return (List<E>) ReflectionUtils.invokeMethod(
				ReflectionUtils.findMethod(repository.getClass(), "findAll", Iterable.class), repository, ids);
	}
}
