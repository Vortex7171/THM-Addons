/*
 * This file is part of THM Addons — https://github.com/Leonn170709/THM-Addons
 * Copyright (c) THM Addons contributors. Credit the devs, keep the link.
 * By using this code you agree to the license terms and to keep your repo public.
 */

package xyz.thm.addon.utils;

/**
 * Smooths the reported ping and turns it into confirmation timings, so a 250 ms player waits for the
 * server's answer instead of re-asking at a 10 ms player's pace. Pure logic, no game access.
 */
public final class PingPacing {
    private static final double ALPHA = 0.1;
    private static final int MAX_PING = 1000;

    private double smoothed = -1;
    private double jitter;

    /** Feed the raw tab-list latency once per tick; a drop is followed faster than a spike. */
    public void update(int pingMs) {
        double sample = Math.max(0, Math.min(MAX_PING, pingMs));
        if (smoothed < 0) smoothed = sample;
        else {
            jitter += ALPHA * (Math.abs(sample - smoothed) - jitter);
            smoothed += (sample > smoothed ? ALPHA : ALPHA * 3) * (sample - smoothed);
        }
    }

    public void reset() {
        smoothed = -1;
        jitter = 0;
    }

    public int pingMs() {
        return smoothed < 0 ? 0 : (int) Math.round(smoothed);
    }

    /** Round trip in client ticks, at least one. */
    public int rttTicks() {
        return Math.max(1, (int) Math.ceil(pingMs() / 50.0));
    }

    /** Never asks before the answer can have arrived: round trip plus a tick each for jitter and the server's tick. */
    public int resendTicks(int base) {
        return Math.max(base, rttTicks() + 2);
    }

    /** How long a placed or broken block is watched for a revert. */
    public int revertWindow(int base) {
        return base + 2 * rttTicks();
    }

    /** Stable ticks before the adaptive rate climbs one step: quick on a clean link, slower when answers arrive late. */
    public int stableTicksPerStep(int base) {
        int ping = pingMs();
        if (ping <= 60) return Math.max(40, base * 3 / 10);
        if (ping <= 120) return Math.max(40, base / 2);
        if (ping <= 180) return base;
        return base * 3 / 2;
    }

    /** One extra tick of break progress before STOP on a jittery link, so it can't reach the server a tick early and be refused. */
    public int breakMarginTicks() {
        return jitter >= 35 ? 1 : 0;
    }
}
