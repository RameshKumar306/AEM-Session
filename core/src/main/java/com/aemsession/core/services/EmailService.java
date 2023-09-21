package com.aemsession.core.services;

import com.day.cq.commons.mail.MailTemplate;
import com.day.cq.mailer.MessageGatewayService;
import org.apache.commons.lang.text.StrLookup;

public interface EmailService {

    public boolean sendEmail(final MessageGatewayService messageGatewayService,
                          final MailTemplate mailTemplate, final StrLookup lookupMap, final String fromAddress,
                          final String toAddress, final String subject);

}
