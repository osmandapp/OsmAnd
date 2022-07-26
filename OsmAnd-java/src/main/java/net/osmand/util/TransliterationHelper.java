package net.osmand.util;

import net.osmand.PlatformUtil;
import net.sf.junidecode.Junidecode;

import org.apache.commons.logging.Log;

public class TransliterationHelper {

	public static final Log LOG = PlatformUtil.getLog(TransliterationHelper.class);

	private static boolean japanese;

	//private static Tokenizer tokenizer;
	//private static Map<String, String> katakanaMap = new HashMap<>();

	private TransliterationHelper() {
	}

	public static boolean isJapanese() {
		return japanese;
	}

	public static void setJapanese(boolean japanese) {
		TransliterationHelper.japanese = japanese;
	}

	public static String transliterate(String text) {
		if (japanese) {
			// do not transliterate japanese for now
			//return japanese2Romaji(text);
			return text;
		} else {
			return Junidecode.unidecode(text);
		}
	}

	/*
	private static String japanese2Romaji(String text) {

		if (tokenizer == null) {
			tokenizer = new Tokenizer();
		}

		boolean capitalizeWords = true;

		List<Token> tokens = tokenizer.tokenize(text);

		StringBuilder builder = new StringBuilder();
		if (katakanaMap.isEmpty()) {
			initKanaMap();
		}
		for (Token token : tokens) {
			String type = token.getAllFeaturesArray()[1];

			if (token.getAllFeaturesArray()[0].equals("記号")) {
				builder.append(token.getSurface());
				continue;
			}
			switch (token.getAllFeaturesArray()[1]) {
				case "数":
				case "アルファベット":
				case "サ変接続":
					builder.append(token.getSurface());
					continue;
				default:
					String lastFeature = token.getAllFeaturesArray()[8];
					if (lastFeature.equals("*")) {
						builder.append(token.getSurface());
					} else {
						String romaji = convertKanaToRomaji(token.getAllFeaturesArray()[8]);

						if (capitalizeWords) {
							builder.append(romaji.substring(0, 1).toUpperCase());
							builder.append(romaji.substring(1));
						} else {
							if (token.getSurface()
								.equals(token.getPronunciation())) {
								romaji = romaji.toUpperCase();
							}
							builder.append(romaji);
						}
					}
			}
			builder.append(" ");
		}
		return builder.toString();
	}

	private static String convertKanaToRomaji(String s) {
		StringBuilder t = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			if (i <= s.length() - 2) {
				if (katakanaMap.containsKey(s.substring(i, i + 2))) {
					t.append(katakanaMap.get(s.substring(i, i + 2)));
					i++;
				} else if (katakanaMap.containsKey(s.substring(i, i + 1))) {
					t.append(katakanaMap.get(s.substring(i, i + 1)));
				} else if (s.charAt(i) == 'ッ') {
					t.append(katakanaMap.get(s.substring(i + 1, i + 2)).charAt(0));
				} else {
					t.append(s.charAt(i));
				}
			} else {
				if (katakanaMap.containsKey(s.substring(i, i + 1))) {
					t.append(katakanaMap.get(s.substring(i, i + 1)));
				} else {
					t.append(s.charAt(i));
				}
			}
		}
		return t.toString();
	}

	private static void initKanaMap() {
		katakanaMap.put("ア", "a");
		katakanaMap.put("イ", "i");
		katakanaMap.put("ウ", "u");
		katakanaMap.put("エ", "e");
		katakanaMap.put("オ", "o");
		katakanaMap.put("カ", "ka");
		katakanaMap.put("キ", "ki");
		katakanaMap.put("ク", "ku");
		katakanaMap.put("ケ", "ke");
		katakanaMap.put("コ", "ko");
		katakanaMap.put("サ", "sa");
		katakanaMap.put("シ", "shi");
		katakanaMap.put("ス", "su");
		katakanaMap.put("セ", "se");
		katakanaMap.put("ソ", "so");
		katakanaMap.put("タ", "ta");
		katakanaMap.put("チ", "chi");
		katakanaMap.put("ツ", "tsu");
		katakanaMap.put("テ", "te");
		katakanaMap.put("ト", "to");
		katakanaMap.put("ナ", "na");
		katakanaMap.put("ニ", "ni");
		katakanaMap.put("ヌ", "nu");
		katakanaMap.put("ネ", "ne");
		katakanaMap.put("ノ", "no");
		katakanaMap.put("ハ", "ha");
		katakanaMap.put("ヒ", "hi");
		katakanaMap.put("フ", "fu");
		katakanaMap.put("ヘ", "he");
		katakanaMap.put("ホ", "ho");
		katakanaMap.put("マ", "ma");
		katakanaMap.put("ミ", "mi");
		katakanaMap.put("ム", "mu");
		katakanaMap.put("メ", "me");
		katakanaMap.put("モ", "mo");
		katakanaMap.put("ヤ", "ya");
		katakanaMap.put("ユ", "yu");
		katakanaMap.put("ヨ", "yo");
		katakanaMap.put("ラ", "ra");
		katakanaMap.put("リ", "ri");
		katakanaMap.put("ル", "ru");
		katakanaMap.put("レ", "re");
		katakanaMap.put("ロ", "ro");
		katakanaMap.put("ワ", "wa");
		katakanaMap.put("ヲ", "wo");
		katakanaMap.put("ン", "n");
		katakanaMap.put("ガ", "ga");
		katakanaMap.put("ギ", "gi");
		katakanaMap.put("グ", "gu");
		katakanaMap.put("ゲ", "ge");
		katakanaMap.put("ゴ", "go");
		katakanaMap.put("ザ", "za");
		katakanaMap.put("ジ", "ji");
		katakanaMap.put("ズ", "zu");
		katakanaMap.put("ゼ", "ze");
		katakanaMap.put("ゾ", "zo");
		katakanaMap.put("ダ", "da");
		katakanaMap.put("ヂ", "ji");
		katakanaMap.put("ヅ", "zu");
		katakanaMap.put("デ", "de");
		katakanaMap.put("ド", "do");
		katakanaMap.put("バ", "ba");
		katakanaMap.put("ビ", "bi");
		katakanaMap.put("ブ", "bu");
		katakanaMap.put("ベ", "be");
		katakanaMap.put("ボ", "bo");
		katakanaMap.put("パ", "pa");
		katakanaMap.put("ピ", "pi");
		katakanaMap.put("プ", "pu");
		katakanaMap.put("ペ", "pe");
		katakanaMap.put("ポ", "po");
		katakanaMap.put("キャ", "kya");
		katakanaMap.put("キュ", "kyu");
		katakanaMap.put("キョ", "kyo");
		katakanaMap.put("シャ", "sha");
		katakanaMap.put("シュ", "shu");
		katakanaMap.put("ショ", "sho");
		katakanaMap.put("チャ", "cha");
		katakanaMap.put("チュ", "chu");
		katakanaMap.put("チョ", "cho");
		katakanaMap.put("ニャ", "nya");
		katakanaMap.put("ニュ", "nyu");
		katakanaMap.put("ニョ", "nyo");
		katakanaMap.put("ヒャ", "hya");
		katakanaMap.put("ヒュ", "hyu");
		katakanaMap.put("ヒョ", "hyo");
		katakanaMap.put("リャ", "rya");
		katakanaMap.put("リュ", "ryu");
		katakanaMap.put("リョ", "ryo");
		katakanaMap.put("ギャ", "gya");
		katakanaMap.put("ギュ", "gyu");
		katakanaMap.put("ギョ", "gyo");
		katakanaMap.put("ジャ", "ja");
		katakanaMap.put("ジュ", "ju");
		katakanaMap.put("ジョ", "jo");
		katakanaMap.put("ティ", "ti");
		katakanaMap.put("ディ", "di");
		katakanaMap.put("ツィ", "tsi");
		katakanaMap.put("ヂャ", "dya");
		katakanaMap.put("ヂュ", "dyu");
		katakanaMap.put("ヂョ", "dyo");
		katakanaMap.put("ビャ", "bya");
		katakanaMap.put("ビュ", "byu");
		katakanaMap.put("ビョ", "byo");
		katakanaMap.put("ピャ", "pya");
		katakanaMap.put("ピュ", "pyu");
		katakanaMap.put("ピョ", "pyo");
		katakanaMap.put("ー", "-");
	}
	*/
}
