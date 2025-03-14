package net.osmand.data;

import java.util.ArrayList;
import java.util.List;




public class QuadTree<T> {
	
	private static class Node<T> {
		List<T> data = null;
		Node<T>[] children = null;
		QuadRect bounds;

		@SuppressWarnings("unchecked")
		private Node(QuadRect b) {
			bounds = new QuadRect(b.left, b.top, b.right, b.bottom);
			children = new Node[4];
		}
	}

    private float ratio;
	private int maxDepth;
	private Node<T> root;

	public QuadTree(QuadRect r, int depth/* =8 */, float ratio /* = 0.55 */) {
		this.ratio = ratio;
		this.root = new Node<T>(r);
		this.maxDepth = depth;
	}

	public void insert(T data, QuadRect box) {
		int depth = 0;
		doInsertData(data, box, root, depth);
	}
	
	public void clear() {
		clear(root);
	}
	
	private void clear(Node<T> rt) {
		if(rt != null ){
			if(rt.data != null) {
				rt.data.clear();
			}
			if(rt.children != null) {
				for(Node<T> c : rt.children) {
					clear(c);
				}
			}
		}
	}

	public void insert(T data, float x, float y) {
		insert(data, new QuadRect(x, y, x, y));
	}

	public List<T> queryInBox(QuadRect box, List<T> result) {
		result.clear();
		queryNode(box, result, root);
		return result;
	}

	private void queryNode(QuadRect box, List<T> result, Node<T> node) {
		if (node != null) {
			if (QuadRect.intersects(box, node.bounds)) {
				if (node.data != null) {
					result.addAll(node.data);
				}
				for (int k = 0; k < 4; ++k) {
					queryNode(box, result, node.children[k]);
				}
			}
		}
	}

	private void doInsertData(T data, QuadRect box, Node<T> n, int depth) {
		if (++depth >= maxDepth) {
			if (n.data == null) {
				n.data = new ArrayList<T>();
			}
			n.data.add(data);
		} else {
			QuadRect[] ext = new QuadRect[4];
			splitBox(n.bounds, ext);
			for (int i = 0; i < 4; ++i) {
				if (ext[i].contains(box)) {
					if (n.children[i] == null) {
						n.children[i] = new Node<T>(ext[i]);
					}
					doInsertData(data, box, n.children[i], depth);
					return;
				}
			}
			if (n.data == null) {
				n.data = new ArrayList<T>();
			}
			n.data.add(data);
		}
	}

	void splitBox(QuadRect node_extent, QuadRect[] n) {
		// coord2d c=node_extent.center();


		double lx = node_extent.left;
		double ly = node_extent.top;
		double hx = node_extent.right;
		double hy = node_extent.bottom;

		n[0] = new QuadRect(lx, ly, lx + (hx - lx) * ratio, ly + (hy - ly) * ratio);
		n[1] = new QuadRect(lx + (hx - lx) * (1 - ratio), ly, hx, ly + (hy - ly) * ratio);
		n[2] = new QuadRect(lx, ly + (hy - ly) * (1 - ratio), lx + (hx - lx) * ratio, hy);
		n[3] = new QuadRect(lx + (hx - lx) * (1 - ratio), ly + (hy - ly) * (1 - ratio), hx, hy);
	}

	public interface QuadTreeItemInQuadRect<T> {
		public boolean contains(QuadRect rect, T item);
	}

	public boolean checkIntersection(QuadRect bbox, int hintDepth, QuadTreeItemInQuadRect<T> contains) {
		// Adjust hintDepth if it exceeds the tree's maxDepth
		if (hintDepth != -1 && hintDepth > maxDepth) {
			hintDepth = -1;
		}
		return checkIntersectionRecursive(bbox, root, 0, hintDepth, contains);
	}

	private boolean checkIntersectionRecursive(QuadRect bbox, Node<T> node, int currentDepth, int targetDepth,
											   QuadTreeItemInQuadRect<T> contains) {
		if (node == null || !QuadRect.intersects(bbox, node.bounds)) {
			return false;
		}
		if (targetDepth != -1) {
			// Check for intersection at the specified depth
			if (currentDepth == targetDepth) {
				return true; // Node's bounds intersect at target depth
			} else if (currentDepth > targetDepth) {
				return false; // Current depth exceeds target (adjusted) depth
			}
		}
		// Precise check: look for data intersecting the bbox
		if (node.data != null) {
			for (T item : node.data) {
				if (contains.contains(bbox, item)) {
					return true;
				}
			}
		}
		// Recurse into children to check all relevant nodes
		for (int i = 0; i < 4; i++) {
			if (checkIntersectionRecursive(bbox, node.children[i], currentDepth + 1, targetDepth, contains)) {
				return true;
			}
		}
		return false;
	}

}
