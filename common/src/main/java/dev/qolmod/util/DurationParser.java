package dev.qolmod.util;

/**
 * Utility for parsing human-readable durations like "1d2h30m" into seconds.
 */
public class DurationParser {

    private DurationParser() {}

    /**
     * Parse a duration string (e.g. "1d2h30m10s") into total seconds.
     * Supports d(ays), h(ours), m(inutes), s(econds).
     * Returns -1 if the format is invalid.
     */
    public static long parseToSeconds(String input) {
        if (input == null || input.isEmpty()) return -1;

        long total = 0;
        StringBuilder num = new StringBuilder();

        for (char c : input.toLowerCase().toCharArray()) {
            if (Character.isDigit(c)) {
                num.append(c);
            } else {
                if (num.length() == 0) return -1;
                long value = Long.parseLong(num.toString());
                num.setLength(0);

                switch (c) {
                    case 'd': total += value * 86400; break;
                    case 'h': total += value * 3600; break;
                    case 'm': total += value * 60; break;
                    case 's': total += value; break;
                    default: return -1;
                }
            }
        }

        // If there's a trailing number with no unit, treat as seconds
        if (num.length() > 0) {
            total += Long.parseLong(num.toString());
        }

        return total > 0 ? total : -1;
    }

    /**
     * Format seconds into a human-readable string (e.g. "1d 2h 30m").
     */
    public static String format(long totalSeconds) {
        if (totalSeconds <= 0) return "permanent";

        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");

        return sb.toString().trim();
    }
}
