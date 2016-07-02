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

package com.synopsys.arc.jenkins.plugins.ownership.util.mail;

import org.jenkinsci.plugins.ownership.util.mail.MailFormatter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests for {@link MailFormatter}.
 * @author Oleg Nenashev
 */
public class MailFormatterTest {
    
    MailFormatter formatter = new MailFormatter();
    
    /**
     * Just checks if nothing breaks horribly.
     * @throws UnsupportedEncodingException Issues with the default encoding
     * @throws MalformedURLException {@link MailFormatter} generated wrong link
     */
    public @Test void spotCheck() throws UnsupportedEncodingException, MalformedURLException {
        String res = formatter.createMailToString(
                Arrays.asList("test@foo.bar"), 
                Arrays.asList("test1@foo.bar", "test2@foo.bar"), null, 
                "[Jenkins] - Test subject", 
                "Test body \n with multiple \r\n lines");
        assertTrue(res.startsWith("mailto:"));
        //TODO: check contents
        final URL url = new URL(res);
    }
}
