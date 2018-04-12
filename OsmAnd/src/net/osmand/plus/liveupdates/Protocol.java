package net.osmand.plus.liveupdates;

/**
 * Created by GaidamakUA on 1/12/16.
 */
public final class Protocol {
	private Protocol(){}

	public static class RankingByMonthResponse {
		public String month;
		public RankingByMonth[] rows;
	}
	
	public static class RankingUserByMonthResponse {
		public String month;
		public UserRankingByMonth[] rows;
	}
	
	public static class RecipientsByMonth {
		public String month;
		public String message;
		public float regionBtc;
		public int regionCount;
		public float regionPercentage;
		public float btc;
		public float eur;
		public float eurRate;
		public Recipient[] rows;
	}
	
	public static class UserRankingByMonth {
		public String user ;
		public int changes;
		public int globalchanges;
		public int rank;
		public int grank;
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

	public static class Recipient {
		String osmid;
		int changes;
		String btcaddress;
		int rank;
		int weight;
		float btc;
	}
}
