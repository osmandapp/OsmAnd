package net.osmand.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class ArabicNormalizer {

    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{Mn}");

    public static String normalize(String text) {
        if (text == null) {
            return null;  // Handle null input
        }

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        normalized = DIACRITICS_PATTERN.matcher(normalized).replaceAll(""); // Remove diacritics efficiently

        // Hamza variations
        normalized = normalized.replace("إ", "ا"); // Initial hamza on alif
        normalized = normalized.replace("أ", "ا"); // Initial hamza on waw
        normalized = normalized.replace("ئ", "ي"); // Hamza on ya' (This should be 'ي' not 'ا' for better accuracy)
        normalized = normalized.replace("ؤ", "و"); // Hamza on waw

        // Other normalizations
        normalized = normalized.replace("آ", "ا");  // Alif madda
        normalized = normalized.replace("ى", "ي"); // Final form of ya'
        normalized = normalized.replace("ة", "ه"); // Teh marbuta to ha'

        // Kashida
        normalized = normalized.trim().replaceAll("\u0640", "");// Kashida

        return normalized;
    }

}
