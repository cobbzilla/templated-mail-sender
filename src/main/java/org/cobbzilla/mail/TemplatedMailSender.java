package org.cobbzilla.mail;

import com.github.jknack.handlebars.Handlebars;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.handlebars.HandlebarsUtil;
import org.cobbzilla.util.io.StreamUtil;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@Slf4j @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class TemplatedMailSender {

    // things from the email to put into handlebars scope, in addition to user-supplied parameters
    private static final String SCOPE_TO_NAME = "toName";
    private static final String SCOPE_TO_EMAIL = "toEmail";
    private static final String SCOPE_FROM_NAME = "fromName";
    private static final String SCOPE_FROM_EMAIL = "fromEmail";
    private static final String SCOPE_BCC = "bcc";

    // template suffixes
    public static final String FROMEMAIL_SUFFIX = ".fromEmail";
    public static final String FROMNAME_SUFFIX = ".fromName";
    public static final String CC_SUFFIX = ".cc";
    public static final String BCC_SUFFIX = ".bcc";
    public static final String SUBJECT_SUFFIX = ".subject";
    public static final String TEXT_SUFFIX = ".textMessage";
    public static final String HTML_SUFFIX = ".htmlMessage";

    @Getter @Setter protected MailSender mailSender;
    private Handlebars getHandlebars() { return mailSender.getHandlebars(); }

    @Getter @Setter protected File fileRoot;

    public void deliverMessage (TemplatedMail mail) throws Exception {
        mailSender.send(prepareMessage(mail, fileRoot));
    }

    public void deliverMessage (TemplatedMail mail, MailSuccessHandler successHandler, MailErrorHandler errorHandler) {
        try {
            mailSender.send(prepareMessage(mail, fileRoot));
            if (successHandler != null) {
                try {
                    successHandler.handleSuccess(mail);
                } catch (Exception e) {
                    die("Error calling successHandler (" + successHandler + "): " + e, e);
                }
            }
        } catch (Exception e) {
            if (errorHandler != null) errorHandler.handleError(this, mail, successHandler, e);
        }
    }

    public SimpleEmailMessage prepareMessage (TemplatedMail mail, File fileRoot) throws Exception {

        Map<String, Object> scope = mail.getParameters();
        if (scope == null) {
            log.warn("No parameters found to put in scope");
            scope = new HashMap<>();
        }
        final String locale = mail.getLocale();
        final String templateName = mail.getTemplateName();
        if (!mail.hasFromEmail()) {
            mail.setFromName(render(templateName, locale, scope, FROMNAME_SUFFIX));
            mail.setFromEmail(render(templateName, locale, scope, FROMEMAIL_SUFFIX));
            if (!mail.hasFromEmail()) {
                throw new IllegalArgumentException("fromEmail not set and no fromEmail template could be mustache.rendered for template: " + templateName);
            }
        }

        String cc = render(templateName, locale, scope, CC_SUFFIX);
        if (!empty(mail.getCc())) cc = (cc == null ? "" : cc + ", " ) + mail.getCc();

        String bcc = render(templateName, locale, scope, BCC_SUFFIX);
        if (!empty(mail.getBcc())) bcc = (bcc == null ? "" : bcc + ", ") + mail.getBcc();

        // we do not put "cc" and "bcc" into scope, as they should not be needed in the subject or textBody
        if (mail.hasFromName()) scope.put(SCOPE_FROM_NAME, mail.getFromName());
        scope.put(SCOPE_FROM_EMAIL, mail.getFromName());

        if (mail.getToName() != null) scope.put(SCOPE_TO_NAME, mail.getToName());
        scope.put(SCOPE_TO_EMAIL, mail.getToEmail());

        final String subject = renderSubject(locale, scope, templateName);
        if (subject == null) {
            throw new IllegalArgumentException("No subject template could be mustache.rendered for template: "+templateName+" (locale="+mail.getLocale()+")");
        }
        final String textBody = renderTextBody(locale, scope, templateName);
        if (textBody == null) {
            throw new IllegalArgumentException("No text body template could be mustache.rendered for template: "+templateName+" (locale="+mail.getLocale()+")");
        }

        String htmlBody = renderHtmlBody(locale, scope, templateName);

        final SimpleEmailMessage emailMessage = new SimpleEmailMessage();
        emailMessage.setFromName(mail.getFromName());
        emailMessage.setFromEmail(mail.getFromEmail());
        emailMessage.setToName(mail.getToName());
        emailMessage.setToEmail(mail.getToEmail());
        emailMessage.setCc(cc);
        emailMessage.setBcc(bcc);
        emailMessage.setMessage(textBody);
        emailMessage.setHtmlMessage(htmlBody);
        emailMessage.setSubject(subject);
        emailMessage.setImages(mail.getImages());
        emailMessage.setAttachments(mail.getAttachments());
        return emailMessage;
    }

    private String render(String templateName, String locale, Map<String, Object> scope, String suffix) {
        try {
            String template = null;
            try {
                if (!empty(locale)) {
                    template = StreamUtil.loadResourceAsString(templateName + suffix + "_" + locale);
                }
            } catch (Exception ignored) {}
            if (empty(template)) {
                template = StreamUtil.loadResourceAsString(templateName + suffix);
            }
            return HandlebarsUtil.apply(getHandlebars(), template, scope);

        } catch (Exception e) {
            log.warn("render error ("+templateName+suffix+"): "+e);
            return null;
        }
    }

    public String renderSubject(String locale, Map<String, Object> scope, String templateName) {
        return render(templateName, locale, scope, SUBJECT_SUFFIX);
    }

    public String renderTextBody(String locale, Map<String, Object> scope, String templateName) {
        return render(templateName, locale, scope, TEXT_SUFFIX);
    }

    public String renderHtmlBody(String locale, Map<String, Object> scope, String templateName) {
        return render(templateName, locale, scope, HTML_SUFFIX);
    }

    protected String sanitizeMessage(Object message) {
        String data = message.toString();
        final int firstCurly = data.indexOf("{");
        if (firstCurly == -1) {
            throw new IllegalArgumentException("No opening curly brace found: "+data);
        }
        data = data.substring(firstCurly);
        int lastCurly = data.lastIndexOf("}");
        if (lastCurly == -1) {
            throw new IllegalArgumentException("No closing curly brace found: "+data);
        }
        data = data.substring(0, lastCurly+1);
        return data;
    }

}
