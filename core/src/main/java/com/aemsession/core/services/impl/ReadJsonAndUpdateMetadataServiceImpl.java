package com.aemsession.core.services.impl;

import com.aemsession.core.constants.SignetConstants;
import com.aemsession.core.services.MetadataSchemaFieldMappingService;
import com.aemsession.core.services.ReadJsonAndUpdateMetadataService;
import com.aemsession.core.utils.ResolverUtil;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * This service is used to read json file and update metadata.
 * */
@Component(service = ReadJsonAndUpdateMetadataService.class, immediate = true)
public class ReadJsonAndUpdateMetadataServiceImpl implements ReadJsonAndUpdateMetadataService {

    public static final Logger LOG = LoggerFactory.getLogger(ReadJsonAndUpdateMetadataServiceImpl.class);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private QueryBuilder queryBuilder;

    @Reference
    private MetadataSchemaFieldMappingService metadataSchemaFieldMappingService;

    /**
     * This method is used to read json and update metadata.
     * @param jsonStream
     * @param updatedAssetsList
     * @return List<String>
     * */
    @Override
    public List<String> readJsonAndUpdateMetadata(InputStream jsonStream, List<String> updatedAssetsList) {
        ResourceResolver resourceResolver = null;
        Session session = null;
        List<String> failures = new ArrayList<>();
        try {
            resourceResolver = ResolverUtil.newResolver(resourceResolverFactory);
            session = resourceResolver.adaptTo(Session.class);
            String jsonDataString = IOUtils.toString(jsonStream, StandardCharsets.UTF_8);
            JSONArray jsonArray = new JSONArray(jsonDataString);

            for (int iteration = 0; iteration < jsonArray.length(); iteration++) {
                JSONObject signetObject = jsonArray.getJSONObject(iteration).getJSONObject(SignetConstants.SIGNET_OBJECT);
                String productId = signetObject.getJSONObject(SignetConstants.SIGNET_PIM_OBJECT).get(SignetConstants.PRODUCTID_KEY).toString();
                List<Node> assetNodes = getAemAssetNodesBasedOnProductId(productId, session);
                if (assetNodes.size() >= 1) {
                    Map<String, String> pimMetadataMap = getPimMetadata(jsonArray, signetObject, iteration);
                    Map<String, String> jsonAndMetaDataSchemaMap = metadataSchemaFieldMappingService.getJsonAndMetaDataSchemaMap();
                    for (Node assetNode : assetNodes) {
                        String updatedAssetName = updateAssetMetadata(assetNode, pimMetadataMap, jsonAndMetaDataSchemaMap, session);
                        if (!updatedAssetsList.contains(updatedAssetName)) {
                            updatedAssetsList.add(updatedAssetName);
                        }
                    }
                } else {
                    failures.add(productId);
                }
            }
        } catch (JSONException | RepositoryException | LoginException | IOException e) {
            LOG.error("Exception while reading json :( | {}", e);
        } finally {
            if (null != resourceResolver) {
                resourceResolver.close();
            }
            if (null != session) {
                session.logout();
            }
        }
        return failures;
    }

    /**
     * This method fetched the nodes based on product ids of json file.
     * @param productId
     * @param session
     * @return List<Node>
     * @throws RepositoryException
     * */
    private List<Node> getAemAssetNodesBasedOnProductId(String productId, Session session) throws RepositoryException {
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put(SignetConstants.PATH, SignetConstants.DAM_PRODUCTID_SEARCH_PATH);
        queryMap.put(SignetConstants.TYPE, SignetConstants.DAM_ASSET);
        queryMap.put(SignetConstants.ONE_PROPERTY, SignetConstants.SEARCH_PROPERTY_PRODUCTID);
        queryMap.put(SignetConstants.ONE_PROPERTY_DOT_VALUE, productId);
        queryMap.put(SignetConstants.ONE_PROPERTY_DOT_OPERATION, SignetConstants.SEARCH_OPERATION_EQUALS);
        queryMap.put(SignetConstants.P_DOT_LIMIT, "-1");
        Query query = queryBuilder.createQuery(PredicateGroup.create(queryMap), session);
        SearchResult result = query.getResult();
        List<Node> assetNodes = new ArrayList<>();
        for (Hit hit : result.getHits()) {
            Node assetNode = hit.getResource().adaptTo(Node.class);
            assetNodes.add(assetNode);
        }
        return assetNodes;
    }

    /**
     * This method will give metadata map to be updated for the assets.
     * @param jsonArray
     * @param signetObject
     * @param index
     * @return Map<String, String>
     * */
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
            JSONObject extSourceSystemAudit = jsonArray.getJSONObject(index).getJSONObject(SignetConstants.EXT_SOURCE_SYSTEM_AUDIT_OBJECT);
            String sourceKey = extSourceSystemAudit.getJSONObject(SignetConstants.EXTERNAL_KEY_OBJECT).get(SignetConstants.SOURCE_KEY_OBJECT_KEY).toString();
            String lastUpdatedDate = extSourceSystemAudit.get(SignetConstants.LAST_UPDATED_DATE).toString();
            pimMetadata.put(SignetConstants.SOURCE_KEY_OBJECT_KEY, sourceKey);
            pimMetadata.put(SignetConstants.LAST_UPDATED_DATE, lastUpdatedDate);
        } catch (JSONException e) {
            LOG.error("JSONException while reading json :( | {}", e);
        }
        return pimMetadata;
    }

    /**
     * This method will update the asset's metadata.
     * @param session
     * @param assetNode
     * @param jsonAndSchemaFieldMap
     * @param pimMetadataMap
     * @return String
     * @throws RepositoryException
     * */
    private String updateAssetMetadata(Node assetNode, Map<String, String> pimMetadataMap, Map<String, String> jsonAndSchemaFieldMap, Session session) throws RepositoryException {
        if (null != assetNode) {
            Node assetMetadataNode = assetNode.getNode(SignetConstants.NN_ASSET_METADATA);
            for (Map.Entry<String, String> metadataJsonMapEntry : jsonAndSchemaFieldMap.entrySet()) {
                if (pimMetadataMap.containsKey(metadataJsonMapEntry.getKey())
                        && null != pimMetadataMap.get(metadataJsonMapEntry.getKey())
                        && !"null".equals(pimMetadataMap.get(metadataJsonMapEntry.getKey()))) {
                    assetMetadataNode.setProperty(metadataJsonMapEntry.getValue(), pimMetadataMap.get(metadataJsonMapEntry.getKey()));
                    session.save();
                }
            }
            return assetNode.getName();
        }
        return StringUtils.EMPTY;
    }
}
