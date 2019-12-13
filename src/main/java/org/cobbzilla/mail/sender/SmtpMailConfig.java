package org.cobbzilla.mail.sender;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang.ArrayUtils;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

/**
 * (c) Copyright 2013 Jonathan Cobb.
 * This code is available under the Apache License, version 2: http://www.apache.org/licenses/LICENSE-2.0.html
 */
@NoArgsConstructor @Accessors(chain=true)
public class SmtpMailConfig {

    public static final int DEFAULT_SMTP_PORT = 25;

    @Getter @Setter private String host;
    @Getter @Setter private int port = DEFAULT_SMTP_PORT;
    @Getter @Setter private String user;
    @Getter @Setter private String password;
    @Getter @Setter private boolean tlsEnabled = false;
    @Getter @Setter private boolean sslEnabled = false;
    @Getter @Setter private String[] domainWhitelist;

    public boolean getHasMailUser() { return getUser() != null; }

    public boolean getHasDomainWhitelist () { return !empty(domainWhitelist); }

    public boolean isWhitelisted(String email) { return isWhitelisted(email, domainWhitelist); }

    public static boolean isWhitelisted(String email, String[] domainWhitelist) {
        if (domainWhitelist == null || domainWhitelist.length == 0) return false;
        final int atPos = email.indexOf("@");
        if (atPos == -1 || atPos == email.length()-1) return false;
        return ArrayUtils.contains(domainWhitelist, email.substring(atPos+1));
    }

}
