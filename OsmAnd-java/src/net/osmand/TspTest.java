package net.osmand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import net.osmand.data.LatLon;
import net.osmand.util.MapUtils;

public class TspTest {
	public static void main(String[] args) {
		ArrayList<LatLon> l = new ArrayList<LatLon>();
		LatLon start = new LatLon(52.2956, 4.95);
		LatLon farest = new LatLon(52.4556, 4.6739);
		
		l.add(new LatLon(52.33, 4.67)); // 2.
		l.add(new LatLon(52.4556, 4.6739)); // 3.
		l.add(new LatLon(52.59, 4.671)); // 4.
		l.add(new LatLon(52.608, 4.9005)); // 5.
		l.add(new LatLon(52.56, 4.9505)); // 6.
		l.add(new LatLon(52.49, 4.9705)); // 7.
		l.add(new LatLon(52.35, 4.9405)); // 8.
		ArrayList<LatLon> sh = new ArrayList<LatLon>(l);
		Collections.shuffle(sh);
		
		int[] mixedOrder = new int[l.size()];
		System.out.print("[");
		for (int i = 0; i < sh.size(); i++) {
			for (int j = 0; j < l.size(); j++) {
				if (l.get(j) == sh.get(i)) {
					mixedOrder[i] = j;
					System.out.print(j + ", ");
					break;
				}
			}
		}
		System.out.println("] ");
//		ans = new TspHeldKarp().readInput(sh, true).solve();
		LatLon end = farest;
		TspAnt t = new TspAnt().readGraph(sh, start, end);
		int [] ans = t.solve();
		
		double s = 0;
		int[] order = new int[ans.length];
		double[] dist = new double[ans.length];
		order[0] = 0;
		for (int k = 1; k < ans.length; k++) {
			if(k == ans.length - 1) {
				int p = mixedOrder[ans[k - 1] - 1];
				dist[k] = MapUtils.getDistance(l.get(p), end);
				order[k] = ans[k];
			} else {
				int c = mixedOrder[ans[k] - 1];
				order[k] = c + 1;
				if (k == 1) {
					dist[k] = MapUtils.getDistance(start, l.get(c));
				} else {
					int p = mixedOrder[ans[k - 1] - 1];
					dist[k] = MapUtils.getDistance(l.get(p), l.get(c));
				}
			}
			s += dist[k];
		}
		System.out.println("Result order : " + Arrays.toString(order));
		System.out.println("Result dist : " + Arrays.toString(dist));
		System.out.println(s);
		

	}

}

