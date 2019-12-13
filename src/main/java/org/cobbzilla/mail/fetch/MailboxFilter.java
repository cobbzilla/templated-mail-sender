package org.cobbzilla.mail.fetch;

public interface MailboxFilter {

    boolean matches (MailboxMessage message);

}
