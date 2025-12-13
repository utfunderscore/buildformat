package org.readutf.buildformat.utils;

import java.lang.reflect. ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DynamicTypeParser {

    public static Type parseType(String typeString) throws ClassNotFoundException {
        typeString = typeString.trim();

        // Handle "class " prefix (from Class.toString())
        if (typeString. startsWith("class ")) {
            typeString = typeString.substring(6).trim();
        }

        // Handle "interface " prefix (from interface Class.toString())
        if (typeString.startsWith("interface ")) {
            typeString = typeString.substring(10).trim();
        }

        // Check if it's a generic type
        if (! typeString.contains("<")) {
            // Simple class without generics
            return Class.forName(typeString);
        }

        // Parse generic type
        int firstBracket = typeString. indexOf('<');
        String rawTypeName = typeString.substring(0, firstBracket).trim();
        Class<?> rawClass = Class.forName(rawTypeName);

        // Extract generic type arguments
        String genericsSection = typeString.substring(firstBracket + 1, typeString.lastIndexOf('>')).trim();
        Type[] typeArguments = parseTypeArguments(genericsSection);

        return createParameterizedType(rawClass, typeArguments);
    }

    private static Type[] parseTypeArguments(String genericsSection) throws ClassNotFoundException {
        List<String> typeArgStrings = splitTypeArguments(genericsSection);
        Type[] typeArguments = new Type[typeArgStrings.size()];

        for (int i = 0; i < typeArgStrings.size(); i++) {
            // Recursively parse each type argument (handles nested generics)
            typeArguments[i] = parseType(typeArgStrings.get(i));
        }

        return typeArguments;
    }

    private static List<String> splitTypeArguments(String genericsSection) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;

        for (char c : genericsSection.toCharArray()) {
            if (c == '<') {
                depth++;
                current.append(c);
            } else if (c == '>') {
                depth--;
                current.append(c);
            } else if (c == ',' && depth == 0) {
                // Only split on commas at the top level
                result. add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current. append(c);
            }
        }

        if (current. length() > 0) {
            result.add(current.toString().trim());
        }

        return result;
    }

    private static ParameterizedType createParameterizedType(Class<?> rawClass, Type[] typeArguments) {
        return new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return typeArguments;
            }

            @Override
            public Type getRawType() {
                return rawClass;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append(rawClass.getName());
                if (typeArguments.length > 0) {
                    sb.append("<");
                    for (int i = 0; i < typeArguments.length; i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(typeArguments[i].getTypeName());
                    }
                    sb.append(">");
                }
                return sb.toString();
            }
        };
    }

}