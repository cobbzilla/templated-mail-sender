package org.cobbzilla.mail;

import com.github.jknack.handlebars.Handlebars;
import org.apache.commons.mail.EmailException;
import org.cobbzilla.mail.sender.SmtpMailConfig;

/**
 * (c) Copyright 2013 Jonathan Cobb.
 * This code is available under the Apache License, version 2: http://www.apache.org/licenses/LICENSE-2.0.html
 */
public interface MailSender {

    void setConfig(SmtpMailConfig mailConfig);

    void send(SimpleEmailMessage message) throws EmailException;

    Handlebars getHandlebars ();

}
