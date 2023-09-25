package com.aemsession.core.listeners;

import com.aemsession.core.constants.SignetConstants;
import com.aemsession.core.services.EmailService;
import com.aemsession.core.services.ReadJsonAndUpdateMetadataService;
import com.aemsession.core.utils.ResolverUtil;
import com.day.cq.commons.mail.MailTemplate;
import com.day.cq.mailer.MessageGatewayService;
import com.day.crx.JcrConstants;
import org.apache.commons.lang.text.StrLookup;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.joda.time.DateTime;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(service = ResourceChangeListener.class,
        immediate = true,
        property = {
                ResourceChangeListener.PATHS + "=" + SignetConstants.PIM_JSONS_PATH_EVENT_FILTER,
                ResourceChangeListener.CHANGES + "=ADDED",
        })
public class PIMJsonFileListener implements ResourceChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(PIMJsonFileListener.class);

    @Reference
    ResourceResolverFactory resourceResolverFactory;

    @Reference
    private ReadJsonAndUpdateMetadataService readJsonAndUpdateMetadataService;

    @Reference
    private EmailService emailService;

    @Reference
    private MessageGatewayService messageGatewayService;

    private ResourceResolver resourceResolver;

    private Session session;

    @Override
    public void onChange(List<ResourceChange> resourceChangeList) {
        for (ResourceChange resourceChange : resourceChangeList) {
            String jsonFilePath = resourceChange.getPath();
            String jsonFileName = jsonFilePath.substring(jsonFilePath.lastIndexOf("/") + 1);
            List<String> updatedMetadataList = new ArrayList<>();
            try {
                if (jsonFilePath.endsWith(SignetConstants.JSON_EXTENSION)) {
                    resourceResolver = ResolverUtil.newResolver(resourceResolverFactory);
                    Resource resource = resourceResolver.getResource(jsonFilePath + SignetConstants.NN_ORIGINAL_RENDITION_JCR_CONTENT);
                    session = resourceResolver.adaptTo(Session.class);
                    if (null != resource) {
                        Node eventNode = resource.adaptTo(Node.class);
                        if (eventNode.hasProperty(JcrConstants.JCR_DATA)) {
                            InputStream jsonStream = eventNode.getProperty(JcrConstants.JCR_DATA).getBinary().getStream();
                            List<String> failuresList = readJsonAndUpdateMetadataService
                                    .readJsonAndUpdateMetadata(jsonStream, updatedMetadataList);
                            LOG.info("Updated Metadata from uploaded Json File : {}", updatedMetadataList);
                            if (failuresList.isEmpty()) {
                                moveJsonFileToArchive(session, jsonFilePath, SignetConstants.ARCHIVED_PIM_JSONS_PATH, jsonFileName);
                                boolean mailSent = sendEmailWithUpdatedMetadataAssets(updatedMetadataList);
                                if (mailSent) {
                                    LOG.info("Email Sent :)");
                                } else {
                                    LOG.info("Email Not Sent :(");
                                }
                            }
                        }
                    }
                }
            } catch (RepositoryException | LoginException e) {
                LOG.error("Error while Listening Json : {}", e);
            }
        }
    }

    private void moveJsonFileToArchive(Session session, String jsonPath, String targetPath, String jsonFileName) {
        try {
            int fileIterator = 1;
            if (!session.itemExists(targetPath + "/" + jsonFileName)) {
                session.move(jsonPath, targetPath + "/" + jsonFileName);
            } else {
                while (session.itemExists(targetPath + "/" + jsonFileName)) {
                    jsonFileName = jsonFileName + "-" + fileIterator;
                    fileIterator++;
                }
                session.move(jsonPath, targetPath + "/" + jsonFileName);
            }
            session.save();
        } catch (RepositoryException e) {
            LOG.error("Error while moving json file to archive : {}", e);
        }
    }

    private boolean sendEmailWithUpdatedMetadataAssets(List<String> updatedMetadata) {
        final MailTemplate mailTemplate = MailTemplate.create(SignetConstants.EMAIL_TEMPLATE, session);
        LOG.info("Sending Email to {}", SignetConstants.TO_MAIL);
        LOG.info("Template: {}", mailTemplate);
        final Map<String, String> contentMap = new HashMap<String, String>();
        contentMap.put("recieverName", "Ramesh Kumar");
        contentMap.put("signetTeam", "Signet Team");
        contentMap.put("updatedMetadata", updatedMetadata.toString());
        contentMap.put("timeStamp", DateTime.now().toString());
        final StrLookup lookup = StrLookup.mapLookup(contentMap);
        return emailService.sendEmail(messageGatewayService, mailTemplate, lookup, SignetConstants.FROM_MAIL,
                SignetConstants.TO_MAIL, SignetConstants.EMAIL_SUBJECT);
    }
}
