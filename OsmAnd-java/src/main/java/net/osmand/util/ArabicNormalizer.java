package net.osmand.util;

public class ArabicNormalizer {

    private static final String DIACRITIC_REGEX = "[\\u064B-\\u0652]";
    private static final String ARABIC_DIGITS = "٠١٢٣٤٥٦٧٨٩";
    private static final String DIGITS_REPLACEMENT = "0123456789";
    private static final String KASHIDA = "\u0640";

    public static boolean isSpecialArabic(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        char first = text.charAt(0);
        if (Character.UnicodeBlock.of(first) == Character.UnicodeBlock.ARABIC) {
            for (char c : text.toCharArray()) {
                if (isDiacritic(c) || isArabicDigit(c) || isKashida(c)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String normalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String result = text.replaceAll(DIACRITIC_REGEX, "");
        result = result.replace(KASHIDA, "");
        return replaceDigits(result);
    }

    private static String replaceDigits(String text) {
        if (text == null) {
            return null;  // Handle null input
        }
        char first = text.charAt(0);
        if (Character.UnicodeBlock.of(first) != Character.UnicodeBlock.ARABIC) {
            return text;
        }

        char[] textChars = text.toCharArray();
        for (int i = 0; i < ARABIC_DIGITS.length(); i++) {
            char c = ARABIC_DIGITS.charAt(i);
            char replacement = DIGITS_REPLACEMENT.charAt(i);
            int index = text.indexOf(c);
            while (index >= 0) {
                textChars[index] = replacement;
                index = text.indexOf(c, index + 1);
            }
        }
        return String.valueOf(textChars);
    }

    private static boolean isDiacritic(char c) {
        return c >= '\u064B' && c <= '\u0652';  // Diacritic range
    }

    private static boolean isArabicDigit(char c) {
        return c >= '\u0660' && c <= '\u0669';  // Arabic-Indic digits ٠-٩
    }

    private static boolean isKashida(char c) {
        return c == '\u0640';  // Kashida character
    }
}
