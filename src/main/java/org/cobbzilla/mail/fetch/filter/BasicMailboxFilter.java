package org.cobbzilla.mail.fetch.filter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.Accessors;
import org.cobbzilla.mail.fetch.MailboxFilter;
import org.cobbzilla.mail.fetch.MailboxMessage;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class BasicMailboxFilter implements MailboxFilter {

    @Getter @Setter private String sender;
    @Getter @Setter private String recipient;
    @Getter @Setter private String subject;

    @JsonIgnore @Getter(lazy=true, value=AccessLevel.PRIVATE) private final MailboxFilter[] subFilters = initSubFilters();
    private MailboxFilter[] initSubFilters() {
        return new MailboxFilter[] {
                empty(sender) ? null : new MailboxSenderFilter(sender),
                empty(recipient) ? null : new MailboxRecipientFilter(recipient),
                empty(subject) ? null : new MailboxSubjectFilter(subject)
        };
    }

    @Override public boolean matches(MailboxMessage message) {
        for (MailboxFilter f : getSubFilters()) {
            if (f != null && !f.matches(message)) return false;
        }
        return true;
    }

}
