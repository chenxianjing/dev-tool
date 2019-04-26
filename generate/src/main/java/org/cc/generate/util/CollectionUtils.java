package org.cc.generate.util;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Iterator;

public class CollectionUtils {

	/**
	 * 交或者差集 集合的泛型为原始类型的包装类
	 * 
	 * @param a
	 * @param b
	 * @param field
	 * @param flag
	 *            false 差集 true 交集
	 * @return
	 */
	private static <E> Collection<E> mix(Collection<E> a, Collection<E> b, boolean flag) {
		Collection<E> a1 = a;
		Collection<E> b1 = b;
		Iterator<E> it = a1.iterator();
		Iterator<E> it2 = b1.iterator();
		E e1;
		boolean contain = flag;
		while (it.hasNext()) {
			e1 = it.next();
			contain = flag;
			try {
				while (it2.hasNext()) {
					if (e1.equals(it2.next())) {
						contain = !flag;
						break;
					}
				}
				if (contain) {
					it.remove();
				}
			} catch (IllegalArgumentException | SecurityException e3) {
				e3.printStackTrace();
			}
		}
		return a1;
	}

	/**
	 * 交或者差集
	 * 
	 * @param a
	 * @param b
	 * @param field
	 * @param flag
	 *            false 差集 true 交集
	 * @return
	 */
	private static <E> Collection<E> mix(Collection<E> a, Collection<E> b, String field, boolean flag) {
		Collection<E> a1 = a;
		Collection<E> b1 = b;
		Iterator<E> it = a1.iterator();
		Iterator<E> it2 = b1.iterator();
		E e1;
		E e2;
		Object aField;
		Object bField;
		boolean contain = flag;
		while (it.hasNext()) {
			e1 = it.next();
			contain = flag;
			try {
				aField = e1.getClass().getMethod("get" + StringUtils.firstLetterToUpper(field)).invoke(e1);
				while (it2.hasNext()) {
					e2 = it2.next();
					bField = e1.getClass().getMethod("get" + StringUtils.firstLetterToUpper(field)).invoke(e2);
					if (aField.equals(bField)) {
						contain = !flag;
						break;
					}
				}
				if (contain) {
					it.remove();
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
					| NoSuchMethodException | SecurityException e3) {
				e3.printStackTrace();
			}
		}
		return a1;
	}

	/**
	 * 取交集
	 * 
	 * @param a
	 *            集合a
	 * @param b
	 *            集合b
	 * @param field
	 *            实体E的比较属性
	 * @return
	 */
	public static <E> Collection<E> intersect(Collection<E> a, Collection<E> b) {
		return mix(a, b, true);
	}

	/**
	 * 取交集
	 * 
	 * @param a
	 *            集合a
	 * @param b
	 *            集合b
	 * @param field
	 *            实体E的比较属性
	 * @return
	 */
	public static <E> Collection<E> intersect(Collection<E> a, Collection<E> b, String field) {
		return mix(a, b, field, true);
	}

	/**
	 * 取差集 a-b a减去公共部分剩下的部分
	 * 
	 * @param a
	 *            集合a
	 * @param b
	 *            集合b
	 * @param field
	 *            实体E的比较属性
	 * @return
	 */
	public static <E> Collection<E> except(Collection<E> a, Collection<E> b) {
		return mix(a, b, false);
	}

	/**
	 * 取差集 a-b a减去公共部分剩下的部分
	 * 
	 * @param a
	 *            集合a
	 * @param b
	 *            集合b
	 * @param field
	 *            实体E的比较属性
	 * @return
	 */
	public static <E> Collection<E> except(Collection<E> a, Collection<E> b, String field) {
		return mix(a, b, field, false);
	}

	/**
	 * 取并集
	 * 
	 * @param a
	 *            集合a
	 * @param b
	 *            集合b
	 * @param field
	 *            实体E的比较属性
	 * @return
	 */
	public static <E> Collection<E> union(Collection<E> a, Collection<E> b) {
		Collection<E> a1 = a;
		Collection<E> b1 = b;
		Iterator<E> it = a1.iterator();
		Iterator<E> it2 = b1.iterator();
		E e1;
		boolean contain = false;
		while (it.hasNext()) {
			e1 = it.next();
			contain = false;
			try {
				while (it2.hasNext()) {
					if (e1.equals(it2.next())) {
						contain = true;
						break;
					}
				}
				if (!contain) {
					b.add(e1);
				}
			} catch (IllegalArgumentException | SecurityException e3) {
				e3.printStackTrace();
			}
		}
		return b1;
	}

	/**
	 * 取并集
	 * 
	 * @param a
	 *            集合a
	 * @param b
	 *            集合b
	 * @param field
	 *            实体E的比较属性
	 * @return
	 */
	public static <E> Collection<E> union(Collection<E> a, Collection<E> b, String field) {
		Collection<E> a1 = a;
		Collection<E> b1 = b;
		Iterator<E> it = a1.iterator();
		Iterator<E> it2 = b1.iterator();
		E e1;
		E e2;
		Object aField;
		Object bField;
		boolean contain = false;
		while (it.hasNext()) {
			e1 = it.next();
			contain = false;
			try {
				aField = e1.getClass().getMethod("get" + StringUtils.firstLetterToUpper(field)).invoke(e1);
				while (it2.hasNext()) {
					e2 = it2.next();
					bField = e1.getClass().getMethod("get" + StringUtils.firstLetterToUpper(field)).invoke(e2);
					if (aField.equals(bField)) {
						contain = true;
						break;
					}
				}
				if (!contain) {
					b.add(e1);
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
					| NoSuchMethodException | SecurityException e3) {
				e3.printStackTrace();
			}
		}
		return b1;
	}

}
