package org.readutf.buildformat;

import java.util.List;

public record Build(
        String name,
        String description,
        long creationTimestamp,
        int version,
        List<String> formats
) {
}
