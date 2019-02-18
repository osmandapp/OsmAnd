package net.osmand.util;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.osmand.PlatformUtil;
import net.sf.junidecode.Junidecode;
import org.apache.commons.logging.Log;

public class TransliterationHelper {

	private static TransliterationHelper instance;

	public final static Log LOG = PlatformUtil.getLog(TransliterationHelper.class);
	public final static int DEFAULT = 1000;
	public final static int JAPANESE = 1001;

	private static int activeMapLanguage = DEFAULT;

	private Map<String, String> kanaMap = new HashMap<>();

	private TransliterationHelper(){}

	static {
		try {
			instance = new TransliterationHelper();
		} catch (Exception e) {
			LOG.debug(e.getMessage(), e);
		}
	}

	public static TransliterationHelper getInstance() {
		return instance;
	}

	public static void setActiveMapLanguage(int activeMapLanguage) {
		TransliterationHelper.activeMapLanguage = activeMapLanguage;
	}

	public static int getActiveMapLanguage() {
		return activeMapLanguage;
	}

	public String transliterateText(String text) {
		switch (activeMapLanguage) {
			case DEFAULT:
				return Junidecode.unidecode(text);
			case JAPANESE:
				return japanese2Romaji(text);
		}
		return text;
	}

	private String japanese2Romaji(String input) {

		boolean capitalizeWords = true;

		Tokenizer tokenizer = new Tokenizer();
		List<Token> tokens = tokenizer.tokenize(input);

		StringBuilder builder = new StringBuilder();
		if (kanaMap.isEmpty()) {
			initKanaMap();
		}
		String lastTokenToMerge = "";
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
						if (lastFeature.endsWith("ッ")) {
							lastTokenToMerge = lastFeature;
							continue;
						} else {
							lastTokenToMerge = "";
						}

						if (capitalizeWords) {
							builder.append(romaji.substring(0, 1).toUpperCase());
							builder.append(romaji.substring(1));
						} else {
							// Convert foreign katakana words to uppercase
							if (token.getSurface()
								.equals(token.getPronunciation())) // detect katakana
							{
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

	private String convertKanaToRomaji(String s) {
		StringBuilder t = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			if (i <= s.length() - 2) {
				if (kanaMap.containsKey(s.substring(i, i + 2))) {
					t.append(kanaMap.get(s.substring(i, i + 2)));
					i++;
				} else if (kanaMap.containsKey(s.substring(i, i + 1))) {
					t.append(kanaMap.get(s.substring(i, i + 1)));
				} else if (s.charAt(i) == 'ッ') {
					t.append(kanaMap.get(s.substring(i + 1, i + 2)).charAt(0));
				} else {
					t.append(s.charAt(i));
				}
			} else {
				if (kanaMap.containsKey(s.substring(i, i + 1))) {
					t.append(kanaMap.get(s.substring(i, i + 1)));
				} else {
					t.append(s.charAt(i));
				}
			}
		}
		return t.toString();
	}

	private void initKanaMap(){
		kanaMap.put("ア", "a");
		kanaMap.put("イ", "i");
		kanaMap.put("ウ", "u");
		kanaMap.put("エ", "e");
		kanaMap.put("オ", "o");
		kanaMap.put("カ", "ka");
		kanaMap.put("キ", "ki");
		kanaMap.put("ク", "ku");
		kanaMap.put("ケ", "ke");
		kanaMap.put("コ", "ko");
		kanaMap.put("サ", "sa");
		kanaMap.put("シ", "shi");
		kanaMap.put("ス", "su");
		kanaMap.put("セ", "se");
		kanaMap.put("ソ", "so");
		kanaMap.put("タ", "ta");
		kanaMap.put("チ", "chi");
		kanaMap.put("ツ", "tsu");
		kanaMap.put("テ", "te");
		kanaMap.put("ト", "to");
		kanaMap.put("ナ", "na");
		kanaMap.put("ニ", "ni");
		kanaMap.put("ヌ", "nu");
		kanaMap.put("ネ", "ne");
		kanaMap.put("ノ", "no");
		kanaMap.put("ハ", "ha");
		kanaMap.put("ヒ", "hi");
		kanaMap.put("フ", "fu");
		kanaMap.put("ヘ", "he");
		kanaMap.put("ホ", "ho");
		kanaMap.put("マ", "ma");
		kanaMap.put("ミ", "mi");
		kanaMap.put("ム", "mu");
		kanaMap.put("メ", "me");
		kanaMap.put("モ", "mo");
		kanaMap.put("ヤ", "ya");
		kanaMap.put("ユ", "yu");
		kanaMap.put("ヨ", "yo");
		kanaMap.put("ラ", "ra");
		kanaMap.put("リ", "ri");
		kanaMap.put("ル", "ru");
		kanaMap.put("レ", "re");
		kanaMap.put("ロ", "ro");
		kanaMap.put("ワ", "wa");
		kanaMap.put("ヲ", "wo");
		kanaMap.put("ン", "n");
		kanaMap.put("ガ", "ga");
		kanaMap.put("ギ", "gi");
		kanaMap.put("グ", "gu");
		kanaMap.put("ゲ", "ge");
		kanaMap.put("ゴ", "go");
		kanaMap.put("ザ", "za");
		kanaMap.put("ジ", "ji");
		kanaMap.put("ズ", "zu");
		kanaMap.put("ゼ", "ze");
		kanaMap.put("ゾ", "zo");
		kanaMap.put("ダ", "da");
		kanaMap.put("ヂ", "ji");
		kanaMap.put("ヅ", "zu");
		kanaMap.put("デ", "de");
		kanaMap.put("ド", "do");
		kanaMap.put("バ", "ba");
		kanaMap.put("ビ", "bi");
		kanaMap.put("ブ", "bu");
		kanaMap.put("ベ", "be");
		kanaMap.put("ボ", "bo");
		kanaMap.put("パ", "pa");
		kanaMap.put("ピ", "pi");
		kanaMap.put("プ", "pu");
		kanaMap.put("ペ", "pe");
		kanaMap.put("ポ", "po");
		kanaMap.put("キャ", "kya");
		kanaMap.put("キュ", "kyu");
		kanaMap.put("キョ", "kyo");
		kanaMap.put("シャ", "sha");
		kanaMap.put("シュ", "shu");
		kanaMap.put("ショ", "sho");
		kanaMap.put("チャ", "cha");
		kanaMap.put("チュ", "chu");
		kanaMap.put("チョ", "cho");
		kanaMap.put("ニャ", "nya");
		kanaMap.put("ニュ", "nyu");
		kanaMap.put("ニョ", "nyo");
		kanaMap.put("ヒャ", "hya");
		kanaMap.put("ヒュ", "hyu");
		kanaMap.put("ヒョ", "hyo");
		kanaMap.put("リャ", "rya");
		kanaMap.put("リュ", "ryu");
		kanaMap.put("リョ", "ryo");
		kanaMap.put("ギャ", "gya");
		kanaMap.put("ギュ", "gyu");
		kanaMap.put("ギョ", "gyo");
		kanaMap.put("ジャ", "ja");
		kanaMap.put("ジュ", "ju");
		kanaMap.put("ジョ", "jo");
		kanaMap.put("ティ", "ti");
		kanaMap.put("ディ", "di");
		kanaMap.put("ツィ", "tsi");
		kanaMap.put("ヂャ", "dya");
		kanaMap.put("ヂュ", "dyu");
		kanaMap.put("ヂョ", "dyo");
		kanaMap.put("ビャ", "bya");
		kanaMap.put("ビュ", "byu");
		kanaMap.put("ビョ", "byo");
		kanaMap.put("ピャ", "pya");
		kanaMap.put("ピュ", "pyu");
		kanaMap.put("ピョ", "pyo");
		kanaMap.put("ー", "-");
	}
}
