package org.readutf.buildformat.requirement;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;


public interface Requirement {

    String getName();

    Class<?> getType();

}
