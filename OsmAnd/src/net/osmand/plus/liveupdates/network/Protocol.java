package net.osmand.plus.liveupdates.network;

/**
 * Created by GaidamakUA on 1/12/16.
 */
public final class Protocol {
	private Protocol(){}

	public static class RankingByMonthResponse {
		public String month;
		public RankingByMonth[] rows;
	}

	// {"rank":"8","countUsers":"713","minChanges":"14","maxChanges":"18","avgChanges":"15.9845722300140252"}
	public static class RankingByMonth {
		public int rank;
		public int countUsers;
		public int minChanges;
		public int maxChanges;
		public float avgChanges;
	}

	// {"month":"2015-11","users":"28363","changes":"673830"}
	public static class TotalChangesByMonthResponse {
		public String month;
		public int users;
		public int changes;
	}
}
