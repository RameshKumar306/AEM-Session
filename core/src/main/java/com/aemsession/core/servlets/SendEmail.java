package com.aemsession.core.servlets;

import com.day.cq.commons.mail.MailTemplate;
import com.day.cq.mailer.MessageGateway;
import com.day.cq.mailer.MessageGatewayService;
import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@Component(service = Servlet.class, immediate = true)
@SlingServletPaths(value = "/bin/sendEmail")
public class SendEmail extends SlingSafeMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(SendEmail.class);

    @Reference
    private MessageGatewayService messageGatewayService;

    private ResourceResolver resourceResolver;

    private Session session;

    private static final String toMail = "ramesh.kumar@grazitti.com";

    private static final String fromMail = "rameshlpu11708643@gmail.com";

    private static final String emailTemplate = "/etc/notification/email/signet/testMail.html";


    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        resourceResolver = request.getResourceResolver();
        session = resourceResolver.adaptTo(Session.class);
        final MailTemplate mailTemplate = MailTemplate.create(emailTemplate, session);
        LOG.debug("Sending Email to {}", toMail);
        LOG.debug("Template: {}", mailTemplate);
        final Map<String, String> contentMap = new HashMap<String, String>();
        contentMap.put("recieverName", "Ramesh Kumar");
        contentMap.put("signetTeam", "Signet Team");
        final StrLookup lookup = StrLookup.mapLookup(contentMap);
        sendMail(messageGatewayService, mailTemplate, lookup, fromMail, toMail, "Metadata Update");
        final PrintWriter out = response.getWriter();
        out.print("Success");
        out.flush();
    }

    public void sendMail(final MessageGatewayService messageGatewayService,
                         final MailTemplate mailTemplate, final StrLookup lookupMap, final String fromAddress,
                         final String toAddress, final String subject) {
        try {
            if (null != mailTemplate && null != lookupMap) {
                final HtmlEmail email = mailTemplate.getEmail(lookupMap, HtmlEmail.class);
                final List<InternetAddress> toAddresses = initializeListOfInternetAddress(toAddress);
                if (!toAddresses.isEmpty() && null != fromAddress && null != subject) {
                    email.setTo(toAddresses);
                    email.setSubject(subject);
                    email.setFrom(fromAddress);
                    final MessageGateway<HtmlEmail> messageGateway = messageGatewayService.getGateway(HtmlEmail.class);
                    messageGateway.send(email);
                    LOG.debug("Email sent to to {}", email);
                }
            }
        } catch (EmailException | IOException | MessagingException e) {
            LOG.error("Exception", e);
        }
    }

    public List<InternetAddress> initializeListOfInternetAddress(final String addressString)
            throws AddressException {

        if (null != addressString) {
            final List<InternetAddress> returnList = new ArrayList<>();
            for (String address : addressString.split(",")) {
                returnList.add(new InternetAddress(address));
            }
            return returnList;
        }
        return Collections.emptyList();
    }
}
