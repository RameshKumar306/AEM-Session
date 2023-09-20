package com.aemsession.core.services;

import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;

import javax.jcr.Session;
import java.util.Map;

public interface QueryBuilderService {

    public SearchResult getQueryResult(QueryBuilder queryBuilder, Session session, String damFolderPath, double productIDMetadataValue);

}
