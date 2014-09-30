/**
 *
 */

package net.osmand.plus.activities;



import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.TurnPathHelper;
import net.osmand.plus.views.controls.MapRouteInfoControl;
import net.osmand.util.Algorithms;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintAttributes.Margins;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.pdfjet.A4;
import com.pdfjet.CodePage;
import com.pdfjet.Font;
import com.pdfjet.PDF;
import com.pdfjet.Page;

/**
 *
 */
public class ShowRouteInfoActivity extends OsmandListActivity {


	private static final int SAVE = 0;
	private static final int SHARE = 1;
	private static final int PRINT = 2;
	private final static float PDF_FONT_SIZE = 12f;
	private final static float PDF_DERAULT_MARGINS = 36f;
	private final static float PDF_TEXT_LEADING = 14f;
	private RoutingHelper helper;
	private TextView header;
	private DisplayMetrics dm;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		ListView lv = new ListView(this);
		lv.setId(android.R.id.list);
		header = new TextView(this);
		helper = ((OsmandApplication)getApplication()).getRoutingHelper();
		lv.addHeaderView(header);
		setContentView(lv);
		dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == SAVE) {
			MapActivityActions.createSaveDirections(ShowRouteInfoActivity.this, helper).show();
			return true;
		}
        if (item.getItemId() == SHARE) {
              final GPXFile gpx = getMyApplication().getRoutingHelper().generateGPXFileWithRoute();

              final Intent sendIntent = new Intent();
              sendIntent.setAction(Intent.ACTION_SEND);
              sendIntent.putExtra(Intent.EXTRA_TEXT, GPXUtilities.asString(gpx, getMyApplication()));
              sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_route_subject));
              sendIntent.setType("application/gpx+xml");
              startActivity(sendIntent);
            return true;
        }
        if (item.getItemId() == PRINT) {
        	print();
          return true;
      }

		return super.onOptionsItemSelected(item);
	}


	@Override
	protected void onResume() {
		super.onResume();
		header.setText(helper.getGeneralRouteInformation());
		float f = Math.min(dm.widthPixels/(dm.density*160),dm.heightPixels/(dm.density*160));
		if (f >= 3) {
			// large screen
			header.setTextSize(dm.scaledDensity * 23);
		}
		setListAdapter(new RouteInfoAdapter(((OsmandApplication)getApplication()).getRoutingHelper().getRouteDirections()));
	}

	@Override
	public void onListItemClick(ListView parent, View v, int position, long id) {
		// headers are included
		if(position < 1){
			return;
		}
		RouteDirectionInfo item = ((RouteInfoAdapter)getListAdapter()).getItem(position - 1);
		Location loc = helper.getLocationFromRouteDirection(item);
		if(loc != null){
			MapRouteInfoControl.directionInfo = position - 1;
			OsmandSettings settings = ((OsmandApplication) getApplication()).getSettings();
			settings.setMapLocationToShow(loc.getLatitude(),loc.getLongitude(),
					Math.max(13, settings.getLastKnownMapZoom()), null, item.getDescriptionRoute(((OsmandApplication) getApplication())) + " " + getTimeDescription(item), null);
			MapActivity.launchMapActivityMoveToTop(this);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		createMenuItem(menu, PRINT, R.string.print_route,
				R.drawable.ic_action_gprint_light, R.drawable.ic_action_gprint_dark,
				MenuItem.SHOW_AS_ACTION_ALWAYS);
		createMenuItem(menu, SAVE, R.string.save_route_as_gpx,
				R.drawable.ic_action_gsave_light, R.drawable.ic_action_gsave_dark,
				MenuItem.SHOW_AS_ACTION_ALWAYS);
		createMenuItem(menu, SHARE, R.string.share_route_as_gpx,
				R.drawable.ic_action_gshare_light, R.drawable.ic_action_gshare_dark,
				MenuItem.SHOW_AS_ACTION_ALWAYS);
		return super.onCreateOptionsMenu(menu);
	}

	class RouteInfoAdapter extends ArrayAdapter<RouteDirectionInfo> {
		RouteInfoAdapter(List<RouteDirectionInfo> list) {
			super(ShowRouteInfoActivity.this, R.layout.route_info_list_item, list);
			this.setNotifyOnChange(false);
		}


		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.route_info_list_item, parent, false);
			}
			RouteDirectionInfo model = (RouteDirectionInfo) getItem(position);
			TextView label = (TextView) row.findViewById(R.id.description);
			TextView distanceLabel = (TextView) row.findViewById(R.id.distance);
			TextView timeLabel = (TextView) row.findViewById(R.id.time);
			ImageView icon = (ImageView) row.findViewById(R.id.direction);

			TurnPathHelper.RouteDrawable drawable = new TurnPathHelper.RouteDrawable(getResources());
			drawable.setRouteType(model.getTurnType());
			icon.setImageDrawable(drawable);


			distanceLabel.setText(OsmAndFormatter.getFormattedDistance(model.distance, getMyApplication()));
			label.setText(model.getDescriptionRoute(((OsmandApplication) getApplication())));
			String timeText = getTimeDescription(model);
			timeLabel.setText(timeText);
			return row;
		}


	}

	private String getTimeDescription(RouteDirectionInfo model) {
		final int timeInSeconds = model.getExpectedTime();
		return Algorithms.formatDuration(timeInSeconds);
	}

	@SuppressLint("NewApi")
	private class RouteInfoPrintDocumentAdapter extends PrintDocumentAdapter {
		List<RouteDirectionInfo> list;
		String title;
		float[] pageSize;
		float marginLeft;
		float marginRight;
		float marginTop;
		float marginBottom;
		int totalPages;
		int linesPerPage;

		public RouteInfoPrintDocumentAdapter(List<RouteDirectionInfo> list, String title) {
			this.title = title;
			this.list = list;
			pageSize = new float[2];
			marginLeft = 0f;
			marginRight = 0f;
			marginTop = 0f;
			marginBottom = 0f;
			totalPages = 0;
			linesPerPage = 0;
		}

		@Override
		public void onLayout(PrintAttributes oldAttributes,
				PrintAttributes newAttributes,
				CancellationSignal cancellationSignal,
				LayoutResultCallback callback, Bundle extras) {

			if (cancellationSignal.isCanceled()) {
				callback.onLayoutCancelled();
				return;
			}

			pageSize[0] = (float) Math.rint(newAttributes.getMediaSize()
					.getWidthMils() * 72f / 1000f);
			pageSize[1] = (float) Math.rint(newAttributes.getMediaSize()
					.getHeightMils() * 72f / 1000f);

			Margins margins = newAttributes.getMinMargins();
			marginLeft = (float) Math.rint(margins.getLeftMils() * 72 / 1000f);
			marginRight = (float) Math.rint(margins.getRightMils() * 72 / 1000f);
			marginTop =  (float) Math.rint(margins.getTopMils() * 72 / 1000f);
			marginBottom =  (float) Math.rint(margins.getBottomMils() * 72 / 1000f);
			if (marginLeft == 0f) {
				marginLeft = PDF_DERAULT_MARGINS;
			}
			if (marginRight == 0f) {
				marginRight = PDF_DERAULT_MARGINS;
			}
			if (marginTop == 0f) {
				marginTop = PDF_DERAULT_MARGINS;
			}
			if (marginBottom == 0f) {
				marginBottom = PDF_DERAULT_MARGINS;
			}

			linesPerPage = computeLinesPerPage();
			totalPages = computePageCount();

			if (totalPages > 0) {
				PrintDocumentInfo info = new PrintDocumentInfo.Builder(
						"print_output.pdf")
						.setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
						.setPageCount(totalPages).build();
				callback.onLayoutFinished(info, true);
			} else {
				// Otherwise report an error to the print framework
				callback.onLayoutFailed("Page count calculation failed.");
			}
		}

		@Override
		public void onWrite(PageRange[] pages,
				ParcelFileDescriptor destination,
				CancellationSignal cancellationSignal,
				WriteResultCallback callback) {
			ArrayList<Integer> writtenPagesArray = new ArrayList<Integer>();

			PDF pdf = null;
			InputStream fontInputStream = null;
			try {
				pdf = new PDF(new BufferedOutputStream(new FileOutputStream(
						destination.getFileDescriptor())));
				fontInputStream = getAssets().open("fonts/Roboto-Regular.ttf");
				Font font = new Font(pdf, fontInputStream, CodePage.UNICODE, true);
				font.setSize(PDF_FONT_SIZE);
				int totalLinesCount = totalLinesCount();
				int shift = 0;
				if (!TextUtils.isEmpty(title)) {
					shift = 1;
				}
				for (int i = 0; i < totalPages; i++) {
					if (containsPage(pages, i)) {
						Page page = new Page(pdf, pageSize);
						page.setTextStart();
						page.setTextFont(font);
						page.setTextLocation(marginLeft, marginTop);
						page.setTextLeading(PDF_TEXT_LEADING);
						int startLineIndex = i * linesPerPage;
						for (int j = startLineIndex; (j < (startLineIndex + linesPerPage)) && (j < totalLinesCount); j++) {
							if ((j == 0) && (shift > 0)) {
								page.println(title);
							} else {
								RouteDirectionInfo model = list.get(j - shift);
						        String distance = OsmAndFormatter.getFormattedDistance(model.distance, getMyApplication());
						        String description = model.getDescriptionRoute(((OsmandApplication) getApplication()));
								String timeText = getTimeDescription(model);
					            page.println(distance + " " + description + " " + timeText);
							}
						}
						page.setTextEnd();
						writtenPagesArray.add(i);
					}
				}
			} catch (Exception e) {
		        callback.onWriteFailed(e.toString());
		        return;
			} finally {
				if (pdf != null) {
					try {
						pdf.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				if (fontInputStream != null) {
					try {
						fontInputStream.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			PageRange[] writtenPages = computeWrittenPages(writtenPagesArray);
			callback.onWriteFinished(writtenPages);
		}


		private int computeLinesPerPage() {
			int linesPerPage = 0;
			if (pageSize[1] > 0) {
				float height = pageSize[1];
				height -= marginTop;
				height -= marginBottom;
				linesPerPage = (int)(height / PDF_FONT_SIZE + PDF_TEXT_LEADING);
			}

			return linesPerPage;
		}

		private int computePageCount() {
			int pageCount = 0;
			int totalLinesCount = totalLinesCount();
			if ((totalLinesCount > 0) && (linesPerPage > 0)) {
				pageCount = totalLinesCount / linesPerPage;
				if ((totalLinesCount % linesPerPage) != 0) {
					pageCount++;
				}
			}

			return pageCount;
		}

		private int totalLinesCount() {
			int totalLinesCount = list.size();
			if (!TextUtils.isEmpty(title)) {
				totalLinesCount++;
			}
			return totalLinesCount;
		}

		private boolean containsPage(PageRange[] pages, int page) {
			boolean contains = false;
			for (int i = 0; i < pages.length; i++) {
				if (pages[i].equals(PageRange.ALL_PAGES)) {
					contains = true;
					break;
				} else if ((pages[i].getStart() <= page)
						&& (page <= pages[i].getEnd())) {
					contains = true;
					break;
				}
			}

			return contains;
		}

		private PageRange[] computeWrittenPages(List<Integer> pages) {
			ArrayList<PageRange> writtenPagesList = new ArrayList<PageRange>();
			int pagesSize = pages.size();
			if (pagesSize > 0) {
				int start = pages.get(0);
				int end = start;
				for (int i = 1; i < pagesSize; i++) {
					int page = pages.get(i);
					if ((page - end) > 1) {
						PageRange pageRange = new PageRange(start, end);
						writtenPagesList.add(pageRange);
						start = page;
					}
					end = page;
				}
				PageRange lastPageRange = null;
				int writtenPagesListSize = writtenPagesList.size();
				if (writtenPagesListSize > 0) {
					lastPageRange = writtenPagesList.get(writtenPagesListSize - 1);
				}
				if (lastPageRange == null) {
					writtenPagesList.add(PageRange.ALL_PAGES);
				} else {
					PageRange pageRange = new PageRange(start, end);
					if (!lastPageRange.equals(pageRange)) {
						writtenPagesList.add(pageRange);
					}
				}
			} else {
				writtenPagesList.add(PageRange.ALL_PAGES);
			}

			PageRange[] writtenPages = new PageRange[writtenPagesList.size()];
			writtenPagesList.toArray(writtenPages);
			return writtenPages;
		}
	}

	void print() {
		// Just live uncommented preferred printing option pdf or html
//		printViaPdf();		// for study the possibilities
		printViaHtml();
	}

	@SuppressLint("NewApi")
	void printViaPdf() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { // use Android Print Framework
			PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);

			// Get a print adapter instance
			PrintDocumentAdapter printAdapter = new RouteInfoPrintDocumentAdapter(
					((OsmandApplication) getApplication()).getRoutingHelper()
							.getRouteDirections(),
					helper.getGeneralRouteInformation());

			// Create a print job with name and adapter instance
			String jobName = "OsmAnd route info";
			printManager.print(jobName, printAdapter,
					new PrintAttributes.Builder().build());
		} else { // just open pdf
			File file = generateRouteInfoPdf(
					((OsmandApplication) getApplication()).getRoutingHelper()
							.getRouteDirections(),
					helper.getGeneralRouteInformation());
			if (file.exists()) {
				Uri uri = Uri.fromFile(file);
				Intent intent;
				intent = new Intent(Intent.ACTION_VIEW).setDataAndType(
						uri, "application/pdf");
				startActivity(intent);
			}
		}
	}

	@SuppressLint("NewApi")
	void printViaHtml() {
		File file = generateRouteInfoHtml(
				((OsmandApplication) getApplication()).getRoutingHelper()
						.getRouteDirections(),
				helper.getGeneralRouteInformation());
		if (file.exists()) {
			Uri uri = Uri.fromFile(file);
			Intent browserIntent;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { // use Android Print Framework
				browserIntent = new Intent(this, PrintDialogActivity.class)
						.setDataAndType(uri, "text/html");
			} else { // just open html document
				browserIntent = new Intent(Intent.ACTION_VIEW).setDataAndType(
						uri, "text/html");
			}
			startActivity(browserIntent);
		}
	}

	private File generateRouteInfoHtml(List<RouteDirectionInfo> list, String title) {
		final String FILE_NAME = "route_info.html";
		File file = null;

		StringBuilder html = new StringBuilder();
		html.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
		html.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">");
		html.append("<head>");
		html.append("<title>Route info</title>");
		html.append("<meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\" />");
		html.append("</head>");
		html.append("<body>");

		FileOutputStream fos = null;
		try {
			if (!TextUtils.isEmpty(title)) {
				html.append("<p>");
				html.append(title);
				html.append("</p>");
			}
			final String NBSP = "&nbsp;";
			for (RouteDirectionInfo routeDirectionInfo : list) {
				html.append("<p>");
		        String distance = OsmAndFormatter.getFormattedDistance(routeDirectionInfo.distance, getMyApplication());
				html.append(distance);
				html.append(NBSP);
		        String description = routeDirectionInfo.getDescriptionRoute(((OsmandApplication) getApplication()));
		        html.append(description);
		        html.append(NBSP);
				String timeText = getTimeDescription(routeDirectionInfo);
		        html.append(timeText);
				html.append("</p>");
			}
			html.append("</body>");
			html.append("</html>");

			file = new File(((OsmandApplication) getApplication())
					.getAppCustomization().getExternalStorageDir(),
					IndexConstants.APP_DIR + FILE_NAME);
			fos = new FileOutputStream(file);
			fos.write(html.toString().getBytes("UTF-8"));
			fos.flush();
		} catch (IOException e) {
			file = null;
			e.printStackTrace();
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (Exception e) {
					file = null;
					e.printStackTrace();
				}
			}
		}

		return file;
	}

	private File generateRouteInfoPdf(List<RouteDirectionInfo> list, String title) {
		final String FILE_NAME = "route_info.pdf";
		File file = new File(((OsmandApplication) getApplication())
				.getAppCustomization().getExternalStorageDir(),
				IndexConstants.APP_DIR + FILE_NAME);

		PDF pdf = null;
		InputStream fontInputStream = null;
		try {
			pdf = new PDF(new BufferedOutputStream(new FileOutputStream(file)));
			fontInputStream = getAssets().open("fonts/Roboto-Regular.ttf");
			Font font = new Font(pdf, fontInputStream, CodePage.UNICODE, true);
			font.setSize(PDF_FONT_SIZE);
			Page page = new Page(pdf, A4.PORTRAIT);
			page.setTextStart();
			page.setTextFont(font);
			page.setTextLocation(PDF_DERAULT_MARGINS, PDF_DERAULT_MARGINS);
			page.setTextLeading(PDF_TEXT_LEADING);
			float height = page.getHeight() - 2 * PDF_DERAULT_MARGINS;
			int linesPerPage = (int)(height / PDF_FONT_SIZE + PDF_TEXT_LEADING);
			int lines = 0;
			if (!TextUtils.isEmpty(title)) {
				page.println(title);
				lines++;
			}
			for (RouteDirectionInfo routeDirectionInfo : list) {
				if (lines > linesPerPage) {
					page.setTextEnd();
					page = new Page(pdf, A4.PORTRAIT);
					page.setTextStart();
					page.setTextFont(font);
					page.setTextLocation(PDF_DERAULT_MARGINS, PDF_DERAULT_MARGINS);
					page.setTextLeading(PDF_TEXT_LEADING);
					lines = 0;
				}
				String distance = OsmAndFormatter.getFormattedDistance(
						routeDirectionInfo.distance, getMyApplication());
				String description = routeDirectionInfo
						.getDescriptionRoute(((OsmandApplication) getApplication()));
				String timeText = getTimeDescription(routeDirectionInfo);
	            page.println(distance + " " + description + " " + timeText);

				lines++;
			}
			page.setTextEnd();
		} catch (Exception e) {
			file = null;
		} finally {
			if (pdf != null) {
				try {
					pdf.close();
				} catch (Exception e) {
					file = null;
					e.printStackTrace();
				}
			}
			if (fontInputStream != null) {
				try {
					fontInputStream.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return file;
	}

}

