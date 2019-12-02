package net.osmand.router;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.PathWrapper;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.StopWatch;

import net.osmand.PlatformUtil;

import org.apache.commons.logging.Log;

import java.io.File;

public class GraphHopperRouterImpl {

	private static Log LOG = PlatformUtil.getLog(GraphHopperRouterImpl.class);

	private GraphHopper hopper;
	private GHRequest request;
	private String ghGraphPath, currentArea;

	private GraphHopperRouterImpl() {
	}

	public GraphHopperRouterImpl(GHRequest params, String ghGraphPath, String area) {
		this.request = params;
		this.ghGraphPath = ghGraphPath;
		this.currentArea = area;

		loadGHGraphs();
	}

	private void loadGHGraphs() {
		try {
			hopper = new GraphHopper().forMobile();
			hopper.load(new File(ghGraphPath, currentArea).getAbsolutePath() + "-gh");
			LOG.info("found graph " + hopper.getGraphHopperStorage().toString() + ", nodes:" + hopper.getGraphHopperStorage().getNodes());
		} catch (Exception e) {
			LOG.error("Error loading graphs! Check the path!");
		}
	}

	public PathWrapper calculateRoute() {
		if (hopper != null) {
			LOG.info("calculating path ...");
			float time;

			StopWatch sw = new StopWatch().start();

			GHResponse resp = hopper.route(request);
			time = sw.stop().getSeconds();

			if (!resp.hasErrors()) {
				LOG.info("from:" + request.getPoints().get(0).lat + "," + request.getPoints().get(0).lon + " to:" + request.getPoints().get(request.getPoints().size() - 1).lat + ","
						+ request.getPoints().get(request.getPoints().size() - 1).lon + " found path with distance:" + resp.getBest().getDistance()
						/ 1000f + ", nodes:" + resp.getBest().getPoints().getSize() + ", time:"
						+ time + " " + resp.getDebugInfo());
				LOG.info("the route is " + (int) (resp.getBest().getDistance() / 100) / 10f
						+ "km long, time:" + resp.getBest().getTime() / 60000f + "min, debug:" + time);
			}
			return resp.getBest();
		} else {
			return null;
		}
	}

	public static void main(String[] args) {
		String path = "/home/madwasp79/OsmAnd-maps/graphhopper/";
		String area = "ukrainekieveurope";
		GHRequest req = new GHRequest(50.439296, 30.478347, 50.449700, 30.478776).
				setAlgorithm(Parameters.Algorithms.DIJKSTRA_BI);
		req.getHints().
				put(Parameters.Routing.INSTRUCTIONS, "false");

		GraphHopperRouterImpl gh = new GraphHopperRouterImpl(req, path, area);
		PathWrapper pw = gh.calculateRoute();
	}
}
