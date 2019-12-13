package org.cobbzilla.mail;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.opensagres.xdocreport.core.io.IOUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.apache.commons.codec.binary.Base64InputStream;
import org.cobbzilla.util.http.HttpUtil;

import javax.activation.DataSource;
import java.beans.Transient;
import java.io.*;

import static org.cobbzilla.util.daemon.ZillaRuntime.notSupported;

@Accessors(chain=true) @ToString(of={"name", "includeType", "contentType"}) @EqualsAndHashCode(of="name")
public class SimpleEmailImage implements DataSource {

    @Getter @Setter private String name;
    @Getter @Setter private EmailIncludeType includeType;
    @Getter @Setter private String contentType;
    @Getter @Setter private File file;
    @Getter @Setter private String url;
    @Getter @Setter private String base64data;

    @Override @JsonIgnore @Transient public InputStream getInputStream() throws IOException {
        return file != null
                ? new FileInputStream(file)
                : url != null
                    ? HttpUtil.get(url)
                    : new Base64InputStream(IOUtils.toInputStream(base64data), false);
    }

    @Override @JsonIgnore @Transient public OutputStream getOutputStream() throws IOException { return notSupported("getOutputStream"); }

}
