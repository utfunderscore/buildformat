package org.readutf.buildformat;

import java.time.LocalDateTime;
import java.util.List;

public record Build(
        String name,
        String description,
        LocalDateTime creationTimestamp,
        int version,
        List<String> formats
) {
}
