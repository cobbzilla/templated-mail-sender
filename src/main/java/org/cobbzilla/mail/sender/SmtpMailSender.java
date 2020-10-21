package org.cobbzilla.mail.sender;

import com.github.jknack.handlebars.Handlebars;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.model.Calendar;
import org.apache.commons.mail.*;
import org.cobbzilla.mail.MailSender;
import org.cobbzilla.mail.SimpleEmailAttachment;
import org.cobbzilla.mail.SimpleEmailImage;
import org.cobbzilla.mail.SimpleEmailMessage;
import org.cobbzilla.mail.ical.ICalEvent;
import org.cobbzilla.mail.ical.ICalUtil;
import org.cobbzilla.util.string.Base64;
import org.cobbzilla.util.string.StringUtil;

import javax.activation.DataSource;
import javax.activation.FileDataSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.*;
import static org.cobbzilla.util.string.StringUtil.removeWhitespace;
import static org.cobbzilla.util.system.Sleep.sleep;

/**
 * (c) Copyright 2013-2019 Jonathan Cobb.
 * This code is available under the Apache License, version 2: http://www.apache.org/licenses/LICENSE-2.0.html
 */
@NoArgsConstructor @AllArgsConstructor @Slf4j
public class SmtpMailSender implements MailSender {

    private static final List<String> TEST_DOMAINS = Arrays.asList(
            "@example.com", ".example.com", "@example.net", ".example.net",
            "@example.org", ".example.org", "@example.edu", ".example.edu",
            "@localhost", ".localhost",
            ".example", ".invalid", ".local", ".test"
    );

    @Getter @Setter private SmtpMailConfig config;
    @Getter @Setter private Handlebars handlebars;
    @Getter @Setter private SmtpMailSender debugSender;

    public SmtpMailSender (SmtpMailConfig config) { this.config = config; }

    public SmtpMailSender(SmtpMailConfig smtp, Handlebars handlebars) {
        this(smtp, handlebars, null);
    }

    @Override public void send(SimpleEmailMessage message) throws EmailException {
        final List<String> testEmailAddresses = findTestEmailAddresses(message.getToEmail());
        if (!empty(testEmailAddresses)) {
            if (debugSender == null) {
                log.warn("send: no debugSender to receive emails for: "+StringUtil.toString(testEmailAddresses));
            } else {
                debugSender.send(message);
                return;
            }
        }

        List<String> addresses = splitTrimAndFilterMailAddresses(message.getToEmail());
        if (empty(addresses)) {
            log.info("send: not sending message to " + message.getToEmail());
            return;
        }

        final Email email = constructEmail(message);
        email.setHostName(config.getHost());
        email.setSmtpPort(config.getPort());
        email.setSSL(config.isSslEnabled());
        if (config.getHasMailUser()) {
            email.setAuthenticator(new DefaultAuthenticator(config.getUser(), config.getPassword()));
        }
        email.setTLS(config.isTlsEnabled());
        email.setSubject(message.getSubject());

        if (message.getToName() != null && addresses.size() == 1) {
            email.addTo(addresses.get(0), message.getToName());
        } else {
            if (message.getToName() != null) {
                log.warn("send: email with toName, but with multiple comma separated toEmail-s. Leaving toName blank.");
            }
            for (String a : addresses) email.addTo(a);
        }

        addresses = splitTrimAndFilterMailAddresses(message.getBcc());
        if (!empty(addresses)) for (String a : addresses) email.addBcc(a);

        addresses = splitTrimAndFilterMailAddresses(message.getCc());
        if (!empty(addresses)) for (String a : addresses) email.addCc(a);

        if (message.getFromName() != null) {
            email.setFrom(message.getFromEmail(), message.getFromName());
        } else {
            email.setFrom(message.getFromEmail());
        }

        sendEmail_internal(email);
    }

    private List<String> splitTrimAndFilterMailAddresses(String addresses) {
        if (addresses == null) return null;

        final List<String> result = new ArrayList<>();
        if (addresses.contains(",")) {
            StringUtil.split(addresses, ",").stream().map(String::trim).forEach(e -> {
                if (isValidEmailDomain(e)) result.add(e);
            });
        } else {
            addresses = addresses.trim();
            if (isValidEmailDomain(addresses)) result.add(addresses);
        }
        return result;
    }

    public static boolean isTestDomain(String address) {
        for (String testDomain : TEST_DOMAINS) {
            if (address.endsWith(testDomain)) return true;
        }
        return false;
    }

    private boolean isValidEmailDomain(String address) {
        return isValidEmailDomain(address, config.getDomainWhitelist());
    }

    private static boolean isValidEmailDomain(final String emailAddress, String[] whitelist) {
        if (SmtpMailConfig.isWhitelisted(emailAddress, whitelist)) return false;
        if (isTestDomain(emailAddress)) return false;
        // do not allow commas, semi-colons, or any whitespace in email addresses
        if (emailAddress.contains(",")
                || emailAddress.contains(";")
                || !removeWhitespace(emailAddress).equals(emailAddress)) return false;
        return true;
    }

    public static List<String> findTestEmailAddresses(String addresses) {
        return findTestEmailAddresses(addresses, null);
    }

    public static List<String> findTestEmailAddresses(String addresses, String[] whitelist) {
        if (addresses == null) return Collections.emptyList();

        final List<String> result = new ArrayList<>();
        if (addresses.contains(",")) {
            StringUtil.split(addresses, ",").stream().map(String::trim).forEach(e -> {
                if (!isValidEmailDomain(e, whitelist)) result.add(e);
            });
        } else {
            addresses = addresses.trim();
            if (!isValidEmailDomain(addresses, whitelist)) result.add(addresses);
        }
        return result;
    }

    public static final int MAX_TRIES = 5;

    protected void sendEmail_internal(Email email) throws EmailException {
        long wait = 5000;
        for (int tries=1; tries <= MAX_TRIES; tries++) {
            try {
                email.send();
                return;

            } catch (EmailException e) {
                if (tries < MAX_TRIES) {
                    log.warn("Error sending email (try #"+(tries+1)+", will retry): " + shortError(e));
                    sleep(wait, "waiting to send sending email (try #" + (tries + 1) + ", abandoning)");
                    wait *= 2;

                } else {
                    log.warn("Error sending email (try #"+tries+", abandoning): " + shortError(e));
                    throw e;
                }
            }
        }
    }

    private Email constructEmail(SimpleEmailMessage message) throws EmailException {
        MultiPartEmail email = new MultiPartEmail();
        if (message instanceof ICalEvent) {
            ICalEvent iCalEvent = (ICalEvent) message;

            // Calendar iCalendar = new Calendar();
            Calendar iCalendar = ICalUtil.newCalendarEvent(iCalEvent.getProdId(), iCalEvent);
            byte[] attachmentData = ICalUtil.toBytes(iCalendar);

            String icsName = iCalEvent.getIcsName() + ".ics";
            String contentType = "text/calendar; icsName=\""+icsName+"\"";
            try {
                email.attach(new ByteArrayDataSource(attachmentData, contentType), icsName, "", EmailAttachment.ATTACHMENT);
            } catch (IOException e) {
                throw new EmailException("constructEmail: couldn't attach: "+shortError(e), e);
            }

        } else if (message.getHasHtmlMessage()) {
            final HtmlEmail htmlEmail = new HtmlEmail();
            if (message.hasImages()) {
                for (SimpleEmailImage image : message.getImages()) {
                    switch (image.getIncludeType()) {
                        case base64_embed:
                        case url_embed:
                            final String cid = htmlEmail.embed(image, image.getName());
                            message.setHtmlMessage(message.getHtmlMessage().replaceAll("@IMAGE:"+image.getName(), "cid:"+cid));
                            break;

                        case url_link:
                            message.setHtmlMessage(message.getHtmlMessage().replaceAll("@IMAGE:"+image.getName(), image.getUrl()));
                            break;

                        default:
                            return notSupported("constructEmail - image "+image+" has unsupported includeType: "+image.getIncludeType());
                    }
                }
            }
            htmlEmail.setTextMsg(message.getMessage());
            htmlEmail.setHtmlMsg(message.getHtmlMessage());
            email = htmlEmail;

        } else {
            email.setMsg(message.getMessage());
        }

        if (message.hasAttachments()) {
            for (SimpleEmailAttachment attachment : message.getAttachments()) {
                try {
                    final DataSource ds;
                    if (attachment.hasFile()) {
                        ds = new FileDataSource(attachment.getFile());
                    } else {
                        ds = new ByteArrayDataSource(Base64.decode(attachment.getBase64bytes()), attachment.getContentType());
                    }
                    email.attach(ds, attachment.getName(), attachment.getDescription());
                } catch (IOException e) {
                    throw new EmailException("Error decoding attachment: "+attachment.getName()+": "+shortError(e), e);
                }

            }
        }

        return email;
    }
}
