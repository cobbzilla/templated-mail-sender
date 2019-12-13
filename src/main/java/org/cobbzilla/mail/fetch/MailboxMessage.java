package org.cobbzilla.mail.fetch;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.mail.SimpleEmailMessage;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.util.string.ValidationRegexes;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@NoArgsConstructor @Slf4j
public class MailboxMessage extends SimpleEmailMessage {

    @Getter @Setter private String messageId;

    @Getter @Setter private String additionalRecipients;
    public void addAdditionalRecipient(String email) {
        this.additionalRecipients = empty(this.additionalRecipients) ? email : this.additionalRecipients + "," + email;
    }
    @JsonIgnore public List<String> getAdditionalRecipientsList () {
        return empty(additionalRecipients) ? Collections.EMPTY_LIST : StringUtil.split(additionalRecipients, ",");
    }

    @Getter @Setter private String additionalSenders;
    public void addAdditionalSender(String email) {
        this.additionalSenders = empty(this.additionalSenders) ? email : this.additionalSenders + "," + email;
    }
    @JsonIgnore public List<String> getAdditionalSendersList () {
        return empty(additionalSenders) ? Collections.EMPTY_LIST : StringUtil.split(additionalSenders, ",");
    }

    public MailboxMessage(Message message, String mailboxOwner) throws IOException, MessagingException {
        Address[] addrs;

        addrs = message.getRecipients(Message.RecipientType.BCC);
        if (addrs != null) for (Address a : addrs) addBcc(extractEmail(a));

        addrs = message.getRecipients(Message.RecipientType.CC);
        if (addrs != null) for (Address a : addrs) addCc(extractEmail(a));

        addrs = message.getRecipients(Message.RecipientType.TO);
        if (addrs != null) {
            for (Address a : addrs) {
                if (getToEmail() == null) {
                    setToEmail(extractEmail(a));
                    setToName(extractName(a));
                } else {
                    addAdditionalRecipient(extractEmail(a));
                }
            }
        }

        addrs = message.getFrom();
        if (addrs != null) {
            for (Address a : addrs) {
                if (getFromEmail() == null) {
                    setFromEmail(extractEmail(a, mailboxOwner));
                    setFromName(extractName(a));
                } else {
                    addAdditionalSender(extractEmail(a));
                }
            }
        }
        setSubject(message.getSubject());

        final String[] messageIds = message.getHeader("Message-ID");
        if (messageIds == null || empty(messageIds[0])) {
            setMessageId("" + message.getMessageNumber());
        } else {
            setMessageId(messageIds[0]);
        }

        addParts(message);
    }

    protected String extractEmail(Address a) {
        return (a instanceof InternetAddress) ? ((InternetAddress) a).getAddress() : a.toString();
    }

    protected String extractEmail(Address a, String usePersonalIfEmailIsThis) {
        final String found = extractEmail(a);
        if (found.equals(usePersonalIfEmailIsThis)) {
            final String shouldBeEmail = extractName(a);
            if (empty(shouldBeEmail)) return found;
            if (!ValidationRegexes.EMAIL_PATTERN.matcher(shouldBeEmail).matches()) {
                log.warn("extractEmail ("+a+"): expected personal field to be proper email address, was "+shouldBeEmail);
                return found;
            }
            return shouldBeEmail;
        }
        return (a instanceof InternetAddress) ? ((InternetAddress) a).getAddress() : a.toString();
    }

    protected String extractName(Address a) {
        return (a instanceof InternetAddress) ? ((InternetAddress) a).getPersonal() : a.toString();
    }

    private void addParts(Part bodyPart) throws MessagingException, IOException {
        String contentType = bodyPart.getContentType();
        if (contentType == null) {
            log.warn("addParts: no Content-Type for part, skipping");
            return;
        }
        contentType = contentType.toLowerCase();
        if (contentType.contains("/html")) {
            safeSetHtmlMessage(bodyPart.getContent().toString());

        } else if (contentType.contains("text/plain")) {
            safeSetMessage(bodyPart.getContent().toString());

        } else if (bodyPart.getContent() instanceof MimeMultipart) {
            final StringBuilder html = new StringBuilder();
            final StringBuilder text = new StringBuilder();
            final MimeMultipart mimeMultipart = (MimeMultipart) bodyPart.getContent();
            for (int i=0; i<mimeMultipart.getCount(); i++) {
                final BodyPart p = mimeMultipart.getBodyPart(i);
                if (p.getContentType().toLowerCase().contains("text/plain")) {
                    text.append(p.getContent());
                } else {
                    html.append(p.getContent());
                }
            }
            safeSetMessage(text.toString());
            safeSetHtmlMessage(html.toString());

        } else if (bodyPart.getFileName() != null) {
            addAttachment(new MailboxAttachment(bodyPart));

        } else if (bodyPart.isMimeType("message/rfc822")) {
            addParts((Part) bodyPart.getContent());

        } else {
            log.warn("MailboxMessage: unrecognized Content-Type for part (treating as String): "+ contentType);
            safeSetMessage(bodyPart.getContent().toString());
        }
    }

    private void safeSetHtmlMessage(String m) {
        if (getHasHtmlMessage()) {
            log.warn("addParts: already have m message, not overwriting");
        } else {
            setHtmlMessage(m);
        }
    }

    private void safeSetMessage(String m) {
        if (!empty(getMessage())) {
            log.warn("addParts: already have text message, not overwriting");
        } else {
            setMessage(m);
        }
    }
}
