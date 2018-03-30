package com.xy.reflect.method.access;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Beans {

    private final static ConcurrentMap<Class<?>, MethodAccess> METHOD_CACHE = new ConcurrentHashMap<>();

    private final static ConcurrentMap<Class<?>, Field[]> FIELDS_CACHE = new ConcurrentHashMap<>();

    private static MethodAccess setIfAbsentMethodAccess(Class<?> aClass) {
        MethodAccess methodAccess = METHOD_CACHE.get(aClass);
        if (methodAccess == null) {
            MethodAccess newMethodAccess = MethodAccess.get(aClass);
            methodAccess = METHOD_CACHE.putIfAbsent(aClass, newMethodAccess);
            if (methodAccess == null) {
                methodAccess = newMethodAccess;
            }
        }
        return methodAccess;
    }

    private static Field[] setIfAbsentFileds(Class<?> aClass) {
        Field[] fields = FIELDS_CACHE.get(aClass);
        if (fields == null) {
            Field[] newFiled = aClass.getDeclaredFields();
            fields = FIELDS_CACHE.putIfAbsent(aClass, newFiled);
            if (fields == null) {
                fields = newFiled;
            }
        }
        return fields;
    }

    public static void copyProperties(Object dest, Object orig) {
        final MethodAccess destMethodAccess = setIfAbsentMethodAccess(dest.getClass());
        final MethodAccess origMethodAccess = setIfAbsentMethodAccess(orig.getClass());
        Field[] fields = setIfAbsentFileds(orig.getClass());

        for (Field field : fields) {
            Object param = origMethodAccess.invoke(orig, getMethodName(field.getName()));
            destMethodAccess.invoke(dest, setMethodName(field.getName()), param);
        }
    }

    private static String getMethodName(String fieldName) {
        char n = fieldName.charAt(0);
        return "get" + fieldName.replace(n, (char) (n - 32));
    }

    private static String setMethodName(String fieldName) {
        char n = fieldName.charAt(0);
        return "set" + fieldName.replace(n, (char) (n - 32));
    }

}
