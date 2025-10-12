package org.readutf.buildformat.common.schematic;

import org.readutf.buildformat.common.markers.Marker;

import java.util.List;

public record BuildData(String buildName, List<Marker> markers, byte[] schematicData, byte[] polarData) {
}
