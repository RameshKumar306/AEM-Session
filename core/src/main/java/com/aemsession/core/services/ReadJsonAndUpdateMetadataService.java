package com.aemsession.core.services;

import java.io.InputStream;
import java.util.List;

/**
 * Interface for ReadJsonAndUpdateMetadataService
 * */
public interface ReadJsonAndUpdateMetadataService {

    public List<String> readJsonAndUpdateMetadata(InputStream inputStream, List<String> updatedMetadataList);

}
