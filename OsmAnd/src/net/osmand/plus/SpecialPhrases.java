package net.osmand.plus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.osmand.plus.R;

import android.content.Context;

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
	 * Use this method to query a special phrase for a certain subtype
	 * 
	 * If the language isn't set yet, it will default to English
	 * 
	 * @param key the subtype to query
	 * @return the special phrase according to the asked key
	 */
	public static String getSpecialPhrase(String key){
		if (!isLanguageSet()) {
			throw new NullPointerException("The language has not been set");
		}
		return m.get(key);
	}

	/**
	 * Set the special phrases to a certain language.
	 * This method needs to be used before the getSpecialPhrase method
	 * 
	 * @param lang the language to use
	 */
	public static void setLanguage(Context ctx, Locale lang) {
		if (lang.getLanguage().equals(new Locale("af").getLanguage())) {
			loadText(ctx, R.raw.specialphrases_af);
		} else if (lang.getLanguage().equals(new Locale("ar").getLanguage())) {
			loadText(ctx, R.raw.specialphrases_ar);
		} else if (lang.getLanguage().equals(new Locale("br").getLanguage())) {
			loadText(ctx, R.raw.specialphrases_br);
		} else if (lang.getLanguage().equals(new Locale("ca").getLanguage())) {
			loadText(ctx, R.raw.specialphrases_ca);
		} else if (lang.getLanguage().equals(new Locale("cs").getLanguage())) {
			loadText(ctx, R.raw.specialphrases_cs);
		} else if (lang.getLanguage().equals(new Locale("de").getLanguage())) {
			loadText(ctx, R.raw.specialphrases_de);
		} else if (lang.getLanguage().equals(new Locale("en").getLanguage())) {
			loadText(ctx, R.raw.specialphrases_en);
		} else if (lang.getLanguage().equals(new Locale("es").getLanguage())) {
			loadText(ctx, R.raw.specialphrases_es);
		} else if (lang.getLanguage().equals(new Locale("et").getLanguage())) {
			loadText(ctx, R.raw.specialphrases_et);
		} else if (lang.getLanguage().equals(new Locale("eu").getLanguage())) {
			loadText(ctx, R.raw.specialphrases_eu);
		} else if (lang.getLanguage().equals(new Locale("fa").getLanguage())) {
			loadText(ctx, R.raw.specialphrases_fa);
		} else if (lang.getLanguage().equals(new Locale("fi").getLanguage())) {
			loadText(ctx, R.raw.specialphrases_fi);
		} else if (lang.getLanguage().equals(new Locale("fr").getLanguage())) {
			loadText(ctx, R.raw.specialphrases_fr);
		} else if (lang.getLanguage().equals(new Locale("gl").getLanguage())) {
			loadText(ctx, R.raw.specialphrases_gl);
		} else if (lang.getLanguage().equals(new Locale("hr").getLanguage())) {
			loadText(ctx, R.raw.specialphrases_hr);
		} else if (lang.getLanguage().equals(new Locale("hu").getLanguage())) {
			loadText(ctx, R.raw.specialphrases_hu);
		} else if (lang.getLanguage().equals(new Locale("ia").getLanguage())) {
			loadText(ctx, R.raw.specialphrases_ia);
		} else if (lang.getLanguage().equals(new Locale("is").getLanguage())) {
			loadText(ctx, R.raw.specialphrases_is);
		} else if (lang.getLanguage().equals(new Locale("it").getLanguage())) {
			loadText(ctx, R.raw.specialphrases_it);
		} else if (lang.getLanguage().equals(new Locale("ja").getLanguage())) {
			loadText(ctx, R.raw.specialphrases_ja);
		} else if (lang.getLanguage().equals(new Locale("mk").getLanguage())) {
			loadText(ctx, R.raw.specialphrases_mk);
		} else if (lang.getLanguage().equals(new Locale("nl").getLanguage())) {
			loadText(ctx, R.raw.specialphrases_nl);
		} else if (lang.getLanguage().equals(new Locale("no").getLanguage())) {
			loadText(ctx, R.raw.specialphrases_no);
		} else if (lang.getLanguage().equals(new Locale("pl").getLanguage())) {
			loadText(ctx, R.raw.specialphrases_pl);
		} else if (lang.getLanguage().equals(new Locale("ps").getLanguage())) {
			loadText(ctx, R.raw.specialphrases_ps);
		} else if (lang.getLanguage().equals(new Locale("pt").getLanguage())) {
			loadText(ctx, R.raw.specialphrases_pt);
		} else if (lang.getLanguage().equals(new Locale("ru").getLanguage())) {
			loadText(ctx, R.raw.specialphrases_ru);
		} else if (lang.getLanguage().equals(new Locale("sk").getLanguage())) {
			loadText(ctx, R.raw.specialphrases_sk);
		} else if (lang.getLanguage().equals(new Locale("sv").getLanguage())) {
			loadText(ctx, R.raw.specialphrases_sv);
		} else if (lang.getLanguage().equals(new Locale("uk").getLanguage())) {
			loadText(ctx, R.raw.specialphrases_uk);
		} else if (lang.getLanguage().equals(new Locale("vi").getLanguage())) {
			loadText(ctx, R.raw.specialphrases_vi);
		} else {
			// default case
			loadText(ctx, R.raw.specialphrases_en);
		}

	}
	
	public static void loadText(Context ctx, int resourceId) {
		m = new HashMap<String,String>();
		
	    // The InputStream opens the resourceId and sends it to the buffer
	    InputStream is = ctx.getResources().openRawResource(resourceId);
	    BufferedReader br = new BufferedReader(new InputStreamReader(is));
	    String readLine = null;

	    try {
	        // While the BufferedReader readLine is not null 
	        while ((readLine = br.readLine()) != null) {
	        	String[] arr = readLine.split(",");
	        	if (arr != null && arr.length == 2) 
	        		m.put(arr[0], arr[1]);
	        }

		    // Close the InputStream and BufferedReader
		    is.close();
		    br.close();

	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}

}
