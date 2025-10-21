package org.readutf.buildformat;


import org.jspecify.annotations.NonNull;
import org.readutf.buildformat.adapters.GenericTypeAdapter;
import org.readutf.buildformat.types.Position;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildConstructor {

    private final Map<Class<?>, GenericTypeAdapter<?>> genericTypeAdapters;

    public BuildConstructor() {
        this.genericTypeAdapters = new HashMap<>();

        this.genericTypeAdapters.put(double.class, Double::parseDouble);
        this.genericTypeAdapters.put(float.class, Float::parseFloat);
        this.genericTypeAdapters.put(int.class, Integer::parseInt);
        this.genericTypeAdapters.put(String.class, String::valueOf);
    }

    public <T> T construct(@NonNull Class<T> clazz, BuildMarkerData buildData) throws Exception {
        if (!clazz.isRecord()) {
            throw new Exception("Class must be a record");
        }

        Constructor<?>[] constructors = clazz.getConstructors();
        if (constructors.length != 1) {
            throw new Exception("Record must have exactly one constructor");
        }
        Constructor<?> constructor = constructors[0];

        List<Object> parameters = new ArrayList<>();

        for (Parameter parameter : constructor.getParameters()) {
                String paramName = parameter.getName();
                Class<?> paramType = parameter.getType();

                if (Position.class.equals(paramType)) {
                    List<Position> posList = buildData.positions().get(paramName);
                    if (posList == null || posList.isEmpty()) {
                        throw new Exception("No positions provided for parameter: " + paramName);
                    }
                    parameters.add(posList.getFirst()); // take the first position
                } else if (List.class.isAssignableFrom(paramType)) {
                    java.lang.reflect.Type genericType = parameter.getParameterizedType();
                    boolean isListOfPosition = false;
                    if (genericType instanceof java.lang.reflect.ParameterizedType) {
                        java.lang.reflect.Type[] typeArgs = ((java.lang.reflect.ParameterizedType) genericType).getActualTypeArguments();
                        if (typeArgs.length == 1 && typeArgs[0] instanceof Class && Position.class.equals(typeArgs[0])) {
                            isListOfPosition = true;
                        }
                    }

                    if (isListOfPosition) {
                        List<Position> posList = buildData.positions().get(paramName);
                        if (posList == null) {
                            throw new Exception("No positions provided for parameter: " + paramName);
                        }
                        parameters.add(posList); // pass the whole list
                    } else {
                        throw new Exception("No adapter found for parameter type: " + paramType.getName());
                    }
                } else if (genericTypeAdapters.containsKey(paramType)) {
                    GenericTypeAdapter<?> adapter = genericTypeAdapters.get(paramType);
                    String value = buildData.genericSettings() .get(paramName);
                    if (value == null) {
                        throw new Exception("No generic setting provided for parameter: " + paramName);
                    }
                    Object adaptedValue = adapter.fromString(value);
                    parameters.add(adaptedValue);
                } else {
                    throw new Exception("No adapter found for parameter type: " + paramType.getName());
                }
            }

        constructor.setAccessible(true);
        return clazz.cast(constructor.newInstance(parameters.toArray()));
    }

}
