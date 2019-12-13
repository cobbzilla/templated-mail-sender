package org.cobbzilla.mail.sender;

import com.github.jknack.handlebars.Handlebars;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.mail.EmailException;
import org.cobbzilla.mail.SimpleEmailAttachment;
import org.cobbzilla.mail.SimpleEmailMessage;
import org.cobbzilla.util.collection.SingletonList;
import org.cobbzilla.util.handlebars.ContextMessageSender;

import java.io.File;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.http.HttpContentTypes.fileExt;
import static org.cobbzilla.util.io.FileUtil.toTempFile;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class ContextEmailSender implements ContextMessageSender {

    @Getter @Setter private SmtpMailConfig smtp;
    @Getter @Setter private SimpleEmailMessage defaults;
    @Getter @Setter private SmtpMailSender sender;

    public ContextEmailSender(SmtpMailConfig smtp, Handlebars handlebars, SimpleEmailMessage defaults) {
        this.smtp = smtp;
        this.sender = new SmtpMailSender(smtp, handlebars);

        if (defaults == null || !defaults.hasSubject() || !defaults.hasFromEmail()) {
            die("ContextEmailSender: no subject or fromEmail defined in defaults");
        }

        this.defaults = defaults;
    }

    @Override public void send(String recipient, String subject, String message, String contentType) {

        final String ext = fileExt(contentType);
        final File temp = toTempFile(message, ext);
        temp.deleteOnExit();

        final SimpleEmailMessage msg = new SimpleEmailMessage();
        msg.setToEmail(recipient);
        msg.setSubject(empty(subject) ? defaults.getSubject() : subject);
        msg.setFromEmail(defaults.getFromEmail());
        msg.setFromName(defaults.getFromName());
        msg.setCc(defaults.getCc());
        msg.setBcc(defaults.getBcc());

        final SimpleEmailAttachment attachment = new SimpleEmailAttachment(temp);
        attachment.setContentType(contentType);
        attachment.setName("context"+ext);

        msg.setAttachments(new SingletonList<>(attachment));
        msg.setMessage("Context file is attached");
        try {
            sender.send(msg);
        } catch (EmailException e) {
            die("send: "+e, e);
        }
    }
}
