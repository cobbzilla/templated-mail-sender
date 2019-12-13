package org.cobbzilla.mail.fetch.filter;

import org.cobbzilla.mail.fetch.MailboxMessage;

import java.util.regex.Pattern;

public class MailboxSenderFilter extends MailboxRegexFilterBase {

    public MailboxSenderFilter(String regex) { super(regex); }

    @Override public boolean matches(MailboxMessage message) {
        if (!hasRegex()) return true;
        final Pattern pattern = getPattern();
        if (pattern.matcher(message.getFromEmail()).find()) return true;
        for (String s : message.getAdditionalSendersList()) {
            if (pattern.matcher(s).find()) return true;
        }
        return false;
    }

}
