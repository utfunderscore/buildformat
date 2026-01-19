package org.readutf.buildformat.hytale.requirement.collectors.text;

public interface Convertor<T> {

    T convert(String str) throws Exception;

}
