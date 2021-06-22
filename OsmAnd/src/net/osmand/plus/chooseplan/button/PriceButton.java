package net.osmand.plus.chooseplan.button;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase;

public abstract class PriceButton<T extends InAppPurchase> {

	private String id;
	protected CharSequence title;
	protected CharSequence price;
	protected String discount;
	protected String regularPrice;
	protected T purchaseItem;

	public PriceButton(@NonNull String id, @NonNull T purchaseItem) {
		this.id = id;
		this.purchaseItem = purchaseItem;
	}

	public abstract void onApply(@NonNull FragmentActivity activity,
	                             @NonNull InAppPurchaseHelper purchaseHelper);

	public String getId() {
		return id;
	}

	public CharSequence getTitle() {
		return title;
	}

	public CharSequence getPrice() {
		return price;
	}

	public String getDiscount() {
		return discount;
	}

	public String getRegularPrice() {
		return regularPrice;
	}

	public T getPurchaseItem() {
		return purchaseItem;
	}

	public void setTitle(CharSequence title) {
		this.title = title;
	}

	public void setPrice(CharSequence price) {
		this.price = price;
	}

	public void setDiscount(String discount) {
		this.discount = discount;
	}

	public void setRegularPrice(String regularPrice) {
		this.regularPrice = regularPrice;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof PriceButton)) return false;

		PriceButton<?> that = (PriceButton<?>) o;

		return id != null ? id.equals(that.id) : that.id == null;
	}

	@Override
	public int hashCode() {
		return id != null ? id.hashCode() : 0;
	}
}
