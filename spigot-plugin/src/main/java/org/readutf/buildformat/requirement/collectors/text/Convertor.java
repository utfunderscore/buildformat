package org.readutf.buildformat.requirement.collectors.text;

public interface Convertor<T> {

    T convert(String str) throws Exception;

}
