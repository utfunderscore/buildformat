package org.readutf.buildformat.store;

import org.readutf.buildformat.BuildData;

public interface BuildDataStore {

    void save(String buildName, int version, BuildData buildData) throws Exception;

    void delete(String buildName, int version) throws Exception;

    BuildData get(String buildName, int version) throws Exception;

}
