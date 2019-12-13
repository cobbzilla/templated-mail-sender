package org.cobbzilla.mail;

public interface MailErrorHandler {

    public void handleError(TemplatedMailSender mailSender,
                            TemplatedMail mail,
                            MailSuccessHandler successHandler,
                            Exception e);
}
