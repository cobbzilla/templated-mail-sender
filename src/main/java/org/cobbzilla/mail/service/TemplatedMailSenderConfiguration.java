package org.cobbzilla.mail.service;

import org.cobbzilla.mail.SimpleEmailMessage;
import org.cobbzilla.mail.sender.SmtpMailConfig;
import org.cobbzilla.util.handlebars.HasHandlebars;

import java.util.Map;

public interface TemplatedMailSenderConfiguration extends HasHandlebars {

    SmtpMailConfig getSmtp();

    String getEmailTemplateRoot();

    Map<String, SimpleEmailMessage> getEmailSenderNames();

}
