package com.aemsession.core.services.impl;

import com.aemsession.core.configs.S3ConnectionConfig;
import com.aemsession.core.services.S3ConnectionService;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.*;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.amazonaws.services.securitytoken.model.Credentials;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = S3ConnectionService.class, immediate = true)
@Designate(ocd = S3ConnectionConfig.class)
public class S3ConnectionServiceImpl implements S3ConnectionService {

    public static final Logger LOG = LoggerFactory.getLogger(S3ConnectionServiceImpl.class);

    private S3ConnectionConfig s3ConnectionConfig;

    @Activate
    protected void activate(S3ConnectionConfig config) {
        s3ConnectionConfig = config;
    }

    @Override
    public String getS3BucketName() {
        return s3ConnectionConfig.bucketName();
    }

    @Override
    public String getAWSRegion() {
        return s3ConnectionConfig.awsRegion();
    }

    @Override
    public AmazonS3 getS3Connection() {
        final String clientRegion = s3ConnectionConfig.awsRegion();
        final String roleARN = s3ConnectionConfig.assumeRoleARN();
        final String roleSessionName = s3ConnectionConfig.assumeRoleSessionName();
        try {
            AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder.standard()
                    .withCredentials(new ProfileCredentialsProvider())
                    .withRegion(clientRegion)
                    .build();
            AssumeRoleRequest roleRequest = new AssumeRoleRequest()
                    .withRoleArn(roleARN)
                    .withRoleSessionName(roleSessionName);
            AssumeRoleResult roleResponse = stsClient.assumeRole(roleRequest);
            Credentials sessionCredentials = roleResponse.getCredentials();
            BasicSessionCredentials awsCredentials = new BasicSessionCredentials(
                    sessionCredentials.getAccessKeyId(),
                    sessionCredentials.getSecretAccessKey(),
                    sessionCredentials.getSessionToken());
            return AmazonS3ClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                    .withRegion(s3ConnectionConfig.awsRegion())
                    .build();
        }
        catch(AmazonServiceException e) {
            LOG.error("AmazonServiceException while connecting with S3 : {}", e);
        }
        catch(SdkClientException e) {
            LOG.error("SdkClientException while connecting with S3 : {}", e);
        }
        return null;
    }
}
