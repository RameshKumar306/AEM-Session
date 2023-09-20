package com.aemsession.core.servlets;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component(service = Servlet.class, immediate = true)
@SlingServletPaths(value = "/bin/readWriteData")
public class ReadWriteDataFromExcel extends SlingSafeMethodsServlet {

    public static final Logger LOG = LoggerFactory.getLogger(ReadWriteDataFromExcel.class);

    @Reference
    private QueryBuilder queryBuilder;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String excelFilePath = request.getParameter("excelPath");
        String damFolderPath = request.getParameter("damFolderPath");
        ResourceResolver resourceResolver = request.getResourceResolver();
        Session session = resourceResolver.adaptTo(Session.class);

        FileInputStream excelFileStream = null;
        XSSFWorkbook workbook = null;
        try {
            excelFileStream = new FileInputStream(excelFilePath);
            workbook = new XSSFWorkbook(excelFileStream);
            Sheet sheet = workbook.getSheetAt(0);
            int i = 0;
            for (Row row : sheet) {
                if (i != 0) {
                    double productIDMetadataValue = row.getCell(0).getNumericCellValue();
                    String productNameMetadataValue = row.getCell(1).getStringCellValue();

                    Map<String, Object> queryMap = new HashMap<>();
                    queryMap.put("path", damFolderPath);
                    queryMap.put("type", "dam:Asset");
                    queryMap.put("1_property", "jcr:content/metadata/productID");
                    queryMap.put("1_property.value", String.valueOf(productIDMetadataValue).replace(".0", ""));
                    queryMap.put("1_property.operation", "equals");
                    queryMap.put("p.limit", "-1");
                    Query query = queryBuilder.createQuery(PredicateGroup.create(queryMap), session);
                    SearchResult result = query.getResult();
                    for (Hit hit : result.getHits()) {
                        Resource assetResource = hit.getResource();
                        if (null != assetResource) {
                            Node assetNode = assetResource.adaptTo(Node.class);
                            Node assetMetadataNode = assetNode.getNode("jcr:content/metadata");
                            assetMetadataNode.setProperty("productName", productNameMetadataValue);
                        }
                    }
                    session.save();
                }
                i++;
            }
            response.setContentType("text/html");
            response.getWriter().write("Success!!");
        } catch (IOException exception) {
            LOG.error("Repository Exception While Reading Excel File :( | {} ", exception);
        } catch (RepositoryException e) {
            LOG.error("Repository Exception While checking for Asset :( | {} ", e);
        } finally {
            if (null != resourceResolver) {
                resourceResolver.close();
            }
            if (null != excelFileStream) {
                excelFileStream.close();
            }
            if (null != workbook) {
                workbook.close();
            }
        }
    }
}
