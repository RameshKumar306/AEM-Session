package com.aemsession.core.servlets;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component(service = Servlet.class, immediate = true)
@SlingServletPaths(value = "/bin/querybuilder")
public class QueryTest extends SlingSafeMethodsServlet {

    @Reference
    QueryBuilder queryBuilder;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {

        String damFolderPath = request.getParameter("damFolderPath");
        ResourceResolver resourceResolver = request.getResourceResolver();
        Session session = resourceResolver.adaptTo(Session.class);

        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("path", damFolderPath);
        queryMap.put("type", "dam:Asset");
        queryMap.put("1_property", "jcr:content/metadata/productID");
        queryMap.put("1_property.value", "789");
        queryMap.put("1_property.operation", "equals");
        queryMap.put("p.limit", "-1");
        Query query = queryBuilder.createQuery(PredicateGroup.create(queryMap), session);
        SearchResult result = query.getResult();
        String assetPaths = "";
        for (Hit hit : result.getHits()) {
            try {
                Resource assetResource = hit.getResource();
                if (null != assetResource) {
                    assetPaths = assetResource.getPath() + ", ";
                }
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }

        }
        response.setContentType("text/html");
        response.getWriter().write("Total Hits : " + assetPaths);
    }
}
