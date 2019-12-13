package org.cobbzilla.mail;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.daemon.SimpleDaemon;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.daemon.ZillaRuntime.now;

@Slf4j
public class RetryErrorHandler extends SimpleDaemon implements MailErrorHandler {

    private final List<BackloggedMessage> backlog = new ArrayList<>();

    public RetryErrorHandler(boolean now) { if (now) start(); }

    @Override public void handleError(TemplatedMailSender mailSender,
                                      TemplatedMail mail,
                                      MailSuccessHandler successHandler,
                                      Exception e) {
        BackloggedMessage message = new BackloggedMessage(mailSender, successHandler, mail);
        addToBacklog(message);
    }

    protected void addToBacklog(BackloggedMessage message) { synchronized (backlog) { backlog.add(message); } }

    @Getter @Setter private long maxBacklogSize = 1000;
    @Getter @Setter private long minAttemptInterval = TimeUnit.MINUTES.toMillis(2);
    @Getter @Setter private long maxAttemptInterval = TimeUnit.DAYS.toMillis(4);

    @Getter @Setter public long startupDelay = TimeUnit.SECONDS.toMillis(30);
    @Getter @Setter public long sleepTime = TimeUnit.MINUTES.toMillis(1);

    @Override protected void process() {
        final List<BackloggedMessage> toProcess;
        synchronized (backlog) {
            toProcess = new ArrayList<>(backlog);
            backlog.clear();
        }
        for (BackloggedMessage message : toProcess) {
            if (message.deliver()) {
                if (message.successHandler != null) {
                    try {
                        message.successHandler.handleSuccess(message.mail);
                    } catch (Exception e) {
                        // todo: airbrake this
                        log.error("Error calling successHandler, giving up on message (" + message.successHandler + "): " + e, e);
                    }
                }
            } else {
                addToBacklog(message);
            }
        }
    }

    private class BackloggedMessage {
        private TemplatedMailSender mailSender;
        private MailSuccessHandler successHandler;
        private TemplatedMail mail;
        private long nextAttempt;
        private int attempts;
        public BackloggedMessage(TemplatedMailSender mailSender,
                                 MailSuccessHandler successHandler,
                                 TemplatedMail mail) {
            this.mailSender = mailSender;
            this.successHandler = successHandler;
            this.mail = mail;
            this.attempts = 1;
            this.nextAttempt = now() + minAttemptInterval;
        }

        public boolean deliver() {
            // when is the next attempt?
            if (now() < nextAttempt) return false;
            try {
                mailSender.deliverMessage(mail);
                return true;

            } catch (Exception e) {
                attempts++;
                nextAttempt = now() + minAttemptInterval * attempts;
                log.error("deliver: error on attempt #"+attempts+" ("+e+"), next attempt at "+DFORMAT.print(nextAttempt)
                        + " cc:"+ mail.getCc()+", to:"+mail.getToEmail()+", bcc:"+mail.getBcc()+", from:"+mail.getFromEmail()+", exception="+e, e);
                return false;
            }
        }
    }
}
