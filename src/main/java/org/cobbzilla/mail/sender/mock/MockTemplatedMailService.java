package org.cobbzilla.mail.sender.mock;

import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.mail.TemplatedMailSender;
import org.cobbzilla.mail.service.TemplatedMailService;

@Slf4j
public class MockTemplatedMailService extends TemplatedMailService {

    @Override protected TemplatedMailSender initMailSender() { return new MockTemplatedMailSender(); }

}
