package com.aemsession.core.services.impl;

import com.aemsession.core.configs.MetadataSchemaFieldMappingConfig;
import com.aemsession.core.services.MetadataSchemaFieldMappingService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * This is impl service file of MetadataSchemaFieldMappingService config.
 * */
@Component(service = MetadataSchemaFieldMappingService.class, immediate = true)
@Designate(ocd = MetadataSchemaFieldMappingConfig.class)
public class MetadataSchemaFieldMappingServiceImpl implements MetadataSchemaFieldMappingService {

    public static final Logger LOG = LoggerFactory.getLogger(MetadataSchemaFieldMappingServiceImpl.class);

    private MetadataSchemaFieldMappingConfig metadataSchemaFieldConfig;

    @Activate
    protected void activate(MetadataSchemaFieldMappingConfig config) {
        metadataSchemaFieldConfig = config;
    }

    /**
     * This method will give values of entered values in OSGi Config
     * @return String[]
     * */
    @Override
    public String[] fieldValues() {
        return metadataSchemaFieldConfig.fieldValues();
    }

    /**
     * This method will give mapping of json object and metadata schema field.
     * @return Map<String, String>
     * */

    @Override
    public Map<String, String> getJsonAndMetaDataSchemaMap() {
        Map<String, String> jsonAndMetadataMap = new HashMap<>();
        String[] configFieldValues = metadataSchemaFieldConfig.fieldValues();
        for (String fieldValue : configFieldValues) {
            String[] split = fieldValue.split(":");
            jsonAndMetadataMap.put(split[0], split[1]);
        }
        LOG.info("JsonAndMetadataMap : {}", jsonAndMetadataMap);
        return jsonAndMetadataMap;
    }
}

