package org.cobbzilla.mail.service;

import com.github.jknack.handlebars.Handlebars;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.cobbzilla.mail.*;
import org.cobbzilla.mail.sender.SmtpMailConfig;
import org.cobbzilla.mail.sender.SmtpMailSender;
import org.cobbzilla.util.io.DeleteOnExit;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import static org.cobbzilla.util.daemon.ZillaRuntime.CLASSPATH_PREFIX;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;

@Service @Slf4j
public class TemplatedMailService implements MailErrorHandler {

    public static final String T_WELCOME = "welcome";
    public static final String T_RESET_PASSWORD = "reset_password";

    public static final String PARAM_ACCOUNT = "account";
    public static final String PARAM_ADMIN = "admin";
    public static final String PARAM_HOSTNAME = "hostname";
    public static final String PARAM_PASSWORD = "password";

    @Autowired protected TemplatedMailSenderConfiguration configuration;

    @Getter(lazy=true) private final TemplatedMailSender mailSender = initMailSender();

    protected TemplatedMailSender initMailSender() {
        final SmtpMailConfig smtpMailConfig = configuration.getSmtp();
        final Handlebars handlebars = configuration.getHandlebars();
        final String emailTemplateRoot = configuration.getEmailTemplateRoot();
        final File fileRoot;
        if (emailTemplateRoot.startsWith(CLASSPATH_PREFIX)) {
            fileRoot = new TempDir();
            DeleteOnExit.add(fileRoot);
            try {
                final Enumeration<URL> resources = getClass().getClassLoader().getResources(emailTemplateRoot.substring(CLASSPATH_PREFIX.length()));
                while (resources.hasMoreElements()) {
                    final URL url = resources.nextElement();
                    final File dest = new File(fileRoot, url.getPath());
                    FileUtil.mkdirOrDie(dest.getParentFile());
                    FileUtil.toFile(dest, (InputStream) url.getContent());
                }
            } catch (Exception e) {
                return die("initMailSender: error reading templates from classpath: "+e, e);
            }
        } else {
            fileRoot = new File(emailTemplateRoot);
        }

        return new TemplatedMailSender(new SmtpMailSender(smtpMailConfig, handlebars), fileRoot);
    }

    public void deliver (TemplatedMail mail) {
        if (checkDuplicate(mail)) return;
        getMailSender().deliverMessage(mail, null, this);
    }
    public void deliver (TemplatedMail mail, MailSuccessHandler successHandler) {
        if (checkDuplicate(mail)) return;
        getMailSender().deliverMessage(mail, successHandler, this);
    }

    private final CircularFifoQueue cache = new CircularFifoQueue(100);
    private boolean checkDuplicate(TemplatedMail mail) {
        synchronized (cache) {
            if (cache.contains(mail)) {
                log.warn("checkDuplicate: not sending duplicate mail: "+mail);
                return true;
            }
            cache.add(mail);
        }
        return false;
    }

    @Getter(lazy=true) private final RetryErrorHandler retryHandler = initRetryHandler();
    private RetryErrorHandler initRetryHandler() { return new RetryErrorHandler(true); }

    @Override public void handleError(TemplatedMailSender mailSender, TemplatedMail mail, MailSuccessHandler successHandler, Exception e) {
        getRetryHandler().handleError(mailSender, mail, successHandler, e);
    }
}
