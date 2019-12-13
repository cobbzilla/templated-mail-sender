package org.cobbzilla.mail.fetch;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum MailboxProtocol {

    pop3, imap;

    @JsonCreator public static MailboxProtocol fromString (String val) { return valueOf(val.toLowerCase()); }

    public String getJavaProtocol (boolean secure) { return secure ? name() + "s" : name(); }

}
