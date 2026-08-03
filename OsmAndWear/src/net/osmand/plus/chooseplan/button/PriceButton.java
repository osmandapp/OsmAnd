package net.osmand.plus.chooseplan.button;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase;
import net.osmand.util.Algorithms;

public abstract class PriceButton<T extends InAppPurchase> implements Comparable<PriceButton> {

	private final String id;
	protected CharSequence title;
	protected CharSequence price;
	protected String discount;
	protected String description;
	protected T purchaseItem;
	protected boolean discountApplied;

	public PriceButton(@NonNull String id, @NonNull T purchaseItem) {
		this.id = id;
		this.purchaseItem = purchaseItem;
	}

	public abstract void onApply(@NonNull FragmentActivity activity, @NonNull InAppPurchaseHelper purchaseHelper);

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

	public String getDescription() {
		return description;
	}

	public T getPurchaseItem() {
		return purchaseItem;
	}

	public boolean isDiscountApplied() {
		return discountApplied;
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

	public void setDescription(String description) {
		this.description = description;
	}

	public void setDiscountApplied(boolean discountApplied) {
		this.discountApplied = discountApplied;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof PriceButton)) return false;

		PriceButton<?> that = (PriceButton<?>) o;
		return Algorithms.stringsEqual(id, that.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public int compareTo(PriceButton o) {
		return Double.compare(purchaseItem.getPriceValue(), o.getPurchaseItem().getPriceValue());
	}
}
