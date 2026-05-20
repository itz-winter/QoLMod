package dev.qolmod.util;

/**
 * Utility for translating ampersand (&) color/formatting codes to section-sign (§) codes,
 * matching the behavior of Bukkit's ChatColor.translateAlternateColorCodes.
 *
 * Supported codes:
 *   &0-9     — color codes (black, dark red, … white)
 *   &a-f     — color codes (green … white)
 *   &k       — obfuscated / magic
 *   &l       — bold
 *   &m       — strikethrough
 *   &n       — underline
 *   &o       — italic
 *   &r       — reset
 *   &#RRGGBB — hex color (rendered as §x§R§R§G§G§B§B in legacy format)
 *
 * Usage example:
 *   Text.literal(TextUtils.translateAmpersand("&aHello &6World"))
 *   // renders "Hello" in green and "World" in gold
 */
public final class TextUtils {

    private static final String VALID_CODES = "0123456789abcdefklmnorABCDEFKLMNOR";

    private TextUtils() {}

    /**
     * Replaces {@code &x} sequences with the corresponding {@code §x} section-sign codes.
     * Supports standard color/format codes and &#RRGGBB hex colors.
     *
     * @param text input string with & codes
     * @return string with § codes suitable for Text.literal()
     */
    public static String translateAmpersand(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                // Hex color: &#RRGGBB
                if (next == '#' && i + 7 < text.length()) {
                    String hex = text.substring(i + 2, i + 8);
                    if (isHex(hex)) {
                        // Convert to legacy §x§R§R§G§G§B§B format
                        sb.append('§').append('x');
                        for (char h : hex.toCharArray()) {
                            sb.append('§').append(Character.toLowerCase(h));
                        }
                        i += 8;
                        continue;
                    }
                }
                // Standard code
                if (VALID_CODES.indexOf(next) >= 0) {
                    sb.append('§');
                    sb.append(Character.toLowerCase(next));
                    i += 2;
                    continue;
                }
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    private static boolean isHex(String s) {
        for (char c : s.toCharArray()) {
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                return false;
            }
        }
        return true;
    }
}
