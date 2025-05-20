package net.osmand.util;

public class ArabicNormalizer {

    private static final String[] DIACRITIC_REGEX = {"[\\u064B-\\u065F]", "[\\u0610-\\u061A]", "[\\u06D6-\\u06ED]", "\\u0640", "\\u0670"};
    private static final String[] DIACRITIC_REPLACE = {
            "\u0624", "\u0648", // Replace Waw Hamza Above by Waw
            "\u0629", "\u0647", // Replace Ta Marbuta by Ha
            "\u064A", "\u0649", // Replace Ya by Alif Maksura
            "\u0626", "\u0649", // Replace Ya Hamza Above by Alif Maksura
            "\u0622", "\u0627", // Replace Alifs with Hamza Above
            "\u0623", "\u0627", // Replace Alifs with Hamza Below
            "\u0625", "\u0627"  // Replace with Madda Above by Alif
    };
    private static final String ARABIC_DIGITS = "٠١٢٣٤٥٦٧٨٩";
    private static final String DIGITS_REPLACEMENT = "0123456789";

    public static boolean isSpecialArabic(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        char first = text.charAt(0);
        if (Character.UnicodeBlock.of(first) == Character.UnicodeBlock.ARABIC) {
            for (char c : text.toCharArray()) {
                if (isDiacritic(c) || isArabicDigit(c) || isNeedReplace(c)) {
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
        String result = text;
        for (int i = 0; i < DIACRITIC_REGEX.length; i++) {
            result = result.replaceAll(DIACRITIC_REGEX[i], "");
        }
        for (int i = 0; i < DIACRITIC_REPLACE.length; i = i + 2) {
            result = result.replace(DIACRITIC_REPLACE[i], DIACRITIC_REPLACE[i + 1]);
        }
        return replaceDigits(result);
    }

    private static String replaceDigits(String text) {
        if (text == null || text.isEmpty()) {
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
        return (c >= '\u064B' && c <= '\u065F') || 
                (c >= '\u0610' && c <= '\u061A') ||
                (c >= '\u06D6' && c <= '\u06ED') ||
                c == '\u0640' || c == '\u0670';
    }

    private static boolean isNeedReplace(char c) {
        String charAsString = String.valueOf(c);
        for (int i = 0; i < DIACRITIC_REPLACE.length; i += 2) {
            if (DIACRITIC_REPLACE[i].equals(charAsString)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isArabicDigit(char c) {
        return c >= '\u0660' && c <= '\u0669';  // Arabic-Indic digits ٠-٩
    }
}
