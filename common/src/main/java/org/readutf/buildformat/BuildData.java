package org.readutf.buildformat;

public record BuildData(
        BuildMarkerData buildMarkerData,
        byte[] buildData
) {
}
