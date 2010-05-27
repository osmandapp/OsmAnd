package com.osmand.data.index;

public class IndexConstants {
	
	public static final String POI_INDEX_DIR = "POI/";
	public static final String ADDRESS_INDEX_DIR = "Address/";
	
	public static final String POI_INDEX_EXT = ".poi.odb";
	public static final String ADDRESS_INDEX_EXT = ".addr.odb";
	
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
		b.append("create table ").append(columns[0].getTableName()).append(" (");
		boolean first = true;
		for(IndexColumn c : columns){
			if(first) {
				first = false;
			} else {
				b.append(", ");
			}
			b.append(c.toString());
			if(c.getType() != null){
				b.append(" ").append(c.getType());
			}
		}
		b.append(" ); ");
		return b.toString();
	}
	
	public static String generateSelectSQL(IndexColumn[] select){
		return generateSelectSQL(select, null);
	}
	
	public static String generateSelectSQL(IndexColumn[] select, String where){
		StringBuilder b = new StringBuilder();
		b.append("select ");
		boolean first = true;
		for(IndexColumn c : select){
			if(first) {
				first = false;
			} else {
				b.append(", ");
			}
			b.append(c.toString());
		}
		b.append(" FROM ").append(select[0].getTableName());
		if(where != null){
			b.append(" WHERE " ).append(where);
		}
		b.append(" ; ");
		return b.toString();
	}
	
	public static String generatePrepareStatementToInsert(String tableName, int numColumns){
		StringBuilder b = new StringBuilder();
		b.append("insert into ").append(tableName).append(" values (");
		for(int i=0; i< numColumns; i++){
			if(i > 0){
				b.append(", ");
			}
			b.append("?");
		}
		b.append(");");
		return b.toString();
	}
	
	public static String generateCreateIndexSQL(IndexColumn[] columns){
		StringBuilder b = new StringBuilder();
		String tableName = columns[0].getTableName();
		b.append("create index ").append(tableName).append("_index ON ").append(tableName).append(" (");
		boolean first = true;
		for(IndexColumn c : columns){
			if(!c.isIndex()){
				continue;
			}
			if(first) {
				first = false;
			} else {
				b.append(", ");
			}
			b.append(c.toString());
		}
		b.append(" ); ");
		if(first){
			return null;
		}
		return b.toString();
	}

	
	public enum IndexPoiTable implements IndexColumn {
		ID("long"), LATITUDE("double", true), LONGITUDE("double", true), NAME, TYPE, SUBTYPE;
		boolean index = false;
		String type = null;
		private IndexPoiTable(){}
		private IndexPoiTable(String type){
			this.type = type;
		}
		private IndexPoiTable(String type, boolean index){ this(type); this.index = index;}
		
		public static String getTable(){
			return "poi";
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
		
		
	public enum IndexCityTable implements IndexColumn {
		ID("long"), LATITUDE("double", true), LONGITUDE("double", true), NAME, CITY_TYPE;
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
			return "city";
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
		ID("long"), LATITUDE("double", true), LONGITUDE("double", true), NAME, CITY("long", true);
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
			return "street";
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
		ID("long"), LATITUDE("double"), LONGITUDE("double"), STREET("long", true), WAY("long", true);
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
			return "street_node";
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
		ID("long"), LATITUDE("double"), LONGITUDE("double"), NAME, STREET("long", true);
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
			return "building";
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
