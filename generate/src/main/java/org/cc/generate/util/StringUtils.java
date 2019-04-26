package org.cc.generate.util;

public class StringUtils {
	/**
	 * 首字母大写
	 * 
	 * @param s
	 * @return
	 */
	public static String firstLetterToUpper(String s) {
		if (s == null || s.length() == 0) {
			return s;
		} else {
			return s.substring(0, 1).toUpperCase() + s.substring(1, s.length());
		}
	}

	/**
	 * 首字母小写
	 * 
	 * @param s
	 * @return
	 */
	public static String firstLetterToLowwer(String s) {
		if (s == null || s.length() == 0) {
			return s;
		} else {
			return s.substring(0, 1).toLowerCase() + s.substring(1, s.length());
		}
	}
}
