package com.aemsession.core.listeners;

import com.aemsession.core.constants.SignetConstants;
import com.aemsession.core.services.EmailService;
import com.aemsession.core.services.ReadJsonAndUpdateMetadataService;
import com.aemsession.core.utils.ResolverUtil;
import com.day.cq.commons.mail.MailTemplate;
import com.day.cq.mailer.MessageGatewayService;
import com.day.crx.JcrConstants;
import org.apache.commons.lang.text.StrLookup;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.joda.time.DateTime;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Session;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(service = EventHandler.class,
        immediate = true,
        property = {
                EventConstants.EVENT_TOPIC + "=org/apache/sling/api/resource/Resource/ADDED",
                EventConstants.EVENT_FILTER +"=(path=/content/dam/aemsession/pim-jsons/*)"
        })
public class JsonFileListener implements EventHandler {

    private static final Logger LOG = LoggerFactory.getLogger(JsonFileListener.class);

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
    public void handleEvent(Event event) {
        LOG.info("\n Resource event: {} at: {}", event.getTopic(), event.getProperty(SlingConstants.PROPERTY_PATH));
        try {
            String eventPath = event.getProperty(SlingConstants.PROPERTY_PATH).toString();
            if (eventPath.endsWith("/original/jcr:content")) {
                resourceResolver = ResolverUtil.newResolver(resourceResolverFactory);
                Resource resource = resourceResolver.getResource(eventPath);
                session = resourceResolver.adaptTo(Session.class);
                Node eventNode = resource.adaptTo(Node.class);
                if (eventNode.hasProperty(JcrConstants.JCR_DATA)) {
                    InputStream jsonStream = eventNode.getProperty(JcrConstants.JCR_DATA).getBinary().getStream();
                    List<String> updatedMetadata = readJsonAndUpdateMetadataService.readJsonAndUpdateMetadata(jsonStream);
                    LOG.info("Updated Metadata from uploaded Json File : {}", updatedMetadata);
                    boolean mailSent = sendEmailWithUpdatedMetadataAssets(updatedMetadata);
                    if (mailSent) {
                        LOG.info("Email Sent :)");
                    } else {
                        LOG.info("Email Not Sent :(");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
