/*
 * The MIT License
 *
 * Copyright 2017 Ksenia Nenasheva <ks.nenasheva@gmail.com>.
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
package org.jenkinsci.plugins.ownership.integrations.securityinspector;

import hudson.security.Permission;
import java.util.Set;
import javax.annotation.Nonnull;
import static org.hamcrest.MatcherAssert.assertThat;
import org.hamcrest.Matchers;
import org.jenkinsci.plugins.securityinspector.model.PermissionReport;
import org.junit.Assert;

/**
 * Basic asserts for {@link PermissionReport}
 *
 * @author Ksenia Nenasheva <ks.nenasheva@gmail.com>
 */
public class PermissionReportAssert {

    public static <TRow> void assertHasPermissions(
            @Nonnull PermissionReport<TRow, Boolean> report, @Nonnull TRow row,
            @Nonnull Permission ... permissions) {
        for (Permission permission : permissions) {
            assertPermissionValue(report, row, permission, true);
        }
    }
    
    public static <TRow> void assertHasNotPermissions(
            @Nonnull PermissionReport<TRow, Boolean> report, @Nonnull TRow row,
            @Nonnull Permission ... permissions) {
        for (Permission permission : permissions) {
            assertPermissionValue(report, row, permission, false);
        }
    }
    
    public static <TRow, TEntryReport> void assertPermissionValue(
            @Nonnull PermissionReport<TRow, TEntryReport> report, @Nonnull TRow row,
            @Nonnull Permission permission, @Nonnull TEntryReport expectedValue) {
        
        final TEntryReport entryReport = report.getEntry(row, permission);
        assertThat("Wrong value for " + report.getRowTitle(row) + "=" + row + " and permission=" + permission.getId(), 
                entryReport, Matchers.equalTo(expectedValue));
        
    }
    
    public static <TRow> void assertHasRow(
            @Nonnull PermissionReport<TRow, Boolean> report, @Nonnull TRow row) {
        
        final Set<TRow> rows = report.getRows();
        for (TRow r : rows) {
           if (r.equals(row)) {
               return;
           }
        }
        Assert.fail("Row " + row + "hasn't been added in this report");
    }
    
    public static <TRow> void assertHasNotRow(
            @Nonnull PermissionReport<TRow, Boolean> report, @Nonnull TRow row) {
        
        final Set<TRow> rows = report.getRows();
        for (TRow r : rows) {
           if (r.equals(row)) {
               Assert.fail("Row " + row + "has been added in this report");
           }
        }
    }
}
