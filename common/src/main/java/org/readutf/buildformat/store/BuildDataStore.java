package org.readutf.buildformat.store;

import org.readutf.buildformat.BuildData;

public interface BuildDataStore {

    BuildData loadBuildData(String buildId);

}
