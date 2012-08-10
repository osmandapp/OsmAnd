package net.osmand.plus.specialPhrases;

import java.util.Locale;
import java.util.Map;

public class SpecialPhrases {


	private static Map<String,String> m;
	
	/**
	 * Check if the language has been set
	 * @return true if language has been set
	 */
	public static boolean isLanguageSet(){
		return m!= null;
	}

	/**
	 * Use this method to set the language.
	 * 
	 * @param key the key to query
	 * @return the special phrase according to the asked key
	 */
	public static String getSpecialPhrase(String key){
		if (m == null) {
			throw new NullPointerException("Use the setLanguage method before trying to get a phrase");
		}
		return m.get(key);
	}

	/**
	 * Set the special phrases to a certain language.
	 * This method needs to be used before the getSpecialPhrase method
	 * 
	 * @param lang the language to use
	 */
	public static void setLanguage(Locale lang) {
		if (lang.getLanguage().equals(new Locale("af").getLanguage())) {
			m = SpecialPhrasesAF.createMap();
		} else if (lang.getLanguage().equals(new Locale("ar").getLanguage())) {
			m = SpecialPhrasesAR.createMap();
		} else if (lang.getLanguage().equals(new Locale("br").getLanguage())) {
			m = SpecialPhrasesBR.createMap();
		} else if (lang.getLanguage().equals(new Locale("ca").getLanguage())) {
			m = SpecialPhrasesCA.createMap();
		} else if (lang.getLanguage().equals(new Locale("cs").getLanguage())) {
			m = SpecialPhrasesCS.createMap();
		} else if (lang.getLanguage().equals(new Locale("de").getLanguage())) {
			m = SpecialPhrasesDE.createMap();
		} else if (lang.getLanguage().equals(new Locale("en").getLanguage())) {
			m = SpecialPhrasesEN.createMap();
		} else if (lang.getLanguage().equals(new Locale("es").getLanguage())) {
			m = SpecialPhrasesES.createMap();
		} else if (lang.getLanguage().equals(new Locale("et").getLanguage())) {
			m = SpecialPhrasesET.createMap();
		} else if (lang.getLanguage().equals(new Locale("eu").getLanguage())) {
			m = SpecialPhrasesEU.createMap();
		} else if (lang.getLanguage().equals(new Locale("fa").getLanguage())) {
			m = SpecialPhrasesFA.createMap();
		} else if (lang.getLanguage().equals(new Locale("fi").getLanguage())) {
			m = SpecialPhrasesFI.createMap();
		} else if (lang.getLanguage().equals(new Locale("fr").getLanguage())) {
			m = SpecialPhrasesFR.createMap();
		} else if (lang.getLanguage().equals(new Locale("gl").getLanguage())) {
			m = SpecialPhrasesGL.createMap();
		} else if (lang.getLanguage().equals(new Locale("hr").getLanguage())) {
			m = SpecialPhrasesHR.createMap();
		} else if (lang.getLanguage().equals(new Locale("hu").getLanguage())) {
			m = SpecialPhrasesHU.createMap();
		} else if (lang.getLanguage().equals(new Locale("ia").getLanguage())) {
			m = SpecialPhrasesIA.createMap();
		} else if (lang.getLanguage().equals(new Locale("is").getLanguage())) {
			m = SpecialPhrasesIS.createMap();
		} else if (lang.getLanguage().equals(new Locale("it").getLanguage())) {
			m = SpecialPhrasesIT.createMap();
		} else if (lang.getLanguage().equals(new Locale("ja").getLanguage())) {
			m = SpecialPhrasesJA.createMap();
		} else if (lang.getLanguage().equals(new Locale("mk").getLanguage())) {
			m = SpecialPhrasesMK.createMap();
		} else if (lang.getLanguage().equals(new Locale("nl").getLanguage())) {
			m = SpecialPhrasesNL.createMap();
		} else if (lang.getLanguage().equals(new Locale("no").getLanguage())) {
			m = SpecialPhrasesNO.createMap();
		} else if (lang.getLanguage().equals(new Locale("pl").getLanguage())) {
			m = SpecialPhrasesPL.createMap();
		} else if (lang.getLanguage().equals(new Locale("ps").getLanguage())) {
			m = SpecialPhrasesPS.createMap();
		} else if (lang.getLanguage().equals(new Locale("pt").getLanguage())) {
			m = SpecialPhrasesPT.createMap();
		} else if (lang.getLanguage().equals(new Locale("ru").getLanguage())) {
			m = SpecialPhrasesRU.createMap();
		} else if (lang.getLanguage().equals(new Locale("sk").getLanguage())) {
			m = SpecialPhrasesSK.createMap();
		} else if (lang.getLanguage().equals(new Locale("sv").getLanguage())) {
			m = SpecialPhrasesSV.createMap();
		} else if (lang.getLanguage().equals(new Locale("uk").getLanguage())) {
			m = SpecialPhrasesUK.createMap();
		} else if (lang.getLanguage().equals(new Locale("vi").getLanguage())) {
			m = SpecialPhrasesVI.createMap();
		} else {
			// default case
			m = SpecialPhrasesEN.createMap();
		}

	}
}
