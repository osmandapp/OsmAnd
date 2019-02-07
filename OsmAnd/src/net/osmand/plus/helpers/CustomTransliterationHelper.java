package net.osmand.plus.helpers;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;
import java.util.List;

public class CustomTransliterationHelper {

	public static String japanese2Romaji(String input) {

		boolean capitalizeWords = true;
		boolean isDebug = true;

		Tokenizer tokenizer = new Tokenizer() ;
		List<Token> tokens = tokenizer.tokenize(input);

		StringBuffer buffer = new StringBuffer();
		KanaToRomaji kanaToRomaji = new KanaToRomaji();
		String lastTokenToMerge = "";
		for (Token token : tokens) {
			String type = token.getAllFeaturesArray()[1];
			if (isDebug) {
				System.out.println("Type: " + type);
			}

			if( token.getAllFeaturesArray()[0].equals("記号"))  {
				buffer.append(token.getSurface());
				continue;
			}
			switch(token.getAllFeaturesArray()[1]) {
				case "数":
				case "アルファベット":
				case "サ変接続":
					buffer.append(token.getSurface());
					//break;
					continue;
				default:
					String lastFeature = token.getAllFeaturesArray()[8];
					if (lastFeature.equals("*")) {
						buffer.append(token.getSurface());
					}
					else {
						String romaji = kanaToRomaji.convert(token.getAllFeaturesArray()[8]);
						if ( lastFeature.endsWith("ッ") ) {
							lastTokenToMerge = lastFeature;
							continue;
						} else {
							lastTokenToMerge = "";
						}

						if ( capitalizeWords == true ) {
							buffer.append(romaji.substring(0, 1).toUpperCase());
							buffer.append(romaji.substring(1));
						} else {
							// Convert foreign katakana words to uppercase
							if(token.getSurface().equals(token.getPronunciation())) // detect katakana
								romaji = romaji.toUpperCase();
							buffer.append(romaji);
						}
					}
			}
			buffer.append(" ");
		}
		return buffer.toString();
	}


	public static boolean isCharCJK(final char c) {
		if ((Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HIRAGANA)
			||(Character.UnicodeBlock.of(c) == Character.UnicodeBlock.KATAKANA)
			||(Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS)
			|| (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A)
			|| (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B)
			|| (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS)
			|| (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS)

			|| (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.ENCLOSED_CJK_LETTERS_AND_MONTHS)) {
			return true;
		}
		return false;
	}

	public static void main(String[] args) {
		System.out.println(japanese2Romaji("川名本町"));
		System.out.println(japanese2Romaji("明岸寺"));
		System.out.println(japanese2Romaji("つきしま"));

	}
}