package com.example.concerttracker;

import org.junit.Test;
import static org.junit.Assert.*;

public class DatesTest {
    @Test public void validatesAndFormatsDates() {
        assertEquals("2026-09-09", Dates.iso(2026,8,9));
        assertEquals("9/9/2026",Dates.display("2026-09-09"));
        assertTrue(Dates.valid("2024-02-29"));
        assertFalse(Dates.valid("2026-02-29"));
        assertFalse(Dates.valid("2026-13-01"));
        assertFalse(Dates.valid(""));
    }
    @Test public void acceptsCommonSearchFormats() {
        assertTrue(Dates.matches("2026-09-09","9/9/26"));
        assertTrue(Dates.matches("2026-09-09","09/09/2026"));
        assertTrue(Dates.matches("2026-09-09","2026"));
        assertTrue(Dates.matches("2026-09-09","2026-09-09"));
        assertFalse(Dates.matches("2026-09-09","2025"));
    }
}
