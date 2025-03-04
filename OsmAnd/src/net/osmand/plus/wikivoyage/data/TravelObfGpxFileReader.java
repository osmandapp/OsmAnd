package net.osmand.plus.wikivoyage.data;

import static net.osmand.data.Amenity.REF;
import static net.osmand.data.Amenity.ROUTE_ID;
import static net.osmand.osm.MapPoiTypes.ROUTES_PREFIX;
import static net.osmand.osm.MapPoiTypes.ROUTE_TRACK_POINT;
import static net.osmand.plus.wikivoyage.data.TravelGpx.ELE_GRAPH;
import static net.osmand.plus.wikivoyage.data.TravelGpx.ROUTE_ACTIVITY_TYPE;
import static net.osmand.plus.wikivoyage.data.TravelGpx.ROUTE_TYPE;
import static net.osmand.plus.wikivoyage.data.TravelGpx.START_ELEVATION;
import static net.osmand.plus.wikivoyage.data.TravelObfHelper.EXTENSIONS_EXTRA_TAGS;
import static net.osmand.plus.wikivoyage.data.TravelObfHelper.METADATA_EXTRA_TAGS;
import static net.osmand.plus.wikivoyage.data.TravelObfHelper.TAG_URL;
import static net.osmand.plus.wikivoyage.data.TravelObfHelper.TAG_URL_TEXT;
import static net.osmand.plus.wikivoyage.data.TravelObfHelper.getSearchFilter;
import static net.osmand.shared.gpx.GpxUtilities.PointsGroup.OBF_POINTS_GROUPS_BACKGROUNDS;
import static net.osmand.shared.gpx.GpxUtilities.PointsGroup.OBF_POINTS_GROUPS_COLORS;
import static net.osmand.shared.gpx.GpxUtilities.PointsGroup.OBF_POINTS_GROUPS_DELIMITER;
import static net.osmand.shared.gpx.GpxUtilities.PointsGroup.OBF_POINTS_GROUPS_EMPTY_NAME_STUB;
import static net.osmand.shared.gpx.GpxUtilities.PointsGroup.OBF_POINTS_GROUPS_ICONS;
import static net.osmand.shared.gpx.GpxUtilities.PointsGroup.OBF_POINTS_GROUPS_NAMES;
import static net.osmand.shared.gpx.GpxUtilities.PointsGroup.OBF_POINTS_GROUPS_PREFIX;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.HeightDataLoader;
import net.osmand.data.Amenity;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseLoadAsyncTask;
import net.osmand.plus.resources.AmenityIndexRepository;
import net.osmand.plus.utils.FileUtils;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxUtilities;
import net.osmand.shared.gpx.RouteActivityHelper;
import net.osmand.shared.gpx.primitives.Link;
import net.osmand.shared.gpx.primitives.Track;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.shared.util.KAlgorithms;
import net.osmand.shared.util.KMapAlgorithms;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

public class TravelObfGpxFileReader extends BaseLoadAsyncTask<Void, Void, GpxFile> {
    private static final Log LOG = PlatformUtil.getLog(TravelObfGpxFileReader.class);

    // Do not clutter GPX with tags that are always generated.
    private static final Set<String> doNotSaveAmenityGpxTags = Set.of(
            "date", "distance", "route_name", "route_bbox_radius",
            "avg_ele", "min_ele", "max_ele", "start_ele", "ele_graph", "diff_ele_up", "diff_ele_down",
            "avg_speed", "min_speed", "max_speed", "time_moving", "time_moving_no_gaps", "time_span", "time_span_no_gaps"
    );

    private final TravelArticle article;
    private final TravelHelper.GpxReadCallback callback;
    private final List<AmenityIndexRepository> repos;

    public TravelObfGpxFileReader(@NonNull MapActivity mapActivity,
                                  @NonNull TravelArticle article,
                                  @Nullable TravelHelper.GpxReadCallback callback,
                                  @NonNull List<AmenityIndexRepository> repos) {
        super(mapActivity);
        this.article = article;
        this.callback = callback;
        this.repos = repos;
        this.setShouldShowProgress(article instanceof TravelGpx);
    }

    @Override
    protected void onPreExecute() {
        if (isShouldShowProgress()) {
            showProgress(true);
        }
        if (callback != null) {
            callback.onGpxFileReading();
        }
    }

    @Override
    protected GpxFile doInBackground(Void... voids) {
        return buildGpxFile(repos, article, this::isCancelled);
    }

    @Override
    protected void onPostExecute(@Nullable GpxFile gpxFile) {
        article.gpxFileRead = gpxFile != null;
        article.gpxFile = gpxFile;
        if (callback != null) {
            callback.onGpxFileRead(gpxFile);
        }
        hideProgress();
    }

    @Nullable
    private synchronized GpxFile buildGpxFile(@NonNull List<AmenityIndexRepository> repos,
                                              @NonNull TravelArticle article,
                                              @NonNull HeightDataLoader.Cancellable isCancelled) {
        List<BinaryMapDataObject> segmentList = new ArrayList<>();
        Map<String, String> gpxFileExtensions = new TreeMap<>();
        List<Amenity> pointList = new ArrayList<>();
        List<String> pgNames = new ArrayList<>();
        List<String> pgIcons = new ArrayList<>();
        List<String> pgColors = new ArrayList<>();
        List<String> pgBackgrounds = new ArrayList<>();

        boolean loaded = fetchSegmentsAndPoints(repos, article, segmentList, pointList, gpxFileExtensions,
                pgNames, pgIcons, pgColors, pgBackgrounds, isCancelled);

        if (!loaded || isCancelled.isCancelled()) {
            return null;
        }

        GpxFile gpxFile;
        if (article instanceof TravelGpx) {
            gpxFile = new GpxFile(Version.getFullVersion(app));
            gpxFile.getMetadata().setName(Objects.requireNonNullElse(article.title, article.routeId)); // path is name
            if (!Algorithms.isEmpty(article.title) && article.hasOsmRouteId()) {
                gpxFileExtensions.putIfAbsent("name", article.title);
            }
            if (!Algorithms.isEmpty(article.description)) {
                gpxFile.getMetadata().setDesc(article.description);
            }
        } else {
            String description = article.getDescription();
            String title = FileUtils.isValidFileName(description) ? description : article.getTitle();
            gpxFile = new GpxFile(title, article.getLang(), article.getContent());
        }

        if (gpxFileExtensions.containsKey(TAG_URL) && gpxFileExtensions.containsKey(TAG_URL_TEXT)) {
            gpxFile.getMetadata().setLink(new Link(gpxFileExtensions.get(TAG_URL), gpxFileExtensions.get(TAG_URL_TEXT)));
            gpxFileExtensions.remove(TAG_URL_TEXT);
            gpxFileExtensions.remove(TAG_URL);
        } else if (gpxFileExtensions.containsKey(TAG_URL)) {
            gpxFile.getMetadata().setLink(new Link(gpxFileExtensions.get(TAG_URL)));
            gpxFileExtensions.remove(TAG_URL);
        }

        if (!Algorithms.isEmpty(article.getImageTitle())) {
            gpxFile.getMetadata().setLink(new Link(TravelArticle.getImageUrl(article.getImageTitle(), false)));
        }

        if (!segmentList.isEmpty()) {
            boolean hasAltitude = false;
            Track track = new Track();
            for (BinaryMapDataObject segment : segmentList) {
                TrkSegment trkSegment = new TrkSegment();
                for (int i = 0; i < segment.getPointsLength(); i++) {
                    WptPt point = new WptPt();
                    point.setLat(MapUtils.get31LatitudeY(segment.getPoint31YTile(i)));
                    point.setLon(MapUtils.get31LongitudeX(segment.getPoint31XTile(i)));
                    trkSegment.getPoints().add(point);
                }
                String ele_graph = segment.getTagValue(ELE_GRAPH);
                if (!Algorithms.isEmpty(ele_graph)) {
                    hasAltitude = true;
                    List<Integer> heightRes = KMapAlgorithms.INSTANCE.decodeIntHeightArrayGraph(ele_graph, 3);
                    double startEle = Algorithms.parseDoubleSilently(segment.getTagValue(START_ELEVATION), 0);
                    KMapAlgorithms.INSTANCE.augmentTrkSegmentWithAltitudes(trkSegment, heightRes, startEle);
                }
                track.getSegments().add(trkSegment);
            }
            gpxFile.setTracks(new ArrayList<>());
            gpxFile.getTracks().add(track);
            if (!(article instanceof TravelGpx)) {
                gpxFile.setRef(article.ref);
            }
            gpxFile.setHasAltitude(hasAltitude);
            if (gpxFileExtensions.containsKey(GpxUtilities.ACTIVITY_TYPE)) {
                final String activityType =  gpxFileExtensions.get(GpxUtilities.ACTIVITY_TYPE);
                gpxFile.getMetadata().getExtensionsToWrite().put(GpxUtilities.ACTIVITY_TYPE, activityType);

                // cleanup type and activity tags
                gpxFileExtensions.remove(ROUTE_TYPE);
                gpxFileExtensions.remove(ROUTE_ACTIVITY_TYPE);
                gpxFileExtensions.remove(ROUTE_ACTIVITY_TYPE + "_" + activityType);
                gpxFileExtensions.remove(GpxUtilities.ACTIVITY_TYPE); // moved to the metadata
            }

            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            if (gpxFileExtensions.containsKey(EXTENSIONS_EXTRA_TAGS)) {
                Map<String, String> jsonMap = gson.fromJson(gpxFileExtensions.get(EXTENSIONS_EXTRA_TAGS), type);
                if (jsonMap != null) {
                    gpxFile.getExtensionsToWrite().putAll(jsonMap);
                }
                gpxFileExtensions.remove(EXTENSIONS_EXTRA_TAGS);
            }
            if (gpxFileExtensions.containsKey(METADATA_EXTRA_TAGS)) {
                Map<String, String> jsonMap = gson.fromJson(gpxFileExtensions.get(METADATA_EXTRA_TAGS), type);
                if (jsonMap != null) {
                    gpxFile.getMetadata().getExtensionsToWrite().putAll(jsonMap);
                }
                gpxFileExtensions.remove(METADATA_EXTRA_TAGS);
            }

            gpxFile.getExtensionsToWrite().putAll(gpxFileExtensions); // finally
        }
        reconstructPointsGroups(gpxFile, pgNames, pgIcons, pgColors, pgBackgrounds); // create groups before points
        if (!pointList.isEmpty()) {
            for (Amenity wayPoint : pointList) {
                gpxFile.addPoint(article.createWptPt(wayPoint, article.getLang()));
            }
        }
        article.gpxFile = gpxFile;
        return gpxFile;
    }

    private boolean fetchSegmentsAndPoints(@NonNull List<AmenityIndexRepository> repos,
                                           @NonNull TravelArticle article,
                                           @NonNull List<BinaryMapDataObject> segmentList,
                                           @NonNull List<Amenity> pointList,
                                           @NonNull Map<String, String> gpxFileExtensions,
                                           @NonNull List<String> pgNames,
                                           @NonNull List<String> pgIcons,
                                           @NonNull List<String> pgColors,
                                           @NonNull List<String> pgBackgrounds,
                                           @NonNull HeightDataLoader.Cancellable isCancelled) {
        int left = 0, right = Integer.MAX_VALUE, top = 0, bottom = Integer.MAX_VALUE;

        if (article.hasBbox31()) {
            left = (int) article.getBbox31().left;
            right = (int) article.getBbox31().right;
            top = (int) article.getBbox31().top;
            bottom = (int) article.getBbox31().bottom;
        }

        boolean allowReadFromMultipleMaps = article.hasOsmRouteId();

        for (AmenityIndexRepository repo : repos) {
            try {
                if (isCancelled.isCancelled()) {
                    return false;
                }
                if (!allowReadFromMultipleMaps &&
                        !Algorithms.objectEquals(repo.getFile(), article.file)) {
                    continue; // speed up reading of Wikivoyage and User's GPX files in OBF
                }
                if (article instanceof TravelGpx) {
                    BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> sr = BinaryMapIndexReader
                            .buildSearchRequest(left, right, top, bottom, 15, null,
                                    matchSegmentsByRefTitleRouteId(article, segmentList, isCancelled));
                    if (article.routeRadius > 0 && !article.hasBbox31()) {
                        sr.setBBoxRadius(article.lat, article.lon, article.routeRadius);
                    }
                    repo.searchMapIndex(sr);
                }
                BinaryMapIndexReader.SearchRequest<Amenity> pointRequest = BinaryMapIndexReader.buildSearchPoiRequest(
                        0, 0, Algorithms.emptyIfNull(article.title), left, right, top, bottom,
                        getSearchFilter(article.getMainFilterString(), article.getPointFilterString()),
                        matchPointsAndTags(article, pointList, gpxFileExtensions, pgNames, pgIcons, pgColors, pgBackgrounds, isCancelled),
                        null);
                if (article.routeRadius > 0 && !article.hasBbox31()) {
                    pointRequest.setBBoxRadius(article.lat, article.lon, article.routeRadius);
                }
                if (!Algorithms.isEmpty(article.title)) {
                    repo.searchPoiByName(pointRequest);
                } else {
                    repo.searchPoi(pointRequest);
                }
                if (!allowReadFromMultipleMaps && !Algorithms.isEmpty(segmentList)) {
                    break; // speed up reading of User's GPX files
                }
            } catch (Exception e) {
                LOG.error(e.getMessage());
            }
        }
        return !isCancelled.isCancelled(); // might be partially succeed
    }

    @NonNull
    private ResultMatcher<Amenity> matchPointsAndTags(@NonNull TravelArticle article,
                                                      @NonNull List<Amenity> pointList,
                                                      @NonNull Map<String, String> gpxFileExtensions,
                                                      @NonNull List<String> pgNames,
                                                      @NonNull List<String> pgIcons,
                                                      @NonNull List<String> pgColors,
                                                      @NonNull List<String> pgBackgrounds,
                                                      @NonNull HeightDataLoader.Cancellable isCancelled) {
        return new ResultMatcher<Amenity>() {
            boolean isAlreadyProcessed = false;
            @Override
            public boolean publish(Amenity amenity) {
                if (amenity.getRouteId().equals(article.getRouteId())) {
                    if (amenity.isRouteTrack()) {
                        if (!isAlreadyProcessed) {
                            isAlreadyProcessed = true;
                            reconstructActivityFromAmenity(amenity, gpxFileExtensions);
                            amenity.getNamesMap(true).forEach((lang, value) ->
                                    {
                                        if (!"ref".equals(lang) && !"sym".equals(lang)) {
                                            gpxFileExtensions.put("name:" + lang, value);
                                        }
                                    }
                            );
                            for (String tag : amenity.getAdditionalInfoKeys()) {
                                String value = amenity.getAdditionalInfo(tag);
                                if (tag.startsWith(OBF_POINTS_GROUPS_PREFIX)) {
                                    List<String> values = Arrays.asList(value.split(OBF_POINTS_GROUPS_DELIMITER));
                                    switch (tag) {
                                        case OBF_POINTS_GROUPS_NAMES -> pgNames.addAll(values);
                                        case OBF_POINTS_GROUPS_ICONS -> pgIcons.addAll(values);
                                        case OBF_POINTS_GROUPS_COLORS -> pgColors.addAll(values);
                                        case OBF_POINTS_GROUPS_BACKGROUNDS -> pgBackgrounds.addAll(values);
                                    }
                                } else if (!doNotSaveAmenityGpxTags.contains(tag)) {
                                    gpxFileExtensions.put(tag, value);
                                }
                            }
                        }
                    } else if (ROUTE_TRACK_POINT.equals(amenity.getSubType())) {
                        pointList.add(amenity);
                    } else {
                        String amenityLang = amenity.getTagSuffix(Amenity.LANG_YES + ":");
                        if (Algorithms.stringsEqual(article.lang, amenityLang)) {
                            pointList.add(amenity);
                        }
                    }
                }
                return false;
            }
            @Override
            public boolean isCancelled() {
                return isCancelled.isCancelled();
            }
        };
    }

    private void reconstructActivityFromAmenity(@NonNull Amenity amenity,
                                                @NonNull Map<String, String> gpxFileExtensions) {
        if (amenity.isRouteTrack() && amenity.getSubType() != null) {
            String subType = amenity.getSubType();
            if (subType.startsWith(ROUTES_PREFIX)) {
                String osmValue = amenity.getType().getPoiTypeByKeyName(subType).getOsmValue();
                if (!Algorithms.isEmpty(osmValue)) {
                    if (amenity.hasOsmRouteId() || !"other".equals(osmValue)) {
                        gpxFileExtensions.put(ROUTE_TYPE, osmValue); // do not litter gpx with default route_type
                    }
                    RouteActivityHelper helper = app.getRouteActivityHelper();
                    for (String key : amenity.getAdditionalInfoKeys()) {
                        if (key.startsWith(ROUTE_ACTIVITY_TYPE + "_")) {
                            String activityType = amenity.getAdditionalInfo(key);
                            if (!activityType.isEmpty() && helper.findRouteActivity(activityType) != null) {
                                gpxFileExtensions.put(GpxUtilities.ACTIVITY_TYPE, activityType); // osmand:activity in gpx
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    @NonNull
    private ResultMatcher<BinaryMapDataObject> matchSegmentsByRefTitleRouteId(
            @NonNull TravelArticle article,
            @NonNull List<BinaryMapDataObject> segmentList,
            @NonNull HeightDataLoader.Cancellable isCancelled) {
        return new ResultMatcher<BinaryMapDataObject>() {
            @Override
            public boolean publish(BinaryMapDataObject object) {
                if (object.getPointsLength() > 1) {
                    String routeId = article.getRouteId();
                    boolean equalRouteId = !Algorithms.isEmpty(routeId) && routeId.equals(object.getTagValue(ROUTE_ID));

                    if (article instanceof TravelGpx && equalRouteId) {
                        segmentList.add(object); // GPX-in-OBF requires mandatory route_id
                    } else {
                        String name = article.getTitle(), ref = article.ref;
                        boolean equalName = !Algorithms.isEmpty(name) && name.equals(object.getName());
                        boolean equalRef = !Algorithms.isEmpty(ref) && ref.equals(object.getTagValue(REF));
                        if ((equalRouteId && (equalRef || equalName) || (equalRef && equalName))) {
                            segmentList.add(object); // Wikivoyage is allowed to match mixed tags
                        }
                    }
                }
                return false;
            }
            @Override
            public boolean isCancelled() {
                return isCancelled.isCancelled();
            }
        };
    }

    private void reconstructPointsGroups(@NonNull GpxFile gpxFile,
                                         @NonNull List<String> pgNames,
                                         @NonNull List<String> pgIcons,
                                         @NonNull List<String> pgColors,
                                         @NonNull List<String> pgBackgrounds) {
        if (pgNames.size() == pgIcons.size() &&
                pgIcons.size() == pgColors.size() && pgColors.size() == pgBackgrounds.size()) {
            for (int i = 0; i < pgNames.size(); i++) {
                String name = pgNames.get(i);
                String icon = pgIcons.get(i);
                String background = pgBackgrounds.get(i);
                int color = KAlgorithms.INSTANCE.parseColor(pgColors.get(i));
                if (name.isEmpty() || OBF_POINTS_GROUPS_EMPTY_NAME_STUB.equals(name)) {
                    name = GpxFile.DEFAULT_WPT_GROUP_NAME; // follow current default
                }
                GpxUtilities.PointsGroup pg = new GpxUtilities.PointsGroup(name, icon, background, color);
                gpxFile.addPointsGroup(pg);
            }
        }
    }
}
