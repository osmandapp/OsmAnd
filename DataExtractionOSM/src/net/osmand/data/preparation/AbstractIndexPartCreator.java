package net.osmand.data.preparation;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rtree.Element;
import rtree.Pack;
import rtree.RTree;
import rtree.RTreeException;

public class AbstractIndexPartCreator {
	
	private final static Log log = LogFactory.getLog(AbstractIndexPartCreator.class);
	protected int BATCH_SIZE = 1000;
	
	protected Map<PreparedStatement, Integer> pStatements = new LinkedHashMap<PreparedStatement, Integer>();
	
	protected void closePreparedStatements(PreparedStatement... preparedStatements) throws SQLException {
		for (PreparedStatement p : preparedStatements) {
			if (p != null) {
				p.executeBatch();
				p.close();
				pStatements.remove(p);
			}
		}
	}
	
	protected void closeAllPreparedStatements() throws SQLException {
		for (PreparedStatement p : pStatements.keySet()) {
			if (pStatements.get(p) > 0) {
				p.executeBatch();
			}
			p.close();
		}
	}
	
	protected void addBatch(Map<PreparedStatement, Integer> count, PreparedStatement p) throws SQLException {
		addBatch(count, p, BATCH_SIZE, true);
	}
	
	protected void addBatch(Map<PreparedStatement, Integer> count, PreparedStatement p, boolean commit) throws SQLException{
		addBatch(count, p, BATCH_SIZE, commit);
	}
	
	protected void addBatch(Map<PreparedStatement, Integer> count, PreparedStatement p, int batchSize) throws SQLException{
		addBatch(count, p, batchSize, true);
	}
	
	protected void addBatch(Map<PreparedStatement, Integer> count, PreparedStatement p, int batchSize, boolean commit) throws SQLException{
		p.addBatch();
		if(count.get(p) >= batchSize){
			p.executeBatch();
			if(commit){
				p.getConnection().commit();
			}
			count.put(p, 0);
		} else {
			count.put(p, count.get(p) + 1);
		}
	}
	
	protected boolean nodeIsLastSubTree(RTree tree, long ptr) throws RTreeException {
		rtree.Node parent = tree.getReadNode(ptr);
		Element[] e = parent.getAllElements();
		for (int i = 0; i < parent.getTotalElements(); i++) {
			if (e[i].getElementType() != rtree.Node.LEAF_NODE) {
				return false;
			}
		}
		return true;

	}
	
	protected RTree packRtreeFile(RTree tree, String nonPackFileName, String packFileName) throws IOException {
		try {
			assert rtree.Node.MAX < 50 : "It is better for search performance"; //$NON-NLS-1$
			tree.flush();
			File file = new File(packFileName);
			if (file.exists()) {
				file.delete();
			}
			long rootIndex = tree.getFileHdr().getRootIndex();
			if (!nodeIsLastSubTree(tree, rootIndex)) {
				// there is a bug for small files in packing method
				new Pack().packTree(tree, packFileName);
				tree.getFileHdr().getFile().close();
				file = new File(nonPackFileName);
				file.delete();

				return new RTree(packFileName);
			}
		} catch (RTreeException e) {
			log.error("Error flushing", e); //$NON-NLS-1$
			throw new IOException(e);
		}
		return tree;
	}
}
