package org.cobbzilla.mail.fetch;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.mail.SimpleEmailAttachment;
import org.cobbzilla.util.string.Base64;

import javax.mail.MessagingException;
import javax.mail.Part;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MailboxAttachment extends SimpleEmailAttachment {

    @Getter @Setter private String fileName;

    public MailboxAttachment(Part part) throws IOException, MessagingException {

        setContentType(part.getContentType());
        setFileName(part.getFileName());
        setDescription(part.getDescription());

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        part.writeTo(out);
        setBase64bytes(Base64.encodeBytes(out.toByteArray()));
    }

}
