package com.aemsession.core.configs;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "S3 Connection", description = "This Configuration is used to establish a connection between S3 and AEM.")
public @interface S3ConnectionConfig {

    @AttributeDefinition(name = "S3 Bucket Name", description = "Provide S3 bucket Name", type = AttributeType.STRING)
    String bucketName();

    @AttributeDefinition(name = "Assume Role ARN", description = "Provide Assume Role ARN of your IAM User", type = AttributeType.STRING)
    String assumeRoleARN();

    @AttributeDefinition(name = "Assume Role Session Name", description = "Use the role session name to uniquely identify a session when the same role is assumed by different principals or for different reasons.", type = AttributeType.STRING)
    String assumeRoleSessionName();

    @AttributeDefinition(name = "AWS Region", description = "Enter AWS Region", type = AttributeType.STRING)
    String awsRegion();

}
