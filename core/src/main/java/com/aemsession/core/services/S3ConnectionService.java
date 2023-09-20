package com.aemsession.core.services;

import com.amazonaws.services.s3.AmazonS3;

public interface S3ConnectionService {

    public String getS3BucketName();
    public String getAWSRegion();
    public AmazonS3 getS3Connection();

}
