package com.aemsession.core.services.impl;

import com.aemsession.core.services.EmailService;
import com.day.cq.commons.mail.MailTemplate;
import com.day.cq.mailer.MessageGateway;
import com.day.cq.mailer.MessageGatewayService;
import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This service is a helper service for Email
 * */
@Component(service = EmailService.class, immediate = true)
public class EmailServiceImpl implements EmailService {

    private static final Logger LOG = LoggerFactory.getLogger(EmailServiceImpl.class);

    /**
     * This method is used send email
     * @param messageGatewayService
     * @param mailTemplate
     * @param lookupMap
     * @param fromAddress
     * @param toAddress
     * @param subject
     * @return true if mail sent successfully.
     * */
    @Override
    public boolean sendEmail(MessageGatewayService messageGatewayService, MailTemplate mailTemplate, StrLookup lookupMap, String fromAddress, String toAddress, String subject) {
        boolean isEmailSent = false;
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
                    isEmailSent = true;
                    LOG.debug("Email sent to to {}", email);
                }
            }
        } catch (EmailException | IOException | MessagingException e) {
            LOG.error("Exception", e);
        }
        return isEmailSent;
    }

    /**
     * This method will return list of Internet addresses of String Address
     * @param addressString
     * @return List<InternetAddress>
     * */
    private List<InternetAddress> initializeListOfInternetAddress(final String addressString)
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
