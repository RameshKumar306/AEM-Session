package com.aemsession.core.configs;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "JSON and Metadata Schema field mapping", description = "This Configuration is used to map Metadata Schema Fields with JSON objects.")
public @interface MetadataSchemaFieldMappingConfig {
    @AttributeDefinition(name = "Metadata and Json Mapping", description = "Provide Metadata and Json Mapping (format <Source(json-object)>:<Target(schema-field)> for e.g. productId:product_ID)", type = AttributeType.STRING)
    String[] fieldValues();
}
