package org.readutf.buildformat.common.format;

import org.readutf.buildformat.common.markers.Marker;

public interface MarkerAdapter<T> {

    T adapt(Marker marker);

}
