package com.biathlon.app;

public class ResultLogic {
    public static final int PENALTY_LOOP_SECONDS = 25;

    public record Calc(int finalTime, int penaltyTime, int penaltyLoops) {}

    public static Calc compute(String penaltyType, int skiTime, int missesProne, int missesStanding, int loopsManual, int penaltyManual) {
        int misses = Math.max(0, missesProne) + Math.max(0, missesStanding);
        int ski = Math.max(0, skiTime);

        return switch (penaltyType) {
            case "TIME_1MIN" -> new Calc(ski + misses * 60, misses * 60, 0);
            case "TIME_45SEC" -> new Calc(ski + misses * 45, misses * 45, 0);
            case "PENALTY_LOOP" -> {
                int loops = loopsManual > 0 ? loopsManual : misses;
                yield new Calc(ski + loops * PENALTY_LOOP_SECONDS, 0, loops);
            }
            case "RELAY_RULES" -> {
                int loops = Math.max(0, loopsManual);
                int penalty = Math.max(0, penaltyManual);
                yield new Calc(ski + penalty + loops * PENALTY_LOOP_SECONDS, penalty, loops);
            }
            default -> new Calc(ski, 0, 0);
        };
    }

    public static String formatTime(int sec) {
        int s = Math.max(0, sec);
        int h = s / 3600;
        int rem = s % 3600;
        int m = rem / 60;
        int c = rem % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, c) : String.format("%d:%02d", m, c);
    }
}
