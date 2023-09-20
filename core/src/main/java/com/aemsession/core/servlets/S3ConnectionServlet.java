package com.aemsession.core.servlets;

import com.aemsession.core.services.S3ConnectionService;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component(service = Servlet.class, immediate = true)
@SlingServletPaths(value = "/bin/s3ConnectionTest")  // to call this servlet hit -
                                                     // http://localhost:4502/bin/s3ConnectionTest?objectKey=<replace with target s3 object key>
public class S3ConnectionServlet extends SlingSafeMethodsServlet {

    public static final Logger LOG = LoggerFactory.getLogger(S3ConnectionServlet.class);

    @Reference
    private S3ConnectionService s3ConnectionService;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String objectKey = request.getParameter("objectKey");
        AmazonS3 s3Connection = s3ConnectionService.getS3Connection();
        if(null != s3Connection) {
            InputStream s3ObjectJsonStream = getObjectStreamFromS3Bucket(s3Connection, objectKey);
            String objectDataString = IOUtils.toString(s3ObjectJsonStream, StandardCharsets.UTF_8);
            response.setContentType("text/html");
            response.getWriter().write(objectDataString);
        }
    }

    private InputStream getObjectStreamFromS3Bucket(AmazonS3 s3Connection, String objectKey) {
        S3Object s3Object = s3Connection.getObject(s3ConnectionService.getS3BucketName(), objectKey);
        InputStream s3ObjectJsonStream = s3Object.getObjectContent().getDelegateStream();
        return s3ObjectJsonStream;
    }
}
