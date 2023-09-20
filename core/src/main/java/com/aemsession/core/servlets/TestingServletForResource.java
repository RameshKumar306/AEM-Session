package com.aemsession.core.servlets;

import com.aemsession.core.utils.ResolverUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;

@Component(service = Servlet.class, immediate = true)
@SlingServletPaths(value = "/bin/testingResourceExistance")
public class TestingServletForResource extends SlingAllMethodsServlet {

    public static final Logger LOG = LoggerFactory.getLogger(TestingServletForResource.class);

    @Reference
    ResourceResolverFactory resourceResolverFactory;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        try {
            ResourceResolver resourceResolver = ResolverUtil.newResolver(resourceResolverFactory);
//            ResourceResolver resourceResolver = request.getResourceResolver();
            String resourcePath = request.getParameter("resourcePath");
            Resource resource = resourceResolver.getResource(resourcePath);
            LOG.info("Resource : " + resource.getPath());
            response.getWriter().write("Resource Found at : " + resource.getPath());
        } catch (Exception e) {
            LOG.info("Error With Resource : " + e);
        }

    }
}
