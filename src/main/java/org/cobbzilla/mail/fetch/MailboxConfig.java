package org.cobbzilla.mail.fetch;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.beans.Transient;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

@NoArgsConstructor @Accessors(chain=true)
public class MailboxConfig {

    public static final int DEFAULT_POP3_PORT = 110;
    public static final int DEFAULT_POP3S_PORT = 995;
    public static final int DEFAULT_IMAP_PORT = 143;
    public static final int DEFAULT_IMAPS_PORT = 993;

    @Getter @Setter private MailboxProtocol protocol = MailboxProtocol.imap;

    @Getter @Setter private String host;

    @Getter @Setter private String user;
    public boolean getHasMailUser() { return getUser() != null; }

    @Getter @Setter private String password;
    @Getter @Setter private String folder = "INBOX";
    @Getter @Setter private boolean secure = true;

    @Setter private Integer port;
    public int getPort () {
        if (port != null) return port;
        if (protocol == null) return die("getPort: no protocol specified");
        switch (protocol) {
            case imap: return secure ? DEFAULT_IMAPS_PORT : DEFAULT_IMAP_PORT;
            case pop3: return secure ? DEFAULT_POP3S_PORT : DEFAULT_POP3_PORT;
            default: return die("getPort: invalid protocol: "+protocol);
        }
    }

    @JsonIgnore @Transient public String getJavaProtocol () {
        if (protocol == null) return die("getJavaProtocol: no protocol specified");
        return protocol.getJavaProtocol(secure);
    }

}
