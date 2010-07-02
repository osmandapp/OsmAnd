package com.osmand.data.index;

public class IndexConstants {
	
	// Important : Every time you change schema of db upgrade version!!! 
	// If you want that new application support old index : put upgrade code in android app ResourceManager
	public final static int TRANSPORT_TABLE_VERSION = 0;
	public final static int POI_TABLE_VERSION = 0;
	public final static int ADDRESS_TABLE_VERSION = 1;
	
	public static final String POI_INDEX_DIR = "POI/"; //$NON-NLS-1$
	public static final String ADDRESS_INDEX_DIR = "Address/"; //$NON-NLS-1$
	public static final String TRANSPORT_INDEX_DIR = "Transport/"; //$NON-NLS-1$
	
	public static final String POI_INDEX_EXT = ".poi.odb"; //$NON-NLS-1$
	public static final String ADDRESS_INDEX_EXT = ".addr.odb"; //$NON-NLS-1$
	public static final String TRANSPORT_INDEX_EXT = ".trans.odb"; //$NON-NLS-1$
	
	public interface IndexColumn {
		public boolean isIndex();
		
		public String getType();
		
		public String getTableName();
	}
	
	public static String[] generateColumnNames(IndexColumn[] columns) {
		String[] columnNames = new String[columns.length];
		for (int i = 0; i < columnNames.length; i++) {
			columnNames[i] = columns[i].toString();
		}
		return columnNames;
	}
	
	public static String generateCreateSQL(IndexColumn[] columns){
		StringBuilder b = new StringBuilder();
		b.append("create table ").append(columns[0].getTableName()).append(" ("); //$NON-NLS-1$ //$NON-NLS-2$
		boolean first = true;
		for(IndexColumn c : columns){
			if(first) {
				first = false;
			} else {
				b.append(", "); //$NON-NLS-1$
			}
			b.append(c.toString());
			if(c.getType() != null){
				b.append(" ").append(c.getType()); //$NON-NLS-1$
			}
		}
		b.append(" ); "); //$NON-NLS-1$
		return b.toString();
	}
	
	public static String generateSelectSQL(IndexColumn[] select){
		return generateSelectSQL(select, null);
	}
	
	public static String generateSelectSQL(IndexColumn[] select, String where){
		StringBuilder b = new StringBuilder();
		b.append("select "); //$NON-NLS-1$
		boolean first = true;
		for(IndexColumn c : select){
			if(first) {
				first = false;
			} else {
				b.append(", "); //$NON-NLS-1$
			}
			b.append(c.toString());
		}
		b.append(" FROM ").append(select[0].getTableName()); //$NON-NLS-1$
		if(where != null){
			b.append(" WHERE " ).append(where); //$NON-NLS-1$
		}
		b.append(" ; "); //$NON-NLS-1$
		return b.toString();
	}
	
	public static String generatePrepareStatementToInsert(String tableName, int numColumns){
		StringBuilder b = new StringBuilder();
		b.append("insert into ").append(tableName).append(" values ("); //$NON-NLS-1$ //$NON-NLS-2$
		for(int i=0; i< numColumns; i++){
			if(i > 0){
				b.append(", "); //$NON-NLS-1$
			}
			b.append("?"); //$NON-NLS-1$
		}
		b.append(");"); //$NON-NLS-1$
		return b.toString();
	}
	
	public static String generateCreateIndexSQL(IndexColumn[] columns){
		StringBuilder b = new StringBuilder();
		String tableName = columns[0].getTableName();
		b.append("create index ").append(tableName).append("_index ON ").append(tableName).append(" ("); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		boolean first = true;
		for(IndexColumn c : columns){
			if(!c.isIndex()){
				continue;
			}
			if(first) {
				first = false;
			} else {
				b.append(", "); //$NON-NLS-1$
			}
			b.append(c.toString());
		}
		b.append(" ); "); //$NON-NLS-1$
		if(first){
			return null;
		}
		return b.toString();
	}

	// POI index
	
	public enum IndexPoiTable implements IndexColumn {
		ID("long"), LATITUDE("double", true), LONGITUDE("double", true), OPENING_HOURS, NAME, NAME_EN, TYPE, SUBTYPE; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		boolean index = false;
		String type = null;
		private IndexPoiTable(){}
		private IndexPoiTable(String type){
			this.type = type;
		}
		private IndexPoiTable(String type, boolean index){ this(type); this.index = index;}
		
		public static String getTable(){
			return "poi"; //$NON-NLS-1$
		}
		
		public String getTableName(){
			return getTable();
		}

		@Override
		public String getType() {
			return type;
		}

		@Override
		public boolean isIndex() {
			return index;
		}
	}
	
	// Address index		
		
	public enum IndexCityTable implements IndexColumn {
		ID("long"), LATITUDE("double", true), LONGITUDE("double", true), NAME, NAME_EN, CITY_TYPE; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		boolean index = false;
		String type = null;

		private IndexCityTable() {
		}

		private IndexCityTable(String type) {
			this.type = type;
		}

		private IndexCityTable(String type, boolean index) {
			this(type);
			this.index = index;
		}

		public static String getTable() {
			return "city"; //$NON-NLS-1$
		}

		public String getTableName() {
			return getTable();
		}

		@Override
		public String getType() {
			return type;
		}

		@Override
		public boolean isIndex() {
			return index;
		}
	}
	
	public enum IndexStreetTable implements IndexColumn {
		ID("long"), LATITUDE("double", true), LONGITUDE("double", true), NAME, NAME_EN, CITY("long", true); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		boolean index = false;
		String type = null;

		private IndexStreetTable() {
		}

		private IndexStreetTable(String type) {
			this.type = type;
		}

		private IndexStreetTable(String type, boolean index) {
			this(type);
			this.index = index;
		}

		public static String getTable() {
			return "street"; //$NON-NLS-1$
		}

		public String getTableName() {
			return getTable();
		}

		@Override
		public String getType() {
			return type;
		}

		@Override
		public boolean isIndex() {
			return index;
		}
	}
	
	public enum IndexStreetNodeTable implements IndexColumn {
		ID("long", true), LATITUDE("double"), LONGITUDE("double"), STREET("long", true), WAY("long", true); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		boolean index = false;
		String type = null;

		private IndexStreetNodeTable() {
		}

		private IndexStreetNodeTable(String type) {
			this.type = type;
		}

		private IndexStreetNodeTable(String type, boolean index) {
			this(type);
			this.index = index;
		}

		public static String getTable() {
			return "street_node"; //$NON-NLS-1$
		}

		public String getTableName() {
			return getTable();
		}

		@Override
		public String getType() {
			return type;
		}

		@Override
		public boolean isIndex() {
			return index;
		}
	}
	
	public enum IndexBuildingTable implements IndexColumn {
		ID("long"), LATITUDE("double"), LONGITUDE("double"), NAME, NAME_EN, STREET("long", true), POSTCODE(null, true); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		boolean index = false;
		String type = null;

		private IndexBuildingTable() {
		}

		private IndexBuildingTable(String type) {
			this.type = type;
		}

		private IndexBuildingTable(String type, boolean index) {
			this(type);
			this.index = index;
		}

		public static String getTable() {
			return "building"; //$NON-NLS-1$
		}

		public String getTableName() {
			return getTable();
		}

		@Override
		public String getType() {
			return type;
		}

		@Override
		public boolean isIndex() {
			return index;
		}
	}
	
	// Transport Index
	public enum IndexTransportStop implements IndexColumn {
		ID("long", true), LATITUDE("double", true), LONGITUDE("double", true), NAME, NAME_EN; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		boolean index = false;
		String type = null;

		private IndexTransportStop() {
		}

		private IndexTransportStop(String type) {
			this.type = type;
		}

		private IndexTransportStop(String type, boolean index) {
			this(type);
			this.index = index;
		}

		public static String getTable() {
			return "transport_stop"; //$NON-NLS-1$
		}

		public String getTableName() {
			return getTable();
		}

		@Override
		public String getType() {
			return type;
		}

		@Override
		public boolean isIndex() {
			return index;
		}
	}
	
	
	public enum IndexTransportRouteStop implements IndexColumn {
		STOP("long", true), ROUTE("long", true), DIRECTION("short"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		boolean index = false;
		String type = null;

		private IndexTransportRouteStop() {
		}

		private IndexTransportRouteStop(String type) {
			this.type = type;
		}

		private IndexTransportRouteStop(String type, boolean index) {
			this(type);
			this.index = index;
		}

		public static String getTable() {
			return "transport_route_stop"; //$NON-NLS-1$
		}

		public String getTableName() {
			return getTable();
		}

		@Override
		public String getType() {
			return type;
		}

		@Override
		public boolean isIndex() {
			return index;
		}
	}
	
	public enum IndexTransportRoute implements IndexColumn {
		ID("long", true), TYPE, OPERATOR, REF(null, true), NAME, NAME_EN, DIST("int"); //$NON-NLS-1$ //$NON-NLS-2$
		boolean index = false;
		String type = null;

		private IndexTransportRoute() {
		}

		private IndexTransportRoute(String type) {
			this.type = type;
		}

		private IndexTransportRoute(String type, boolean index) {
			this(type);
			this.index = index;
		}

		public static String getTable() {
			return "transport_route"; //$NON-NLS-1$
		}

		public String getTableName() {
			return getTable();
		}

		@Override
		public String getType() {
			return type;
		}

		@Override
		public boolean isIndex() {
			return index;
		}
	}
	
}
