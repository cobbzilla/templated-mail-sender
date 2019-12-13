package org.cobbzilla.mail.fetch;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.daemon.SimpleDaemon;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Slf4j
public class MailboxWatcher extends SimpleDaemon {

    private static final long DEFAULT_SLEEP_INTERVAL = TimeUnit.MINUTES.toMillis(5);

    private MailboxChecker checker;
    private MailboxMessageHandler handler;
    private Long sleepInterval;

    @Override protected long getSleepTime() { return sleepInterval != null ? sleepInterval : DEFAULT_SLEEP_INTERVAL; }

    @Override protected void process() {
        final Collection<MailboxMessage> mailboxMessages = checker.checkMail();
        for (MailboxMessage m : mailboxMessages) {
            handler.handle(m);
        }
    }

}
