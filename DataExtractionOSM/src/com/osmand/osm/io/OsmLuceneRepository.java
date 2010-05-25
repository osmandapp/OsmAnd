package com.osmand.osm.io;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import com.osmand.IProgress;
import com.osmand.LogUtil;
import com.osmand.data.Amenity;
import com.osmand.data.Amenity.AmenityType;

public class OsmLuceneRepository {
	private static final Log log = LogUtil.getLog(OsmLuceneRepository.class);
	private static final int MAX_POI_HITS = 1000;
	private static DecimalFormat fmtLatitude = new DecimalFormat("000.000000", new DecimalFormatSymbols(Locale.US));
	private static DecimalFormat negFmtLatitude = new DecimalFormat("00.000000", new DecimalFormatSymbols(Locale.US));
	private static DecimalFormat fmtLongitude = new DecimalFormat("0000.000000", new DecimalFormatSymbols(Locale.US));
	private static DecimalFormat negFmtLongitude = new DecimalFormat("000.000000", new DecimalFormatSymbols(Locale.US));
	
	
	public static String formatLatitude(double latitude){
		if(latitude <0 ){
			return negFmtLatitude.format(latitude);
		} else {
			return fmtLatitude.format(latitude);
		}
	}
	
	public static String formatLongitude(double longitude){
		if(longitude <0 ){
			return negFmtLongitude.format(longitude);
		} else {
			return fmtLongitude.format(longitude);
		}
	}

	private List<Amenity> internalSearch(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude){
		queryFormat.setLength(0);
		queryFormat.append("latitude:[").append(formatLatitude(bottomLatitude)).append(" TO ").append(formatLatitude(topLatitude)).append(
				"]").append(" AND longitude:[").append(formatLongitude(leftLongitude)).append(" TO ").append(
				formatLongitude(rightLongitude)).append("]");

		TopScoreDocCollector collector = TopScoreDocCollector.create(MAX_POI_HITS, true);
		try {
			Query q = new QueryParser(Version.LUCENE_30, "id", new StandardAnalyzer(Version.LUCENE_30)).parse(queryFormat.toString());
			long now = System.currentTimeMillis();
			amenityIndexSearcher.search(q, collector);
			ScoreDoc[] hits = collector.topDocs().scoreDocs;

			List<Amenity> result = new ArrayList<Amenity>(hits.length);
			for (int i = 0; i < hits.length; i++) {
				result.add(convertAmenity(amenityIndexSearcher.doc(hits[i].doc)));
			}
			if (log.isDebugEnabled()) {
				log.debug(String.format("Search for %s done in %s ms found %s.", q, System.currentTimeMillis() - now, hits.length));
			}
			return result;
		} catch (IOException e) {
			log.error("Failed to search.", e);
			throw new RuntimeException(e);
		} catch (ParseException e) {
			log.error("Invalid query.", e);
			return new ArrayList<Amenity>();
		}
	}
	
	private List<Amenity> cachedAmenities = null;
	private double cTopLatitude;
	private double cBottomLatitude;
	private double cLeftLongitude;
	private double cRightLongitude;
	
	private final StringBuilder queryFormat = new StringBuilder();
	private IndexSearcher amenityIndexSearcher;
	
	private boolean isLoading = false;
	
	protected synchronized void loadAmenitiesInAnotherThread(final double topLatitude, final double leftLongitude, final double bottomLatitude, final double rightLongitude){
		isLoading = true;
		new Thread(new Runnable(){

			@Override
			public void run() {
				try {
					cachedAmenities = internalSearch(topLatitude, leftLongitude, bottomLatitude, rightLongitude);
					cTopLatitude = topLatitude;
					cLeftLongitude = leftLongitude;
					cBottomLatitude = bottomLatitude ;
					cRightLongitude = rightLongitude;
				} finally {
					synchronized (this) {
						isLoading = false;
					}
				}
			}
		}, "Searching in index...").start();
	}
	public synchronized List<Amenity> searchAmenities(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude) {
		if(amenityIndexSearcher == null){
			return Collections.emptyList();
		}
		// TODO take into account that right could be -53 & left = 175 (normalized coordinates
		if (cTopLatitude >= topLatitude && cLeftLongitude <= leftLongitude && cRightLongitude >= rightLongitude
				&& cBottomLatitude <= bottomLatitude) {
			return cachedAmenities;
		}
		if(!isLoading){
			double h = (topLatitude - bottomLatitude);
			double w = (rightLongitude - leftLongitude);
			topLatitude += h;
			leftLongitude -= w;
			bottomLatitude -= h;
			rightLongitude += w;
			loadAmenitiesInAnotherThread(topLatitude, leftLongitude, bottomLatitude, rightLongitude);
		}
		return Collections.emptyList();
	}

	public void indexing(final IProgress progress, File dir) {
		long start = System.currentTimeMillis();
		progress.startTask("Indexing lucene", -1);
		try {
			amenityIndexSearcher = new IndexSearcher(FSDirectory.open(dir));
		} catch (Exception t) {
			log.error("Failed to initialize searcher.", t);
			throw new RuntimeException(t);
		}
		if (log.isDebugEnabled()) {
			log.debug("Finished index lucene " + dir.getAbsolutePath() + " " + (System.currentTimeMillis() - start) + "ms");
		}
	}

	protected Amenity convertAmenity(Document document) {
		try {
			Amenity am = new Amenity();
			am.setName(document.get("name"));
			am.setId(Long.parseLong(document.get("id")));
			am.setSubType(document.get("subtype"));
			am.setType(AmenityType.fromString(document.get("type")));
			double longitude = fmtLongitude.parse(document.get("longitude")).doubleValue();
			double latitude = fmtLatitude.parse(document.get("latitude")).doubleValue();
			am.setLocation(latitude, longitude);
			return am;
		} catch (java.text.ParseException e) {
			return null;
		}
	}
	
	
	
}
