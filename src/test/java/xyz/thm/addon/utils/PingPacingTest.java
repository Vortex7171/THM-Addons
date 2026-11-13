/*
 * This file is part of THM Addons — https://github.com/Leonn170709/THM-Addons
 * Copyright (c) THM Addons contributors. Credit the devs, keep the link.
 * By using this code you agree to the license terms and to keep your repo public.
 */

package xyz.thm.addon.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PingPacingTest {
    private static PingPacing at(int ping) {
        PingPacing p = new PingPacing();
        p.update(ping);
        return p;
    }

    @Test
    void lowPingKeepsBaseTimings() {
        PingPacing p = at(10);
        assertEquals(3, p.resendTicks(3));
        assertEquals(42, p.revertWindow(40));
    }

    @Test
    void highPingWaitsForTheAnswer() {
        PingPacing p = at(250);
        assertEquals(5, p.rttTicks());
        assertEquals(7, p.resendTicks(3));
        assertEquals(50, p.revertWindow(40));
        assertEquals(20, p.resendTicks(20), "a longer configured wait is kept");
    }

    @Test
    void climbIsFasterOnCleanLinks() {
        assertTrue(at(20).stableTicksPerStep(200) < at(100).stableTicksPerStep(200));
        assertTrue(at(100).stableTicksPerStep(200) < at(150).stableTicksPerStep(200));
        assertTrue(at(150).stableTicksPerStep(200) < at(250).stableTicksPerStep(200));
    }

    @Test
    void spikesAreSmoothedAndRecoveryIsQuicker() {
        PingPacing p = at(50);
        p.update(500);
        assertTrue(p.pingMs() < 120, "one spike barely moves it");
        PingPacing q = at(50);
        for (int i = 0; i < 30; i++) q.update(300);
        for (int i = 0; i < 10; i++) q.update(50);
        assertTrue(q.pingMs() < 150, "drops are followed faster than spikes");
    }

    @Test
    void noSampleYetUsesLowPingTimings() {
        PingPacing p = new PingPacing();
        assertEquals(0, p.pingMs());
        assertEquals(3, p.resendTicks(3));
    }
}
