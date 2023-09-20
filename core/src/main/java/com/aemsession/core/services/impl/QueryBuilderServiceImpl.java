package com.aemsession.core.services.impl;

import com.aemsession.core.services.QueryBuilderService;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import org.osgi.service.component.annotations.Component;

import javax.jcr.Session;
import java.util.HashMap;
import java.util.Map;

@Component(service = QueryBuilderService.class, immediate = true)
public class QueryBuilderServiceImpl implements QueryBuilderService {
    @Override
    public SearchResult getQueryResult(QueryBuilder queryBuilder, Session session, String damFolderPath, double productIDMetadataValue) {
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("path", damFolderPath);
        queryMap.put("type", "dam:Asset");
        queryMap.put("1_property", "jcr:content/metadata/productID");
        queryMap.put("1_property.value", productIDMetadataValue);
        queryMap.put("1_property.operation", "equals");
        queryMap.put("p.limit", "-1");
        Query query = queryBuilder.createQuery(PredicateGroup.create(queryMap), session);
        return query.getResult();
    }
}
