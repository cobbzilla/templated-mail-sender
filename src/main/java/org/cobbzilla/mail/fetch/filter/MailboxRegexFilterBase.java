package org.cobbzilla.mail.fetch.filter;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.mail.fetch.MailboxFilter;

import java.util.regex.Pattern;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@AllArgsConstructor @Accessors(chain=true)
public abstract class MailboxRegexFilterBase implements MailboxFilter {

    @Getter @Setter private String regex;
    public boolean hasRegex () { return !empty(regex); }

    @Getter(value=AccessLevel.PROTECTED, lazy=true) private final Pattern pattern = initPattern();
    private Pattern initPattern() { return empty(regex) ? null : Pattern.compile(regex); }

}
