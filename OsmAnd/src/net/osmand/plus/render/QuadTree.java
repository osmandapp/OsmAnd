package net.osmand.plus.render;

import java.util.ArrayList;
import java.util.List;

import android.graphics.RectF;

public class QuadTree<T> {
	private static class Node<T> {
		List<T> data = null;
		Node<T>[] children = null;
		RectF bounds;

		@SuppressWarnings("unchecked")
		private Node(RectF b) {
			bounds = new RectF(b);
			children = new Node[4];
		}
	};

	private float ratio;
	private int maxDepth;
	private Node<T> root;

	public QuadTree(RectF r, int depth/* =8 */, float ratio /* = 0.55 */) {
		this.ratio = ratio;
		this.root = new Node<T>(r);
		this.maxDepth = depth;
	}

	void insert(T data, RectF box) {
		int depth = 0;
		doInsertData(data, box, root, depth);
	}

	void queryInBox(RectF box, List<T> result) {
		result.clear();
		queryNode(box, result, root);
	}

	private void queryNode(RectF box, List<T> result, Node<T> node) {
		if (node != null) {
			if (RectF.intersects(box, node.bounds)) {
				if (node.data != null) {
					result.addAll(node.data);
				}
				for (int k = 0; k < 4; ++k) {
					queryNode(box, result, node.children[k]);
				}
			}
		}
	}

	private void doInsertData(T data, RectF box, Node<T> n, int depth) {
		if (++depth >= maxDepth) {
			if (n.data == null) {
				n.data = new ArrayList<T>();
			}
			n.data.add(data);
		} else {
			RectF[] ext = new RectF[4];
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

	void splitBox(RectF node_extent, RectF[] n) {
		// coord2d c=node_extent.center();

		float width = node_extent.width();
		float height = node_extent.height();

		float lox = node_extent.left;
		float loy = node_extent.top;
		float hix = node_extent.right;
		float hiy = node_extent.bottom;

		n[0] = new RectF(lox, loy, lox + width * ratio, loy + height * ratio);
		n[1] = new RectF(hix - width * ratio, loy, hix, loy + height * ratio);
		n[2] = new RectF(lox, hiy - height * ratio, lox + width * ratio, hiy);
		n[3] = new RectF(hix - width * ratio, hiy - height * ratio, hix, hiy);
	}

}
