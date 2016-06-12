/*
 * The MIT License
 *
 * Copyright 2015 Oleg Nenashev <o.v.nenashev@gmail.com>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.ownership.util.mail;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.apache.http.NameValuePair;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

/**
 * Provides the support of operations with e-mails.
 * @author Oleg Nenashev
 * @since 0.6
 */
public class MailFormatter {
    
    private static final String DEFAULT_ENCODING = "UTF-8";
    
    private final @Nonnull String encoding;
    private final @Nonnull String separator;

    public MailFormatter() {
        this (DEFAULT_ENCODING, MailOptions.DEFAULT.getEmailListSeparator());
    }
  
    public MailFormatter(@Nonnull String separator) {
        this (DEFAULT_ENCODING, separator);
    }
    
    public MailFormatter(@Nonnull String encoding, @Nonnull String separator) {
        this.encoding = encoding;
        this.separator = separator;
    }

    public @Nonnull String getEncoding() {
        return encoding;
    }

    public @Nonnull String getSeparator() {
        return separator;
    }
       
    public @Nonnull String createMailToString (
            @CheckForNull List<String> to, 
            @CheckForNull List<String> cc, 
            @CheckForNull List<String> bcc, 
            @CheckForNull String subject, 
            @CheckForNull String body) throws UnsupportedEncodingException {
        StringBuilder b = new StringBuilder("mailto:");
        String toString = joinMailAddresses(to);
        if (toString != null) {
            b.append(URLEncoder.encode(toString, encoding));
        }
        List<NameValuePair> params = new LinkedList<NameValuePair>();
        joinMailAddresses(cc, "cc", params);
        joinMailAddresses(bcc, "bcc", params);
        if (subject != null) {
            params.add(new BasicNameValuePair("subject", subject));
        }
        if (body != null) {
            params.add(new BasicNameValuePair("body", body));
        }
        if (!params.isEmpty()) {
            b.append("?");
            String encodedParams = URLEncodedUtils.format(params, encoding);
            encodedParams = encodedParams.replace("+","%20");
            b.append(encodedParams);
        }
        return b.toString();
    }
    
    private @CheckForNull String joinMailAddresses (@CheckForNull List<String> items) {
        if (items != null && items.size() > 0) {
            return StringUtils.join(items, separator);
        } 
        return null;
    }
    
    private void joinMailAddresses (@CheckForNull List<String> items, 
            @CheckForNull String paramName, @Nonnull List<NameValuePair> target) 
            throws UnsupportedEncodingException {
        final String res = joinMailAddresses(items);
        if (res != null) {
           target.add(new BasicNameValuePair(paramName, res));
        }
    }
    
}
