package net.osmand.router;

import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapRouteReaderAdapter;
import net.osmand.binary.RouteDataObject;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.shared.routing.details.RouteAttributeClassification;
import net.osmand.shared.routing.details.RouteAttributeClassificationRequest;
import net.osmand.shared.routing.details.RouteAttributeClassifier;
import net.osmand.shared.routing.details.RouteSegment;
import net.osmand.shared.routing.details.RouteStatistic;
import net.osmand.shared.routing.details.RouteStatisticsCalculator;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RouteStatisticsHelper {

	private static final Log LOG = PlatformUtil.getLog(RouteStatisticsHelper.class);

	public static final String UNDEFINED_ATTR = "undefined";
	public static final String ROUTE_INFO_PREFIX = "routeInfo_";

	private static final int MIN_INCLINE = -101;
	private static final int MIN_DIVIDED_INCLINE = -20;
	private static final int MAX_INCLINE = 100;
	private static final int MAX_DIVIDED_INCLINE = 20;
	private static final int STEP = 4;
	private static final int[] BOUNDARIES_ARRAY;
	private static final String[] BOUNDARIES_CLASS;

	private static final String ROUTE_INFO_STEEPNESS = "routeInfo_steepness";

	static {
		int NUM = ((MAX_DIVIDED_INCLINE - MIN_DIVIDED_INCLINE) / STEP) + 3;
		BOUNDARIES_ARRAY = new int[NUM];
		BOUNDARIES_CLASS = new String[NUM];
		BOUNDARIES_ARRAY[0] = MIN_INCLINE;
		BOUNDARIES_CLASS[0] = "steepness=" + (MIN_INCLINE + 1) + "_" + MIN_DIVIDED_INCLINE;
		for (int i = 1; i < NUM - 1; i++) {
			BOUNDARIES_ARRAY[i] = MIN_DIVIDED_INCLINE + (i - 1) * STEP;
			BOUNDARIES_CLASS[i] = "steepness=" + (BOUNDARIES_ARRAY[i - 1] + 1) + "_" + BOUNDARIES_ARRAY[i];
		}
		BOUNDARIES_ARRAY[NUM - 1] = MAX_INCLINE;
		BOUNDARIES_CLASS[NUM - 1] = "steepness="+MAX_DIVIDED_INCLINE+"_"+MAX_INCLINE;
	}

	public static List<RouteStatistic> calculateRouteStatistic(List<RouteSegmentResult> route,
	                                                            RenderingRulesStorage currentRenderer,
	                                                            RenderingRulesStorage defaultRenderer,
	                                                            RenderingRuleSearchRequest currentSearchRequest,
	                                                            RenderingRuleSearchRequest defaultSearchRequest) {
		return calculateRouteStatistic(route, null, currentRenderer, defaultRenderer,
				currentSearchRequest, defaultSearchRequest);
	}

	public static List<RouteStatistic> calculateRouteStatistic(List<RouteSegmentResult> route,
	                                                            List<String> attributesNames,
	                                                            RenderingRulesStorage currentRenderer,
	                                                            RenderingRulesStorage defaultRenderer,
	                                                            RenderingRuleSearchRequest currentSearchRequest,
	                                                            RenderingRuleSearchRequest defaultSearchRequest) {
		if (route == null) {
			return Collections.emptyList();
		}
		if (Algorithms.isEmpty(attributesNames)) {
			attributesNames = getRouteStatisticAttrsNames(currentRenderer, defaultRenderer, false);
		}
		List<RouteSegment> sharedRoute = new ArrayList<>(route.size());
		for (int i = 0; i < route.size(); i++) {
			// Statistics use segment values only; these synthetic ranges satisfy the shared contract
			// without reconstructing RouteCalculationResult's point-aligned segment list here.
			sharedRoute.add(RouteSegmentResultSnapshotAdapter.toStatisticsSnapshot(route.get(i), i));
		}
		return RouteStatisticsCalculator.INSTANCE.calculate(
				sharedRoute,
				attributesNames,
				new SharedRouteAttributeClassifier(
						currentRenderer,
						defaultRenderer,
						currentSearchRequest,
						defaultSearchRequest));
	}

	/** Keeps Android rendering objects outside common code while preserving current/default fallback. */
	private static class SharedRouteAttributeClassifier implements RouteAttributeClassifier {

		private final RenderingRulesStorage currentRenderer;
		private final RenderingRulesStorage defaultRenderer;
		private final RenderingRuleSearchRequest currentSearchRequest;
		private final RenderingRuleSearchRequest defaultSearchRequest;

		SharedRouteAttributeClassifier(RenderingRulesStorage currentRenderer,
		                               RenderingRulesStorage defaultRenderer,
		                               RenderingRuleSearchRequest currentSearchRequest,
		                               RenderingRuleSearchRequest defaultSearchRequest) {
			this.currentRenderer = currentRenderer;
			this.defaultRenderer = defaultRenderer;
			this.currentSearchRequest = currentSearchRequest;
			this.defaultSearchRequest = defaultSearchRequest;
		}

		@Override
		public RouteAttributeClassification classify(RouteAttributeClassificationRequest request) {
			RouteAttributeClassification classification = classify(
					request, currentRenderer, currentSearchRequest);
			return classification != null
					? classification
					: classify(request, defaultRenderer, defaultSearchRequest);
		}

		private RouteAttributeClassification classify(RouteAttributeClassificationRequest request,
		                                              RenderingRulesStorage renderer,
		                                              RenderingRuleSearchRequest baseSearchRequest) {
			if (renderer == null) {
				return null;
			}
			RenderingRuleSearchRequest searchRequest = new RenderingRuleSearchRequest(baseSearchRequest);
			if (request.getMainTag() != null) {
				searchRequest.setStringFilter(renderer.PROPS.R_TAG, request.getMainTag());
				searchRequest.setStringFilter(renderer.PROPS.R_VALUE, request.getMainValue());
			}
			searchRequest.setStringFilter(renderer.PROPS.R_ADDITIONAL, request.getAdditional());
			if (!searchRequest.searchRenderingAttribute(request.getAttributeName())) {
				return null;
			}
			return new RouteAttributeClassification(
					searchRequest.getStringPropertyValue(renderer.PROPS.R_ATTR_STRING_VALUE),
					searchRequest.getIntPropertyValue(renderer.PROPS.R_ATTR_COLOR_VALUE));
		}
	}

	public static List<String> getRouteStatisticAttrsNames(RenderingRulesStorage currentRenderer,
	                                                       RenderingRulesStorage defaultRenderer,
	                                                       boolean excludeSteepness) {
		List<String> attributeNames = new ArrayList<>();
		if (currentRenderer != null) {
			for (String s : currentRenderer.getRenderingAttributeNames()) {
				if (s.startsWith(ROUTE_INFO_PREFIX)) {
					attributeNames.add(s);
				}
			}
		}
		if (attributeNames.isEmpty() && defaultRenderer != null) {
			for (String s : defaultRenderer.getRenderingAttributeNames()) {
				if (s.startsWith(ROUTE_INFO_PREFIX)) {
					attributeNames.add(s);
				}
			}
		}
		if (excludeSteepness) {
			attributeNames.remove(ROUTE_INFO_STEEPNESS);
		}
		return attributeNames;
	}

	public static class RouteStatisticComputer {

		final RenderingRulesStorage currentRenderer;
		final RenderingRulesStorage defaultRenderer;
		final RenderingRuleSearchRequest currentRenderingRuleSearchRequest;
		final RenderingRuleSearchRequest defaultRenderingRuleSearchRequest;

		public RouteStatisticComputer(RenderingRulesStorage currentRenderer, RenderingRulesStorage defaultRenderer,
		                              RenderingRuleSearchRequest currentRenderingRuleSearchRequest,
		                              RenderingRuleSearchRequest defaultRenderingRuleSearchRequest) {
			this.currentRenderer = currentRenderer;
			this.defaultRenderer = defaultRenderer;
			this.currentRenderingRuleSearchRequest = currentRenderingRuleSearchRequest;
			this.defaultRenderingRuleSearchRequest = defaultRenderingRuleSearchRequest;
		}

		public RouteAttributeClassification classifySegment(String attribute, int slopeClass, RouteDataObject routeObject) {
			RouteAttributeClassification res = new RouteAttributeClassification(UNDEFINED_ATTR, 0);
			RenderingRuleSearchRequest currentRequest = 
					currentRenderer == null ? null : new RenderingRuleSearchRequest(currentRenderingRuleSearchRequest);
			if (currentRenderer != null
					&& searchRenderingAttribute(attribute, currentRenderer, currentRequest, routeObject, slopeClass)) {
				String propertyName = currentRequest.getStringPropertyValue(currentRenderer.PROPS.R_ATTR_STRING_VALUE);
				res = new RouteAttributeClassification(
						propertyName == null ? UNDEFINED_ATTR : propertyName,
						currentRequest.getIntPropertyValue(currentRenderer.PROPS.R_ATTR_COLOR_VALUE));
			} else {
				RenderingRuleSearchRequest defaultRequest = new RenderingRuleSearchRequest(defaultRenderingRuleSearchRequest);
				if (searchRenderingAttribute(attribute, defaultRenderer, defaultRequest, routeObject, slopeClass)) {
					String propertyName = defaultRequest.getStringPropertyValue(defaultRenderer.PROPS.R_ATTR_STRING_VALUE);
					res = new RouteAttributeClassification(
							propertyName == null ? UNDEFINED_ATTR : propertyName,
							defaultRequest.getIntPropertyValue(defaultRenderer.PROPS.R_ATTR_COLOR_VALUE));
				}
			}
			return res;
		}

		protected boolean searchRenderingAttribute(String attribute, RenderingRulesStorage rrs,
		                                           RenderingRuleSearchRequest req, RouteDataObject routeObject,
												   int slopeClass) {
			//String additional = attrName + "=" + attribute;
			boolean mainTagAdded = false;
			StringBuilder additional = new StringBuilder(slopeClass >= 0 ? (BOUNDARIES_CLASS[slopeClass] + ";") : "");
			int encodingRulesSize = routeObject.region.quickGetEncodingRulesSize();
			for (int type : routeObject.getTypes()) {
				if (type < 0 || type >= encodingRulesSize) {
					LOG.warn("Skipping invalid route encoding rule id=" + type
							+ " for route object id=" + routeObject.getId()
							+ ", rules=" + encodingRulesSize);
					continue;
				}
				BinaryMapRouteReaderAdapter.RouteTypeRule tp = routeObject.region.quickGetEncodingRule(type);
				if (tp.getTag().equals("highway") || tp.getTag().equals("route")
						|| tp.getTag().equals("railway") || tp.getTag().equals("aeroway")
						|| tp.getTag().equals("aerialway") || tp.getTag().equals("piste:type")) {
					if (!mainTagAdded) {
						req.setStringFilter(rrs.PROPS.R_TAG, tp.getTag());
						req.setStringFilter(rrs.PROPS.R_VALUE, tp.getValue());
						mainTagAdded = true;
					}
				} else {
					additional.append(tp.getTag()).append("=").append(tp.getValue()).append(";");
				}
			}
			req.setStringFilter(rrs.PROPS.R_ADDITIONAL, additional.toString());
			return req.searchRenderingAttribute(attribute);
		}
	}

}
