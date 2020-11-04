package net.osmand;

import net.osmand.data.LatLon;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
/*
 *  === Implementation of ant swarm TSP solver. ===
 *  
 * The algorithm is described in [1, page 8].
 * 
 * == Tweaks/notes == 
 *  - I added a system where the ant chooses with probability
 *    "pr" to go to a purely random town. This did not yield better
 * results so I left "pr" fairly low.
 *  - Used an approximate pow function - the speedup is
 *    more than a factor of 10! And accuracy is not needed
 *    See AntTsp.pow for details.
 *  
 * == Parameters ==
 * I set the parameters to values suggested in [1]. My own experimentation
 * showed that they are pretty good.
 * 
 * == Usage ==
 * - Compile: javac AntTsp.java
 * - Run: java AntTsp <TSP file>
 * 
 * == TSP file format ==
 * Full adjacency matrix. Columns separated by spaces, rows by newline.
 * Weights parsed as doubles, must be >= 0.
 * 
 * == References == 
 * [1] M. Dorigo, The Ant System: Optimization by a colony of cooperating agents
 * ftp://iridia.ulb.ac.be/pub/mdorigo/journals/IJ.10-SMC96.pdf
 * 
 */

// https://github.com/lukedodd/ant-tsp
public class TspAnt {
    // Algorithm parameters:
    // original amount of trail
    private double c = 1.0;
    // trail preference
    private double alpha = 1;
    // greedy preference
    private double beta = 5;
    // trail evaporation coefficient
    private double evaporation = 0.5;
    // new trail deposit coefficient;
    private double Q = 500;
    // number of ants used = numAntFactor*numTowns
    private double numAntFactor = 0.8;
    // probability of pure random selection of the next town
    private double pr = 0.01;

    // Reasonable number of iterations
    // - results typically settle down by 500
    private int maxIterations = 2000;

    public int n = 0; // # towns
    public int m = 0; // # ants
    private double graph[][] = null;
    private double trails[][] = null;
    private Ant ants[] = null;
    private Random rand = new Random();
    private double probs[] = null;

    private int currentIndex = 0;

    public int[] bestTour;
    public double bestTourLength;

    // Ant class. Maintains tour and tabu information.
    private class Ant {
        public int tour[] = new int[graph.length];
        // Maintain visited list for towns, much faster
        // than checking if in tour so far.
        public boolean visited[] = new boolean[graph.length];

        public void visitTown(int town) {
            tour[currentIndex + 1] = town;
            visited[town] = true;
        }

        public boolean visited(int i) {
            return visited[i];
        }

        public double tourLength() {
            double length = graph[tour[n - 1]][tour[0]];
            for (int i = 0; i < n - 1; i++) {
                length += graph[tour[i]][tour[i + 1]];
            }
            return length;
        }

        public void clear() {
            for (int i = 0; i < n; i++)
                visited[i] = false;
        }
    }

    // Read in graph from a file.
    // Allocates all memory.
    // Adds 1 to edge lengths to ensure no zero length edges.
    public TspAnt readGraph(List<LatLon> intermediates, LatLon start, LatLon end) {
		boolean keepEndPoint = end != null;
		List<LatLon> l = new ArrayList<LatLon>();
		if (start != null) {
			l.add(start);
		}
    	l.addAll(intermediates);
        if (keepEndPoint) {
            l.add(end);
        }
        n = l.size() ;
//        System.out.println("Cost");
        graph = new double[n][n];
        double maxSum = 0;
		for (int i = 0; i < n ; i++) {
			double maxIWeight = 0;
			for (int j = 1; j < n ; j++) {
				double d = Math.rint(MapUtils.getDistance(l.get(i), l.get(j))) + 0.1;
				maxIWeight = Math.max(d, maxIWeight);
				graph[i][j] = d;
			}
			maxSum += maxIWeight;
		}
		maxSum = Math.rint(maxSum) + 1;
		for (int i = 0; i < n; i++) {
			if (keepEndPoint && i == n - 1) {
				graph[i][0] = 0.1;
			} else {
				graph[i][0] = maxSum;
			}
//			System.out.println(Arrays.toString(graph[i]));
		}
		
        m = (int) (n * numAntFactor);
        // all memory allocations done here
        trails = new double[n][n];
        probs = new double[n];
        ants = new Ant[m];
        for (int j = 0; j < m; j++)
            ants[j] = new Ant();
        return this;
    }

    // Approximate power function, Math.pow is quite slow and we don't need accuracy.
    // See: 
    // http://martin.ankerl.com/2007/10/04/optimized-pow-approximation-for-java-and-c-c/
    // Important facts:
    // - >25 times faster
    // - Extreme cases can lead to error of 25% - but usually less.
    // - Does not harm results -- not surprising for a stochastic algorithm.
    public static double pow(final double a, final double b) {
        final int x = (int) (Double.doubleToLongBits(a) >> 32);
        final int y = (int) (b * (x - 1072632447) + 1072632447);
        return Double.longBitsToDouble(((long) y) << 32);
    }

    // Store in probs array the probability of moving to each town
    // [1] describes how these are calculated.
    // In short: ants like to follow stronger and shorter trails more.
    private void probTo(Ant ant) {
        int i = ant.tour[currentIndex];

        double denom = 0.0;
        for (int l = 0; l < n; l++)
            if (!ant.visited(l))
                denom += pow(trails[i][l], alpha)
                        * pow(1.0 / graph[i][l], beta);


        for (int j = 0; j < n; j++) {
            if (ant.visited(j)) {
                probs[j] = 0.0;
            } else {
                double numerator = pow(trails[i][j], alpha)
                        * pow(1.0 / graph[i][j], beta);
                probs[j] = numerator / denom;
            }
        }

    }

    // Given an ant select the next town based on the probabilities
    // we assign to each town. With pr probability chooses
    // totally randomly (taking into account tabu list).
    private int selectNextTown(Ant ant) {
        // sometimes just randomly select
        if (rand.nextDouble() < pr) {
            int t = rand.nextInt(n - currentIndex); // random town
            int j = -1;
            for (int i = 0; i < n; i++) {
                if (!ant.visited(i))
                    j++;
                if (j == t)
                    return i;
            }

        }
        // calculate probabilities for each town (stored in probs)
        probTo(ant);
        // randomly select according to probs
        double r = rand.nextDouble();
        double tot = 0;
        for (int i = 0; i < n; i++) {
            tot += probs[i];
            if (tot >= r)
                return i;
        }

        throw new RuntimeException("Not supposed to get here.");
    }

    // Update trails based on ants tours
    private void updateTrails() {
        // evaporation
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                trails[i][j] *= evaporation;

        // each ants contribution
        for (Ant a : ants) {
            double contribution = Q / a.tourLength();
            for (int i = 0; i < n - 1; i++) {
                trails[a.tour[i]][a.tour[i + 1]] += contribution;
            }
            trails[a.tour[n - 1]][a.tour[0]] += contribution;
        }
    }

    // Choose the next town for all ants
    private void moveAnts() {
        // each ant follows trails...
        while (currentIndex < n - 1) {
            for (Ant a : ants)
                a.visitTown(selectNextTown(a));
            currentIndex++;
        }
    }

    // m ants with random start city
    private void setupAnts() {
        currentIndex = -1;
        for (int i = 0; i < m; i++) {
            ants[i].clear(); // faster than fresh allocations.
            ants[i].visitTown(rand.nextInt(n));
        }
        currentIndex++;

    }

    private void updateBest() {
        if (bestTour == null) {
            bestTour = ants[0].tour;
            bestTourLength = ants[0].tourLength();
        }
        for (Ant a : ants) {
            if (a.tourLength() < bestTourLength) {
                bestTourLength = a.tourLength();
                bestTour = a.tour.clone();
            }
        }
    }

    public static String tourToString(int tour[]) {
        String t = "";
        for (int i : tour)
            t = t + " " + i;
        return t;
    }

    public int[] solve() {
        // clear trails
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                trails[i][j] = c;

        int iteration = 0;
        // run for maxIterations
        // preserve best tour
        while (iteration < maxIterations) {
            setupAnts();
            moveAnts();
            updateTrails();
            updateBest();
            iteration++;
        }
        // Subtract n because we added one to edges on load
        System.out.println("Best tour length: " + (bestTourLength - n*0.1));
        System.out.println("Best tour:" + tourToString(bestTour));
        return alignAnswer(bestTour.clone());
    }
    
    private static int[] alignAnswer(int[] ans) {
		int[] alignAns = new int[ans.length];
		int shift = 0;
		for(int j = 0; j < ans.length; j++) {
			if(ans[j] == 0) {
				shift = j;
				break;
			}
		}
		for (int j = 0; j < ans.length; j++) {
			alignAns[(j - shift + ans.length) % ans.length] = ans[j];
		}
		return alignAns;
	}

    // Load graph file given on args[0].
    // (Full adjacency matrix. Columns separated by spaces, rows by newlines.)
    // Solve the TSP repeatedly for maxIterations
    // printing best tour so far each time. 
    public static void main(String[] args) {
        // Load in TSP data file.
        if (args.length < 1) {
            System.err.println("Please specify a TSP data file.");
            return;
        }
        TspAnt anttsp = new TspAnt();

        // Repeatedly solve - will keep the best tour found.
        for (; ; ) {
            anttsp.solve();
        }

    }
}