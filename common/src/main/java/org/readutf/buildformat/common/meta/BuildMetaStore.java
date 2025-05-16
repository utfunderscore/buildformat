package org.readutf.buildformat.common.meta;

import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Nullable;
import org.readutf.buildformat.common.exception.BuildFormatException;
import org.readutf.buildformat.common.format.BuildFormatData;

public interface BuildMetaStore {

    @NonNull BuildMeta create(String name, String description) throws BuildFormatException;

    @Nullable BuildMeta getByName(String name) throws BuildFormatException;

    @Nullable BuildMeta setFormats(List<BuildFormatData> formats);

}
