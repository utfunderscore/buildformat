package org.readutf.buildformat.adapter;

public interface TypeAdapter<A, B> {

    B convert(A a);

    default B convertUnknown(Object a) {
        return convert((A) a);
    }

}
