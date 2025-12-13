package org.readutf.buildformat.utils;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Utility class for operations related to Java classes and types.
 */
public class ClassUtils {

    public static boolean equals(@NotNull Type type1, @NotNull Type type2) {
        if (type1 instanceof ParameterizedType && type2 instanceof ParameterizedType) {
            return parameterizedTypesEqual((ParameterizedType) type1, (ParameterizedType) type2);
        } else {
            return type1.equals(type2);
        }
    }

    /**
     * Compares two {@link ParameterizedType} instances for equality, including their typeReference arguments.
     *
     * @param parameterizedType  the first parameterized typeReference to compare
     * @param parameterizedType2 the second parameterized typeReference to compare
     * @return true if both parameterized types and their typeReference arguments are equal, false otherwise
     */
    private static boolean parameterizedTypesEqual(
            @NotNull ParameterizedType parameterizedType, @NotNull ParameterizedType parameterizedType2) {

        int length = parameterizedType.getActualTypeArguments().length;

        if (length != parameterizedType2.getActualTypeArguments().length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            Type typeArg1 = parameterizedType.getActualTypeArguments()[i];
            Type typeArg2 = parameterizedType2.getActualTypeArguments()[i];

            if (typeArg1 instanceof ParameterizedType && typeArg2 instanceof ParameterizedType) {
                if (!parameterizedTypesEqual((ParameterizedType) typeArg1, (ParameterizedType) typeArg2)) {
                    return false;
                }
            } else {
                if (!typeArg1.equals(typeArg2)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Returns the wrapper class for a given primitive class.
     * If the input is not a primitive typeReference, returns the input class itself.
     *
     * @param primitiveClass the primitive class to get the wrapper for
     * @return the corresponding wrapper class, or the input class if not primitive
     */
    public static Class<?> getPrimitiveClass(Class<?> primitiveClass) {
        if (primitiveClass == int.class) return Integer.class;
        if (primitiveClass == long.class) return Long.class;
        if (primitiveClass == short.class) return Short.class;
        if (primitiveClass == byte.class) return Byte.class;
        if (primitiveClass == float.class) return Float.class;
        if (primitiveClass == double.class) return Double.class;
        if (primitiveClass == boolean.class) return Boolean.class;
        if (primitiveClass == char.class) return Character.class;

        return primitiveClass; // If it's already a wrapper class
    }



    /**
     * Returns the wrapper class for a given primitive class.
     * If the input is not a primitive typeReference, returns the input class itself.
     *
     * @param primitiveClass the primitive class to get the wrapper for
     * @return the corresponding wrapper class, or the input class if not primitive
     */
    public static Type getPrimitiveClassFromGeneric(Type primitiveClass) {
        if (primitiveClass == int.class) return Integer.class;
        if (primitiveClass == long.class) return Long.class;
        if (primitiveClass == short.class) return Short.class;
        if (primitiveClass == byte.class) return Byte.class;
        if (primitiveClass == float.class) return Float.class;
        if (primitiveClass == double.class) return Double.class;
        if (primitiveClass == boolean.class) return Boolean.class;
        if (primitiveClass == char.class) return Character.class;

        return primitiveClass; // If it's already a wrapper class
    }
}
