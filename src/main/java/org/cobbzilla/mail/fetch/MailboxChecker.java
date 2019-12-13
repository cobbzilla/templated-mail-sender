package org.cobbzilla.mail.fetch;

import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.collection.SingletonList;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import java.util.*;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Slf4j
public class MailboxChecker {

    public MailboxChecker (MailboxConfig config, MailboxFilter filter) {
        setMailbox(config);
        setFilters(new SingletonList<>(filter));
    }

    public MailboxChecker (MailboxConfig config, MailboxFilter[] filters) {
        setMailbox(config);
        setFilters(Arrays.asList(filters));
    }

    @Getter @Setter private MailboxConfig mailbox;
    @Getter @Setter private Collection<MailboxFilter> filters;

    private Set<String> matchedMessageIds = new HashSet<>();

    public Collection<MailboxMessage> checkMail() {
        final List<MailboxMessage> matches = new ArrayList<>();
        try {
            // create properties field
            final Properties properties = System.getProperties();
            final String mailProtocol = mailbox.getJavaProtocol();
            properties.put("mail.store.protocol", mailProtocol);
            final Session emailSession = Session.getDefaultInstance(properties, null);

            // create the POP3 store object and connect with the pop server
            @Cleanup final Store store = emailSession.getStore(mailProtocol);
            store.connect(mailbox.getHost(), mailbox.getUser(), mailbox.getPassword());

            // create the folder object and open it
            Folder emailFolder = null;
            try {
                emailFolder = store.getFolder(mailbox.getFolder());
                emailFolder.open(Folder.READ_ONLY);

                // retrieve the messages from the folder in an array
                final Message[] messages = emailFolder.getMessages();
                for (int i = 0; i < messages.length; i++) {
                    final MailboxMessage mail = new MailboxMessage(messages[i], getMailbox().getUser());
                    if (!matchedMessageIds.contains(mail.getMessageId())) {
                        if (filterMatch(mail, filters)) {
                            matches.add(mail);
                            matchedMessageIds.add(mail.getMessageId());
                        }
                    }
                }

            } finally {
                // close the folder objects
                if (emailFolder != null) {
                    try {
                        emailFolder.close(false);
                    } catch (Exception e) {
                        log.warn("checkMail: error closing folder "+mailbox.getFolder()+": "+e);
                    }
                }
            }

        } catch (Exception e) {
            return die("checkMail: unexpected error: "+e, e);
        }
        return matches;
    }

    private boolean filterMatch(MailboxMessage message, Collection<MailboxFilter> filters) {
        if (empty(filters)) return true;
        for (MailboxFilter filter : filters) {
            if (!filter.matches(message)) return false;
        }
        return true;
    }

}
