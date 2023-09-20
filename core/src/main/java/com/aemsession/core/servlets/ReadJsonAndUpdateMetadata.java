package com.aemsession.core.servlets;

import com.aemsession.core.services.S3ConnectionService;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component(service = Servlet.class, immediate = true)
@SlingServletPaths(value = "/bin/readPimJsonAndUpdateMetadata")
public class ReadJsonAndUpdateMetadata extends SlingSafeMethodsServlet {

    public static final Logger LOG = LoggerFactory.getLogger(ReadJsonAndUpdateMetadata.class);

    @Reference
    private S3ConnectionService s3ConnectionService;

    @Reference
    private QueryBuilder queryBuilder;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        ResourceResolver resourceResolver = request.getResourceResolver();
        Session session = resourceResolver.adaptTo(Session.class);
        try {
            InputStream jsonStream = getJsonStreamFromS3Bucket("PIM Jsons/PIM JSON structure.json");
            String jsonDataString = IOUtils.toString(jsonStream, StandardCharsets.UTF_8);
            JSONArray jsonArray = new JSONArray(jsonDataString);

            List<String> updatedAssets = new ArrayList<>();
            for (int i = 0;i < jsonArray.length();i++) {
                JSONObject signetObject = jsonArray.getJSONObject(i).getJSONObject("_signet");
                String productId = signetObject.getJSONObject("signetPIM").get("productId").toString();
                String skuValue = signetObject.getJSONObject("signetProduct").get("sku").toString();
                Node assetNode = getAemAssetNodeBasedOnProductIdAndSku(productId, skuValue, session);
                Map<String, String> pimMetadataMap = getPimMetadata(jsonArray, signetObject, i);
                String updatedAssetName = updateAssetMetadata(assetNode, pimMetadataMap);
                updatedAssets.add(updatedAssetName);
            }
            session.save();
            response.getWriter().write("Updated Assets with Metadata : " + updatedAssets);

        } catch (JSONException | RepositoryException e) {
            LOG.error("RepositoryException while reading json :( | {}", e);
        } finally {
            if (null != resourceResolver) {
                resourceResolver.close();
            }
            if (null != session) {
                session.logout();
            }
        }
    }

    private Node getAemAssetNodeBasedOnProductIdAndSku(String productId, String sku, Session session) throws RepositoryException {
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("path", "/content/dam/aemsession/test");
        queryMap.put("type", "dam:Asset");
        queryMap.put("1_property", "@jcr:content/metadata/productId");
        queryMap.put("1_property.value", productId);
        queryMap.put("1_property.operation", "equals");
        queryMap.put("2_property", "@jcr:content/metadata/sku");
        queryMap.put("2_property.value", sku);
        queryMap.put("2_property.operation", "equals");
        queryMap.put("property.and", true);
        queryMap.put("p.limit", "-1");
        Query query = queryBuilder.createQuery(PredicateGroup.create(queryMap), session);
        SearchResult result = query.getResult();
        Node assetNode = null;
        if (result.getHits().size() == 1) {
            assetNode = result.getHits().get(0).getResource().adaptTo(Node.class);
        }
        return assetNode;
    }

    private Map<String, String> getPimMetadata(JSONArray jsonArray, JSONObject signetObject, int index) {
        Map<String, String> pimMetadata = new HashMap<>();
        try {
            Iterator keys = signetObject.keys();
            while (keys.hasNext()) {
                JSONObject nextObject = signetObject.getJSONObject(keys.next().toString());
                Iterator innerJsonKeys = nextObject.keys();
                while (innerJsonKeys.hasNext()) {
                    String objectKey = innerJsonKeys.next().toString();
                    String objectValue = nextObject.get(objectKey).toString();
                    if (objectValue != null && !objectValue.isEmpty()) {
                        pimMetadata.put(objectKey, objectValue);
                    }
                }
            }
            JSONObject extSourceSystemAudit = jsonArray.getJSONObject(index).getJSONObject("extSourceSystemAudit");
            String sourceKey = extSourceSystemAudit.getJSONObject("externalKey").get("sourceKey").toString();
            String lastUpdatedDate = extSourceSystemAudit.get("lastUpdatedDate").toString();
            pimMetadata.put("sourceKey", sourceKey);
            pimMetadata.put("lastUpdatedDate", lastUpdatedDate);
        } catch (JSONException e) {
            LOG.error("JSONException while reading json :( | {}", e);
        }
        return pimMetadata;
    }

    private String updateAssetMetadata(Node assetNode, Map<String, String> pimMetadataMap) throws RepositoryException {
        if (null != assetNode) {
            Node assetMetadataNode = assetNode.getNode("jcr:content/metadata");
            for (Map.Entry<String, String> metadataEntry : pimMetadataMap.entrySet()) {
                assetMetadataNode.setProperty(metadataEntry.getKey(), metadataEntry.getValue());
            }
            return assetNode.getName();
        }
        return StringUtils.EMPTY;
    }

    private InputStream getJsonStreamFromS3Bucket(String objectKey) {


        AmazonS3 s3Connection = s3ConnectionService.getS3Connection();


        S3Object s3Object = s3Connection.getObject(s3ConnectionService.getS3BucketName(), objectKey);
        InputStream s3ObjectJsonStream = s3Object.getObjectContent().getDelegateStream();
        return s3ObjectJsonStream;
    }
}
