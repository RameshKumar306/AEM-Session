package com.aemsession.core.services;


import java.util.Map;

public interface MetadataSchemaFieldMappingService {
    public String[] fieldValues();

    public Map<String,String> getJsonAndMetaDataSchemaMap();
}
