package com.example.geckobrowser;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExampleUnitTest {
    @Test
    public void sanitizeFilename_stripsUnsafeCharsAndLimitsLength() {
        assertEquals("report_2025_.pdf", BrowserHelper.sanitizeFilename("report:2025?.pdf"));
        assertEquals("download", BrowserHelper.sanitizeFilename("   "));
        assertEquals("download", BrowserHelper.sanitizeFilename(".."));
        assertFalse(BrowserHelper.sanitizeFilename("a/b\\c*d|e").contains("/"));
    }

    @Test
    public void sanitizeFilename_truncatesLongNames() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 120; i++) {
            sb.append('x');
        }
        assertEquals(80, BrowserHelper.sanitizeFilename(sb.toString()).length());
    }
}