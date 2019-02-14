package net.osmand.util;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomTransliterationHelper {

	public static String japanese2Romaji(String input) {

		boolean capitalizeWords = true;

		Tokenizer tokenizer = new Tokenizer();
		List<Token> tokens = tokenizer.tokenize(input);

		StringBuffer buffer = new StringBuffer();
		KanaToRomaji kanaToRomaji = new KanaToRomaji();
		String lastTokenToMerge = "";
		for (Token token : tokens) {
			String type = token.getAllFeaturesArray()[1];

			if (token.getAllFeaturesArray()[0].equals("記号")) {
				buffer.append(token.getSurface());
				continue;
			}
			switch (token.getAllFeaturesArray()[1]) {
				case "数":
				case "アルファベット":
				case "サ変接続":
					buffer.append(token.getSurface());
					continue;
				default:
					String lastFeature = token.getAllFeaturesArray()[8];
					if (lastFeature.equals("*")) {
						buffer.append(token.getSurface());
					} else {
						String romaji = kanaToRomaji.convert(token.getAllFeaturesArray()[8]);
						if (lastFeature.endsWith("ッ")) {
							lastTokenToMerge = lastFeature;
							continue;
						} else {
							lastTokenToMerge = "";
						}

						if (capitalizeWords) {
							buffer.append(romaji.substring(0, 1).toUpperCase());
							buffer.append(romaji.substring(1));
						} else {
							// Convert foreign katakana words to uppercase
							if (token.getSurface()
								.equals(token.getPronunciation())) // detect katakana
							{
								romaji = romaji.toUpperCase();
							}
							buffer.append(romaji);
						}
					}
			}
			buffer.append(" ");
		}
		return buffer.toString();
	}


	public static boolean isCharFromCJK(final char c) {
		return (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HIRAGANA)
			|| (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.KATAKANA)
			|| (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS)
			|| (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A)
			|| (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B)
			|| (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS)
			|| (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS)
			|| (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.ENCLOSED_CJK_LETTERS_AND_MONTHS);
	}

	public static void main(String[] args) {
		System.out.println(japanese2Romaji("川名本町"));
		System.out.println(japanese2Romaji("明岸寺"));
		System.out.println(japanese2Romaji("つきしま"));

	}

	private static class KanaToRomaji {

		Map<String, String> m = new HashMap<String, String>();

		// Constructor
		public KanaToRomaji() {
			m.put("ア", "a");
			m.put("イ", "i");
			m.put("ウ", "u");
			m.put("エ", "e");
			m.put("オ", "o");
			m.put("カ", "ka");
			m.put("キ", "ki");
			m.put("ク", "ku");
			m.put("ケ", "ke");
			m.put("コ", "ko");
			m.put("サ", "sa");
			m.put("シ", "shi");
			m.put("ス", "su");
			m.put("セ", "se");
			m.put("ソ", "so");
			m.put("タ", "ta");
			m.put("チ", "chi");
			m.put("ツ", "tsu");
			m.put("テ", "te");
			m.put("ト", "to");
			m.put("ナ", "na");
			m.put("ニ", "ni");
			m.put("ヌ", "nu");
			m.put("ネ", "ne");
			m.put("ノ", "no");
			m.put("ハ", "ha");
			m.put("ヒ", "hi");
			m.put("フ", "fu");
			m.put("ヘ", "he");
			m.put("ホ", "ho");
			m.put("マ", "ma");
			m.put("ミ", "mi");
			m.put("ム", "mu");
			m.put("メ", "me");
			m.put("モ", "mo");
			m.put("ヤ", "ya");
			m.put("ユ", "yu");
			m.put("ヨ", "yo");
			m.put("ラ", "ra");
			m.put("リ", "ri");
			m.put("ル", "ru");
			m.put("レ", "re");
			m.put("ロ", "ro");
			m.put("ワ", "wa");
			m.put("ヲ", "wo");
			m.put("ン", "n");
			m.put("ガ", "ga");
			m.put("ギ", "gi");
			m.put("グ", "gu");
			m.put("ゲ", "ge");
			m.put("ゴ", "go");
			m.put("ザ", "za");
			m.put("ジ", "ji");
			m.put("ズ", "zu");
			m.put("ゼ", "ze");
			m.put("ゾ", "zo");
			m.put("ダ", "da");
			m.put("ヂ", "ji");
			m.put("ヅ", "zu");
			m.put("デ", "de");
			m.put("ド", "do");
			m.put("バ", "ba");
			m.put("ビ", "bi");
			m.put("ブ", "bu");
			m.put("ベ", "be");
			m.put("ボ", "bo");
			m.put("パ", "pa");
			m.put("ピ", "pi");
			m.put("プ", "pu");
			m.put("ペ", "pe");
			m.put("ポ", "po");
			m.put("キャ", "kya");
			m.put("キュ", "kyu");
			m.put("キョ", "kyo");
			m.put("シャ", "sha");
			m.put("シュ", "shu");
			m.put("ショ", "sho");
			m.put("チャ", "cha");
			m.put("チュ", "chu");
			m.put("チョ", "cho");
			m.put("ニャ", "nya");
			m.put("ニュ", "nyu");
			m.put("ニョ", "nyo");
			m.put("ヒャ", "hya");
			m.put("ヒュ", "hyu");
			m.put("ヒョ", "hyo");
			m.put("リャ", "rya");
			m.put("リュ", "ryu");
			m.put("リョ", "ryo");
			m.put("ギャ", "gya");
			m.put("ギュ", "gyu");
			m.put("ギョ", "gyo");
			m.put("ジャ", "ja");
			m.put("ジュ", "ju");
			m.put("ジョ", "jo");
			m.put("ティ", "ti");
			m.put("ディ", "di");
			m.put("ツィ", "tsi");
			m.put("ヂャ", "dya");
			m.put("ヂュ", "dyu");
			m.put("ヂョ", "dyo");
			m.put("ビャ", "bya");
			m.put("ビュ", "byu");
			m.put("ビョ", "byo");
			m.put("ピャ", "pya");
			m.put("ピュ", "pyu");
			m.put("ピョ", "pyo");
			m.put("ー", "-");
		}

		public String convert(String s) {
			StringBuilder t = new StringBuilder();
			for (int i = 0; i < s.length(); i++) {
				if (i <= s.length() - 2) {
					if (m.containsKey(s.substring(i, i + 2))) {
						t.append(m.get(s.substring(i, i + 2)));
						i++;
					} else if (m.containsKey(s.substring(i, i + 1))) {
						t.append(m.get(s.substring(i, i + 1)));
					} else if (s.charAt(i) == 'ッ') {
						t.append(m.get(s.substring(i + 1, i + 2)).charAt(0));
					} else {
						t.append(s.charAt(i));
					}
				} else {
					if (m.containsKey(s.substring(i, i + 1))) {
						t.append(m.get(s.substring(i, i + 1)));
					} else {
						t.append(s.charAt(i));
					}
				}
			}
			return t.toString();
		}
	}
}