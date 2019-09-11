package org.cc.generate.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;

public class AreaUtil {

    @Getter
    @Setter
    static final class Area{
        /**
         * 省
         */
        private String province;
        /**
         * 市
         */
        private String city;
        /**
         * 县
         */
        private String country;
        /**
         * 乡
         */
        private String townShip;
        /**
         * 村
         */
        private String village;
        /**
         * 门牌号
         */
        private String number;
    }

    private static int paternAddress(String address, String matchStr, int fromIndex, int count) {
        int index = -1;
        String[] matchStrs = matchStr.split("\\|");
        index = address.indexOf(matchStrs[count], fromIndex);
        if (index == -1) {
            count++;
            if (matchStrs.length < count) {
                return -1;
            }
            return paternAddress(address, matchStr, fromIndex, count);
        }
        return index;
    }

    public static Area getArea(String address){
        if(StringUtils.hasText(address)){
            Area area = new Area();
            int indexProvince = paternAddress(address,"省|市|自治区",0,0);
            int indexCity = paternAddress(address,"市|地区|盟|自治州",indexProvince+ 1,0);
            int indexCountry = paternAddress(address,"县|自治县|旗|自治旗|市|区",indexCity+ 1,0);
            int indexTownShip = paternAddress(address,"乡|民族乡|镇|街道",indexCountry+ 1,0);
            int indexVillage = paternAddress(address,"村|路|社区",indexTownShip+ 1,0);
            int indexNumber = paternAddress(address,"号",indexVillage+ 1,0);
            if(indexProvince > -1){
                area.setProvince(address.substring(0,indexProvince));
            }
            if(indexCity > -1 && indexProvince > -1){
                area.setCity(address.substring(indexProvince + 1,indexCity));
            }
            if(indexCity > -1 && indexCountry > -1){
                area.setCountry(address.substring(indexCity + 1,indexCountry));
            }
            if(indexCountry > -1 && indexTownShip > -1){
                area.setTownShip(address.substring(indexCountry + 1,indexTownShip));
            }
            if(indexTownShip > -1 && indexVillage > -1){
                area.setVillage(address.substring(indexTownShip + 1,indexVillage));
            }
            if(indexVillage > -1 && indexNumber > -1){
                area.setNumber(address.substring(indexVillage + 1,indexNumber));
            }
            return area;
        }
        return null;
    }
	/*
	 * public static void main(String[] args) { String address = "江西市赣州市章贡区九和路222号";
	 * Area area = splitArea("江西省赣州市章贡区章贡乡九和路222号");
	 * System.out.println(area.getProvince()); }
	 */

    public static Area splitArea(final String address){
        Area area = new Area();
        if(StringUtils.hasText(address)){
            char[] areaChars = new char[address.length()];
            address.getChars(0,address.length() -1,areaChars,0);
            StringBuilder str = new StringBuilder();
            int i = 0;
            String[] matches = new String[]{"省|市|自治区","市|地区|盟|自治州","县|自治县|旗|自治旗|市|区",
                    "乡|民族乡|镇|街道","村|路|社区","号"};
            int len = matches.length;
            Field[] fileds = area.getClass().getDeclaredFields();
            Field field;
            boolean flag;
            for(char areaChar : areaChars){
                if(i < len){
                    flag = true;
                    String[] strs = matches[i].split("\\|");
                    for(String s : strs){
                        if(s.equals(String.valueOf(areaChar))){
                            flag = false;
                            break;
                        }
                    }
                    if(flag){
                        str.append(areaChar);
                        if(i == len - 1){
                            field = fileds[i];
                            field.setAccessible(true);
                            try {
                                field.set(area,str.toString());
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        }
                    }else{
                        field = fileds[i];
                        field.setAccessible(true);
                        try {
                            field.set(area,str.toString());
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        str = new StringBuilder();
                        i++;
                    }
                }
            }
        }
        return area;
    }
}
