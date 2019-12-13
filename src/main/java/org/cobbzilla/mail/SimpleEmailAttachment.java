package org.cobbzilla.mail;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.http.HttpContentTypes;

import java.io.File;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@NoArgsConstructor @Accessors(chain=true)
public class SimpleEmailAttachment {

    public SimpleEmailAttachment (File f) { setFile(f); }
    public SimpleEmailAttachment (File f, String description) { this(f); setDescription(description); }

    @Setter private String name;
    public String getName() { return !empty(name) ? name : hasFile() ? file.getName() : "attachment"; }

    @Getter @Setter private String description = "no description";

    @Getter @Setter private File file;
    public boolean hasFile() { return file != null; }

    @Getter @Setter private String base64bytes;
    @Setter private String contentType;

    public String getContentType() {
        return !empty(contentType) ? contentType : hasFile() ? HttpContentTypes.contentType(file.getName()) : "application/octet-stream";
    }

}
