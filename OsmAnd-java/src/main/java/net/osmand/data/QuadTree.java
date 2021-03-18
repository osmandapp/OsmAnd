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

}
