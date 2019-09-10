package org.cc.generate.orm.jpa;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import javax.persistence.*;
import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.lang.reflect.Field;
import java.util.*;

/**
 * 动态操作工具类<br>
 *
 * @param <T> 对应entity
 * @author chenxianjing
 * @date 2019-04-24 14:28:20
 * @since 5.0.0
 */
@Slf4j
@Repository
public class DynamicRepository<T> {

    @PersistenceContext
    private EntityManager em;

    /**
     * 动态更新 暂只支持单主键
     *
     * @param t
     * @return
     */
    @Transactional
    public int updateDynamicByPrimaryKey(T t) {
        Class entityClass = t.getClass();
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        //加入当前事务
        em.joinTransaction();
        CriteriaUpdate update = criteriaBuilder.createCriteriaUpdate(entityClass);
        Root<T> root = update.from(entityClass);
        Field[] fields = entityClass.getDeclaredFields();
        List<Field> allFields = new ArrayList<>(fields.length << 1);
        allFields.addAll(Arrays.asList(fields));
        //实体的父类
        Optional<Class<?>> superClassOptional = Optional.of(entityClass.getSuperclass());
        superClassOptional.ifPresent(c ->
                allFields.addAll(Arrays.asList(c.getDeclaredFields()))
        );
        //主键名字
        Field primaryField = null;
        for (Field field : allFields) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(Column.class) && !field.isAnnotationPresent(Id.class)) {
                Object param = ReflectionUtils.getField(field, t);
                if (!Objects.isNull(param)) {
                    update.set(root.get(field.getName()), param);
                }
            }
            if (field.isAnnotationPresent(Id.class)) {
                primaryField = field;
            }
        }
        if (Objects.isNull(primaryField)) {
            log.error("根据主键更新表失败:主键不存在");
            return 0;
        }
        primaryField.setAccessible(true);
        Object primaryFieldValue = ReflectionUtils.getField(primaryField, t);
        update.where(criteriaBuilder.equal(root.get(primaryField.getName()), primaryFieldValue));
        Query query = em.createQuery(update);
        int i = query.executeUpdate();
        //数据更新了，清除jpa的缓存:session对应实体
        Object entityCache = em.find(entityClass, primaryFieldValue);
        if (!Objects.isNull(entityCache)) {
            em.detach(entityCache);
        }
        return i;
    }

    /**
     * 动态参数拼接,如果存在假删除，默认查询未删除的数据，del_flag传-1查询所有，0为已删除，1为未删除,不传默认查询未删除
     *
     * @param t
     * @return
     */
    private TypedQuery<T> getDynamicParam(T t) {
        Class entityClass = t.getClass();
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery queryParam = em.getCriteriaBuilder().createQuery(entityClass);
        Root<T> root = queryParam.from(entityClass);
        Field[] fields = entityClass.getDeclaredFields();
        int fieldLength = fields.length;
        List<Field> allFields = new ArrayList<>(fieldLength << 1);
        allFields.addAll(Arrays.asList(fields));
        //实体的超类
        Optional<Class<?>> superClassOption = Optional.of(entityClass.getSuperclass());
        superClassOption.ifPresent(c -> allFields.addAll(Arrays.asList(c.getDeclaredFields())));
        List<Predicate> paramList = new ArrayList<>(fieldLength);
        //如果实体存在假删除字段且为空，默认查询未删除的数据
        Field delFlag = ReflectionUtils.findField(entityClass, "delFlag");
        if (!Objects.isNull(delFlag) && Objects.isNull(ReflectionUtils.getField(delFlag, t))) {
            ReflectionUtils.makeAccessible(delFlag);
            paramList.add(criteriaBuilder.equal(root.get(delFlag.getName()), BaseService.UN_DELETE));
        }
        for (Field field : allFields) {
            field.setAccessible(true);
            Object param = ReflectionUtils.getField(field, t);
            if (!Objects.isNull(param) && field.isAnnotationPresent(Column.class)) {
                if (param instanceof String) {
                    if (StringUtils.hasText(String.valueOf(param))) {
                        paramList.add(criteriaBuilder.equal(root.get(field.getName()), param));
                    }
                } else {
                    if("delFlag".equals(field.getName())){
                        //del_flag传-1查询所有
                        if(Integer.parseInt(param.toString()) != -1){
                            paramList.add(criteriaBuilder.equal(root.get(field.getName()), param));
                        }
                    }else{
                        paramList.add(criteriaBuilder.equal(root.get(field.getName()), param));
                    }
                }
            }

        }
        if (!paramList.isEmpty()) {
            Predicate[] predicateArray = new Predicate[paramList.size()];
            paramList.toArray(predicateArray);
            queryParam.where(predicateArray);
        }
        return em.createQuery(queryParam);
    }

    /**
     * 动态查询获取单条数据
     *
     * @param t
     * @return
     */
    public T getOneDynamic(T t) {
        try {
            return getDynamicParam(t).getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * 动态查询获取多条条数据
     *
     * @param t
     * @return
     */
    public List<T> getMoreDynamic(T t) {
        return getDynamicParam(t).getResultList();
    }
}
