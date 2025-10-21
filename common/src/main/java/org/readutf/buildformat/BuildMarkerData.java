package org.readutf.buildformat;

import org.readutf.buildformat.types.Position;

import java.util.List;
import java.util.Map;

public record BuildMarkerData(Map<String, List<Position>> positions, Map<String, String> genericSettings) {



}
