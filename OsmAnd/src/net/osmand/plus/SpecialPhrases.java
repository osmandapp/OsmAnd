package net.osmand.plus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.osmand.Algoritms;

import android.content.Context;
import android.content.res.AssetManager;

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
	 * If the language isn't set yet, a nullpointer exception will be thrown
	 * 
	 * @param key the subtype to query
	 * @return the special phrase according to the asked key, or "null" if the key isn't found
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
	 * @throws IOException when reading the text file failed
	 */
	public static void setLanguage(Context ctx, OsmandSettings settings) throws IOException {
		String lang = getPreferredLanguage(settings).getLanguage();
		m = new HashMap<String,String>();
		// The InputStream opens the resourceId and sends it to the buffer
		InputStream is = null;
		BufferedReader br = null;
		try{
			try {
				is = ctx.getAssets().open("specialphrases/specialphrases_"+lang+".txt");
			} catch (IOException ex) {
				// second try: default to English, if this fails, the error is thrown outside
				is = ctx.getAssets().open("specialphrases/specialphrases_en.txt");
			}
			br = new BufferedReader(new InputStreamReader(is));
			String readLine = null;

			// While the BufferedReader readLine is not null 
			while ((readLine = br.readLine()) != null) {
				String[] arr = readLine.split(",");
				if (arr != null && arr.length == 2) {
					m.put(arr[0], arr[1]);
				}

			}

			// Close the InputStream and BufferedReader
			is.close();
			br.close();
		} finally {
			Algoritms.closeStream(is);
			Algoritms.closeStream(br);
		}

	}
	
	/**
	 * Returns the preferred language
	 * @param set the OsmandSettings used
	 * @return ENGLISH if English names are chosen in the settings, Locale.getDefault otherwise
	 */
	public static Locale getPreferredLanguage(OsmandSettings set){
		if (set.usingEnglishNames()) {
			return Locale.ENGLISH;
		} 
		return Locale.getDefault();
		
	}
	
	

}
