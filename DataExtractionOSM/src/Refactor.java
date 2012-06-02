import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.osmand.Algoritms;

public class Refactor {

	private static final String OSM_AND_RES = "../OsmAnd/res";
	public  String[] images = new String[] {
			"arrow_down.png"	,	"The arrow_down.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"arrow_up.png"	,	"The arrow_up.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"back_to_loc_disabled.png"	,	"The back_to_loc_disabled.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"back_to_loc_normal.png"	,	"The back_to_loc_normal.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"back_to_loc_pressed.png"	,	"The back_to_loc_pressed.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"background.png"	,	"The background.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"background.png"	,	"The background.png icon has identical contents in the following configuration folders: drawable-land-hdpi, drawable-large-land"	,
			"beetle_icon_off.png"	,	"The beetle_icon_off.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"beetle_icon_on.png"	,	"The beetle_icon_on.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"bg_left_pushed.9.png"	,	"The bg_left_pushed.9.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-mdpi"	,
			"bg_left_simple.9.png"	,	"The bg_left_simple.9.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-mdpi"	,
			"bg_leftr_pushed.9.png"	,	"The bg_leftr_pushed.9.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-mdpi"	,
			"bg_leftr_simple.9.png"	,	"The bg_leftr_simple.9.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-mdpi"	,
			"bg_right_pushed.9.png"	,	"The bg_right_pushed.9.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-mdpi"	,
			"bg_right_simple.9.png"	,	"The bg_right_simple.9.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-mdpi"	,
			"bg_rightr_pushed.9.png"	,	"The bg_rightr_pushed.9.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-mdpi"	,
			"bg_rightr_simple.9.png"	,	"The bg_rightr_simple.9.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-mdpi"	,
			"bicycle_icon_off.png"	,	"The bicycle_icon_off.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"bicycle_icon_on.png"	,	"The bicycle_icon_on.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"bicycle_small.png"	,	"The bicycle_small.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"bottom_shadow.9.png"	,	"The bottom_shadow.9.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-mdpi"	,
			"box_expand_normal.png"	,	"The box_expand_normal.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"box_expand_pressed.png"	,	"The box_expand_pressed.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"box_expand_trans_normal.png"	,	"The box_expand_trans_normal.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"box_expand_trans_pressed.png"	,	"The box_expand_trans_pressed.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"box_free_pressed.9.png"	,	"The box_free_pressed.9.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-mdpi"	,
			"box_free_simple.9.png"	,	"The box_free_simple.9.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-mdpi"	,
			"box_leg.png"	,	"drawable-hdpi, drawable-large"	,
			"box_top_l_normal.9.png"	,	"drawable-hdpi, drawable-mdpi"	,
			"box_top_l_pressed.9.png"	,	"drawable-hdpi, drawable-mdpi"	,
			"box_top_pressed.9.png"	,	"drawable-hdpi, drawable-mdpi"	,
			"box_top_r_normal.9.png"	,	"The box_top_r_normal.9.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-mdpi"	,
			"box_top_r_pressed.9.png"	,	"The box_top_r_pressed.9.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-mdpi"	,
			"box_top_simple.9.png"	,	"The box_top_simple.9.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-mdpi"	,
			"box_top_stack_normal.9.png"	,	"The box_top_stack_normal.9.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-mdpi"	,
			"box_top_stack_pressed.9.png"	,	"The box_top_stack_pressed.9.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-mdpi"	,
			"box_top_trans_l.9.png"	,	"The box_top_trans_l.9.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-mdpi"	,
			"box_top_trans_r.9.png"	,	"The box_top_trans_r.9.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-mdpi"	,
			"box_top_trans.9.png"	,	"The box_top_trans.9.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-mdpi"	,
			"button_icon_favorites.png"	,	"The button_icon_favorites.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-land-hdpi, drawable-large-land, drawable-large"	,
			"button_icon_favorites.png"	,	"The button_icon_favorites.png icon has identical contents in the following configuration folders: drawable-land, drawable-mdpi "	,
			"button_icon_map.png"	,	"The button_icon_map.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-land-hdpi, drawable-large-land, drawable-large"	,
			"button_icon_map.png"	,	"The button_icon_map.png icon has identical contents in the following configuration folders: drawable-land, drawable-mdpi"	,
			"button_icon_search.png"	,	"The button_icon_search.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-land-hdpi, drawable-large-land, drawable-large"	,
			"button_icon_search.png"	,	"The button_icon_search.png icon has identical contents in the following configuration folders: drawable-land, drawable-mdpi"	,
			"button_icon_settings.png"	,	"The button_icon_settings.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-land-hdpi, drawable-large-land, drawable-large"	,
			"button_icon_settings.png"	,	"The button_icon_settings.png icon has identical contents in the following configuration folders: drawable-land, drawable-mdpi"	,
			"car_small.png"	,	"The car_small.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"compass_pushed.png"	,	"The compass_pushed.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"default_mode_small.png"	,	"The default_mode_small.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"globus_normal.png"	,	"The globus_normal.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"globus_pressed.png"	,	"The globus_pressed.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"headline_close_button_pressed.png"	,	"The headline_close_button_pressed.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"headline_close_button.png"	,	"The headline_close_button.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"help_icon_pressed.png"	,	"The help_icon_pressed.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"help_icon_simple.png"	,	"The help_icon_simple.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"ic_altitude.png"	,	"The ic_altitude.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"icon_small.png"	,	"The icon_small.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"info_target.png"	,	"The info_target.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"info_time_to_go.png"	,	"The info_time_to_go.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"info_time.png"	,	"The info_time.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"map_btn_menu_o.png"	,	"The map_btn_menu_o.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"map_btn_menu_p.png"	,	"The map_btn_menu_p.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"map_zoom_in_o.png"	,	"The map_zoom_in_o.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"map_zoom_in_p.png"	,	"The map_zoom_in_p.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"map_zoom_out_o.png"	,	"The map_zoom_out_o.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"map_zoom_out_p.png"	,	"The map_zoom_out_p.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"pedestrian_icon_off.png"	,	"The pedestrian_icon_off.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"pedestrian_icon_on.png"	,	"The pedestrian_icon_on.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"pedestrian_small.png"	,	"The pedestrian_small.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"poi.png"	,	"The poi.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-mdpi"	,
			"progress_blue.png"	,	"The progress_blue.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"progress_green.png"	,	"The progress_green.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"progress_grey.png"	,	"The progress_grey.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"tab_back_button_arrow.png"	,	"The tab_back_button_arrow.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"tab_back_button_background.png"	,	"The tab_back_button_background.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"tab_favorites_screen_icon.png"	,	"The tab_favorites_screen_icon.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"tab_icon_favourite_menu.png"	,	"The tab_icon_favourite_menu.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"tab_icon_panel_pushed.png"	,	"The tab_icon_panel_pushed.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"tab_icon_panel.png"	,	"The tab_icon_panel.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"tab_search_address_icon.png"	,	"The tab_search_address_icon.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"tab_search_favorites_icon.png"	,	"The tab_search_favorites_icon.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"tab_search_history_icon.png"	,	"The tab_search_history_icon.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"tab_search_location_icon.png"	,	"The tab_search_location_icon.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"tab_search_panel_background.png"	,	"The tab_search_panel_background.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"tab_search_panel_button_arrow.png"	,	"The tab_search_panel_button_arrow.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"tab_search_panel_button_background_pushed.png"	,	"The tab_search_panel_button_background_pushed.png icon has identical contents in the following configuration folders: drawable-hdpi, drawable-large"	,
			"tab_search_panel_button_background.png"	,	"drawable-hdpi, drawable-large"	,
			"tab_search_transport_icon.png"	,	"drawable-hdpi, drawable-large"	,
			"tab_settings_screen_icon.png"	,	"drawable-hdpi, drawable-large"	,
			"tab_text_separator.png",		"drawable-hdpi, drawable-large",
			"target_point.png"	,	"drawable-hdpi, drawable-large"	,
			"top_shadow.png"	,	"drawable-hdpi, drawable-large"	,
			"zoom_background.9.png"	,	"drawable-hdpi, drawable-large"
			};

	public void run() throws IOException {
		File drawable = new File(OSM_AND_RES,"drawable");
		checkDir(drawable);
		loadDrawables();
		for (int i = 0; i < images.length; i=i+2) {
			String file = images[i];
			String desc = images[i+1];
			List<File> dirs = extractDirs(desc);
			File fileTo = new File(drawable, expandFile(file,desc));
			System.out.println("Copy to:" + fileTo.getAbsolutePath());
			copyFile(new File(dirs.get(0), file), fileTo);
			for (File delDir : dirs) {
				if (!new File(delDir,file).delete()) {
					throw new IOException("Failed to delete:" + new File(delDir,file).getAbsolutePath());
				}
				System.out.println("\tDelete from " + delDir.getAbsolutePath());
				System.out.println("\tXML file " + new File(delDir,cutFile(file)+".xml").getAbsolutePath());
				FileWriter fileWriter = new FileWriter(new File(delDir,cutFile(file)+".xml"));
				writeFile(cutFile(fileTo.getName()), file, fileWriter);
				Algoritms.closeStream(fileWriter);
			}
			writeFile(cutFile(fileTo.getName()), file, new OutputStreamWriter(System.out));
		}
	}

	private void copyFile(File fileFrom, File fileTo) throws IOException {
		FileInputStream in = new FileInputStream(fileFrom);
		FileOutputStream out = new FileOutputStream(fileTo);
		Algoritms.streamCopy(in, out);
		Algoritms.closeStream(in);
		Algoritms.closeStream(out);
	}

	private String cutFile(String name) {
//		String name = fileTo.getName();
		return name.substring(0,name.indexOf('.'));
	}

	private void writeFile(String fileTo, String file, Writer fileWriter) throws IOException {
		String tag = "bitmap";
		if (file.contains(".9.")) {
			tag = "nine-patch";
		}
		fileWriter.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
		fileWriter.append("<" + tag + " xmlns:android=\"http://schemas.android.com/apk/res/android\" android:src=\"@drawable/" + fileTo + "\" />\n");
		fileWriter.flush();
	}

	private String expandFile(String file, String desc) throws IOException {
		String ext = "";
		if (desc.indexOf("mdpi") != -1) {
			ext = "_mdpi.";
		} else if (desc.indexOf("hdpi") != -1) {
			ext = "_hdpi.";
		} else {
			throw new IOException("Don't know how to expand with desc:" + desc);
		}
		return file.replaceFirst("\\.", ext);
	}

	private List<String> drawables = new ArrayList<String>();

	private void loadDrawables() throws IOException {
		File res = new File(OSM_AND_RES);
		checkDir(res);
		drawables.addAll(Arrays.asList(res.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {
				return filename.startsWith("drawable-") && new File(dir,filename).isDirectory();
			}
		})));
		Collections.sort(drawables, new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				return - o1.length() + o2.length();
			}
		});
	}

	private void checkDir(File drawable) throws IOException {
		if (!drawable.exists() || !drawable.isDirectory()) {
			throw new IOException(drawable.getAbsolutePath() + " does not exits or is not a directory!");
		}
	}
	
	private List<File> extractDirs(String desc) {
		List<File> result = new ArrayList<File>();
		for (String drawable : drawables) { //must be sorted from largest to smallets..
			if (desc.contains(drawable)) {
				desc = desc.replace(drawable, ""); //to not have prefix findings...
				result.add(new File(OSM_AND_RES,drawable));
			}
		}
		return result;
	}

	public static void main(String[] args) throws IOException {
		new Refactor().run();
	}
}
