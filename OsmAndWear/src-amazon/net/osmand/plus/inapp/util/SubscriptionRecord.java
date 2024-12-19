package net.osmand.plus.inapp.util;

public class SubscriptionRecord {
	public static int TO_DATE_NOT_SET = -1;
	private String amazonReceiptId;
	private long from;
	private long to = TO_DATE_NOT_SET;
	private String amazonUserId;
	private String sku;

	public long getFrom() {
		return from;
	}

	public void setFrom(final long subscriptionFrom) {
		this.from = subscriptionFrom;
	}

	public long getTo() {
		return to;
	}

	public void setTo(final long subscriptionTo) {
		this.to = subscriptionTo;
	}

	public boolean isActiveNow() {
		return TO_DATE_NOT_SET == to;
	}

	public boolean isActiveForDate(final long date) {
		return date >= from && (isActiveNow() || date <= to);
	}

	public String getAmazonReceiptId() {
		return amazonReceiptId;
	}

	public void setAmazonReceiptId(final String receiptId) {
		this.amazonReceiptId = receiptId;
	}

	public String getAmazonUserId() {
		return amazonUserId;
	}

	public void setAmazonUserId(final String amazonUserId) {
		this.amazonUserId = amazonUserId;
	}

	public void setSku(final String sku) {
		this.sku = sku;
	}

	public String getSku() {
		return this.sku;
	}
}