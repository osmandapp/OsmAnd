package net.osmand;

// simple exact TSP solver based on branch-and-bound/Held--Karp
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import net.osmand.data.LatLon;
import net.osmand.util.MapUtils;

// http://stackoverflow.com/questions/7159259/optimized-tsp-algorithms
public class TspHeldKarp {
  // number of cities
  private int n;
  // cost matrix
  private double[][] cost;
  private int[] order;
  // matrix of adjusted costs
  private double[][] costWithPi;
  Node bestNode = new Node();
  

  /// OSMAND Modification
	public TspHeldKarp readInput(List<LatLon> ls, boolean returnToInitialPoint) {
		n = ls.size();
		order = new int[n];
		cost = new double[n][n];
		// TSPLIB distances are rounded to the nearest integer to avoid the sum of square roots problem
		System.out.println("Cost");
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				if (!returnToInitialPoint && (j == 0)) {
					cost[i][j] = 0;
				} else {
					cost[i][j] = Math.rint(MapUtils.getDistance(ls.get(i), ls.get(j)));
				}
			}
			System.out.println(Arrays.toString(cost[i]));
		}
		
		return this;
	}
  //
  

	public int[] solve() {
		bestNode.lowerBound = Double.MAX_VALUE;
		Node currentNode = new Node();
		currentNode.excluded = new boolean[n][n];
		costWithPi = new double[n][n];
		computeHeldKarp(currentNode);
		PriorityQueue<Node> pq = new PriorityQueue<Node>(11, new NodeComparator());
		do {
			do {
				boolean isTour = true;
				int i = -1;
				for (int j = 0; j < n; j++) {
					if (currentNode.degree[j] > 2 && (i < 0 || currentNode.degree[j] < currentNode.degree[i]))
						i = j;
				}
				if (i < 0) {
					if (currentNode.lowerBound < bestNode.lowerBound) {
						bestNode = currentNode;
						System.err.printf("%.0f", bestNode.lowerBound);
					}
					break;
				}
				System.err.print(".");
				PriorityQueue<Node> children = new PriorityQueue<Node>(11, new NodeComparator());
				children.add(exclude(currentNode, i, currentNode.parent[i]));
				for (int j = 0; j < n; j++) {
					if (currentNode.parent[j] == i)
						children.add(exclude(currentNode, i, j));
				}
				currentNode = children.poll();
				pq.addAll(children);
			} while (currentNode.lowerBound < bestNode.lowerBound);
			System.err.printf("%n");
			currentNode = pq.poll();
		} while (currentNode != null && currentNode.lowerBound < bestNode.lowerBound);
		// output suitable for gnuplot
		// set style data vector
		int j = 0;
		int k = 0;
		do {
			int i = bestNode.parent[j];
			order[k++] = j;
			System.out.printf("%f\t\n", cost[j][i]);
			j = i;
		} while (j != 0);
		return order;
	}

  private Node exclude(Node node, int i, int j) {
    Node child = new Node();
    child.excluded = node.excluded.clone();
    child.excluded[i] = node.excluded[i].clone();
    child.excluded[j] = node.excluded[j].clone();
    child.excluded[i][j] = true;
    child.excluded[j][i] = true;
    computeHeldKarp(child);
    return child;
  }

  private void computeHeldKarp(Node node) {
    node.pi = new double[n];
    node.lowerBound = Double.MIN_VALUE;
    node.degree = new int[n];
    node.parent = new int[n];
    double lambda = 0.1;
    while (lambda > 1e-06) {
      double previousLowerBound = node.lowerBound;
      computeOneTree(node);
      if (!(node.lowerBound < bestNode.lowerBound)) return;
      if (!(node.lowerBound < previousLowerBound)) lambda *= 0.9;
      int denom = 0;
      for (int i = 1; i < n; i++) {
        int d = node.degree[i] - 2;
        denom += d * d;
      }
      if (denom == 0) return;
      double t = lambda * node.lowerBound / denom;
      for (int i = 1; i < n; i++) node.pi[i] += t * (node.degree[i] - 2);
    }
  }

  private void computeOneTree(Node node) {
    // compute adjusted costs
    node.lowerBound = 0.0;
    Arrays.fill(node.degree, 0);
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) costWithPi[i][j] = node.excluded[i][j] ? Double.MAX_VALUE : cost[i][j] + node.pi[i] + node.pi[j];
    }
    int firstNeighbor;
    int secondNeighbor;
    // find the two cheapest edges from 0
    if (costWithPi[0][2] < costWithPi[0][1]) {
      firstNeighbor = 2;
      secondNeighbor = 1;
    } else {
      firstNeighbor = 1;
      secondNeighbor = 2;
    }
    for (int j = 3; j < n; j++) {
      if (costWithPi[0][j] < costWithPi[0][secondNeighbor]) {
        if (costWithPi[0][j] < costWithPi[0][firstNeighbor]) {
          secondNeighbor = firstNeighbor;
          firstNeighbor = j;
        } else {
          secondNeighbor = j;
        }
      }
    }
    addEdge(node, 0, firstNeighbor);
    Arrays.fill(node.parent, firstNeighbor);
    node.parent[firstNeighbor] = 0;
    // compute the minimum spanning tree on nodes 1..n-1
    double[] minCost = costWithPi[firstNeighbor].clone();
    for (int k = 2; k < n; k++) {
      int i;
      for (i = 1; i < n; i++) {
        if (node.degree[i] == 0) break;
      }
      for (int j = i + 1; j < n; j++) {
        if (node.degree[j] == 0 && minCost[j] < minCost[i]) i = j;
      }
      addEdge(node, node.parent[i], i);
      for (int j = 1; j < n; j++) {
        if (node.degree[j] == 0 && costWithPi[i][j] < minCost[j]) {
          minCost[j] = costWithPi[i][j];
          node.parent[j] = i;
        }
      }
    }
    addEdge(node, 0, secondNeighbor);
    node.parent[0] = secondNeighbor;
    node.lowerBound = Math.rint(node.lowerBound);
  }

  private void addEdge(Node node, int i, int j) {
    double q = node.lowerBound;
    node.lowerBound += costWithPi[i][j];
    node.degree[i]++;
    node.degree[j]++;
  }
}

class Node {
  public boolean[][] excluded;
  // Held--Karp solution
  public double[] pi;
  public double lowerBound;
  public int[] degree;
  public int[] parent;
}

class NodeComparator implements Comparator<Node> {
  public int compare(Node a, Node b) {
    return Double.compare(a.lowerBound, b.lowerBound);
  }
}