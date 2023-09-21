package com.aemsession.core.services;

import java.io.InputStream;
import java.util.List;

public interface ReadJsonAndUpdateMetadataService {

    public List<String> readJsonAndUpdateMetadata(InputStream inputStream);

}
