package org.cobbzilla.mail.fetch.filter;

import org.cobbzilla.mail.fetch.MailboxMessage;

public class MailboxSubjectFilter extends MailboxRegexFilterBase {

    public MailboxSubjectFilter(String regex) { super(regex); }

    @Override public boolean matches(MailboxMessage message) {
        return !hasRegex() || getPattern().matcher(message.getSubject()).find();
    }

}
