package org.cobbzilla.mail.sender.mock;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.mail.MailErrorHandler;
import org.cobbzilla.mail.MailSuccessHandler;
import org.cobbzilla.mail.TemplatedMail;
import org.cobbzilla.mail.TemplatedMailSender;
import org.cobbzilla.util.collection.mappy.MappyList;
import org.cobbzilla.util.reflect.ReflectionUtil;
import org.cobbzilla.util.string.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@Slf4j
public class MockTemplatedMailSender extends TemplatedMailSender {

    public static final String EMAIL_CONTENT = "emailContent";

    @Getter private final AtomicReference<MappyList<String, TemplatedMail>> messagesRef = new AtomicReference<>(new MappyList<String, TemplatedMail>());
    public MappyList<String, TemplatedMail> getMessages () { return messagesRef.get(); }

    public List<MockMailbox> getAll () {
        final List<MockMailbox> all = new ArrayList<>();
        final MappyList<String, TemplatedMail> messages = getMessages();
        for (String recipient : messages.keySet()) {
            all.add(new MockMailbox(recipient, messages.getAll(recipient)));
        }
        return all;
    }

    public MockMailbox mailbox (String email) { return new MockMailbox(email, getMessages().getAll(email)); }

    public TemplatedMail getFirstMessage () { return getMessages().entrySet().iterator().next().getValue(); }

    public int messageCount () { return getMessages().size(); }
    public MockTemplatedMailSender reset () {
        messagesRef.set(new MappyList<String, TemplatedMail>());
        final MappyList<String, TemplatedMail> messages = getMessages();
        log.info(getClass().getSimpleName()+".reset: messages("+System.identityHashCode(messages)+") now count="+messages.flatten().size());
        return this;
    }
    public TemplatedMail first() { return getMessages().isEmpty() ? null : getFirstMessage(); }

    @Override public void deliverMessage(TemplatedMail mail) throws Exception {
        // log.info(getClass().getSimpleName()+".deliverMessage: "+mail);
        if (mail.getParameters().get("__debug") != null) return;
        synchronized (messagesRef) {
            final MappyList<String, TemplatedMail> messages = getMessages();
            for (String b : StringUtil.split(mail.getToEmail(), ",")) messages.put(b.trim(), mail);
            if (mail.hasBcc()) for (String b : StringUtil.split(mail.getBcc(), ",")) messages.put(b.trim(), mail);
            if (mail.hasCc()) for (String b : StringUtil.split(mail.getCc(), ",")) messages.put(b.trim(), mail);
            log.info(getClass().getSimpleName() + ".deliverMessage(" + mail.getToEmail() + "/" + getEmailName(mail) + "/" + getEmailHoldType(mail) + "): messages(" + System.identityHashCode(messages) + ") count now=" + messages.flatten().size());
        }
    }

    private String getEmailHoldType(TemplatedMail mail) {
        try {
            return (String) ReflectionUtil.get(mail, "parameters.productHold.holdType");
        } catch (Exception e) {
            return "no-hold-found";
        }
    }

    private String getEmailName(TemplatedMail mail) {
        try {
            return (String) ReflectionUtil.get(mail, "parameters.emailContent.name");
        } catch (Exception e) {
            return mail.getTemplateName();
        }
    }

    @Override public void deliverMessage(TemplatedMail mail, MailSuccessHandler successHandler, MailErrorHandler errorHandler) {
        try {
            deliverMessage(mail);
        } catch (Exception e) {
            if (errorHandler != null) errorHandler.handleError(this, mail, successHandler, e);
        }
        if (successHandler != null) successHandler.handleSuccess(mail);
    }

    public List<TemplatedMail> inbox(String toEmail) { return new ArrayList<>(getMessages().getAll(toEmail)); }

    public TemplatedMail mostRecentMessageTo(String toEmail) {
        final List<TemplatedMail> mails = inbox(toEmail);
        return empty(mails) ? null : mails.get(mails.size()-1);
    }

    @Override public String toString() { return getMessages().toString(); }

    public String debugString () {
        final MappyList<String, TemplatedMail> messages = getMessages();
        final StringBuilder b = new StringBuilder("debug mailbox messages:");
        for (String recipient : messages.keySet()) {

            final List<TemplatedMail> mails = messages.getAll(recipient);
            if (empty(mails)) continue;

            b.append("\n").append(recipient).append(": ");
            final StringBuilder msgBuilder = new StringBuilder();
            for (TemplatedMail m : mails) {
                if (msgBuilder.length() > 0) msgBuilder.append(", ");
                if (m.getParameters().containsKey(EMAIL_CONTENT)) {
                    final Object content = m.getParameters().get(EMAIL_CONTENT);
                    if (ReflectionUtil.hasGetter(content, "name")) {
                        msgBuilder.append(ReflectionUtil.get(content, "name")).append("/").append(getEmailHoldType(m));
                    } else {
                        msgBuilder.append(content.toString());
                    }
                } else {
                    msgBuilder.append(m.getTemplateName());
                }
            }
            b.append(msgBuilder);
        }
        return b.toString();
    }

    @AllArgsConstructor
    public static class MockMailbox {
        @Getter private String recipient;
        @Getter private List<TemplatedMail> mails;
        public TemplatedMail getFirst() { return empty(mails) ? null : mails.get(0); }
        public TemplatedMail getMostRecent() { return empty(mails) ? null : mails.get(mails.size()-1); }
        public boolean hasMessageWithParameter(String param, String path, String match) {
            if (!empty(mails)) {
                for (TemplatedMail m : mails) {
                    final Map<String, Object> params = m.getParameters();
                    if (!empty(params)) {
                        Object value = params.get(param);
                        if (value != null) {
                            if (!empty(path)) value = ReflectionUtil.get(value, path);
                            if (value != null && value.toString().contains(match)) return true;
                        }
                    }
                }
            }
            return false;
        }
    }
}
