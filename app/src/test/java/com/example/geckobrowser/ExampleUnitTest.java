package com.example.geckobrowser;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void guessFilename_prefersSanitizedSuggestedFilename() {
        assertEquals("invoice_2025.pdf", BrowserHelper.guessFilename("https://example.com/file", "invoice 2025.pdf", "application/pdf"));
        assertEquals("report.csv", BrowserHelper.guessFilename(null, "report.csv", null));
    }

    @Test
    public void browserTab_hasSafeDefaults() {
        BrowserTab tab = new BrowserTab(7L, "https://example.com", "Example", true, false);
        assertEquals(7L, tab.id);
        assertTrue(tab.isPrivate);
        assertTrue(tab.isSecure);
        assertFalse(tab.isResettingToHome);
        assertFalse(tab.desktopMode);
    }
}