package net.osmand.util;

import java.util.concurrent.LinkedBlockingDeque;

public class LIFOBlockingDeque<T> extends LinkedBlockingDeque<T> {

	private static final long serialVersionUID = 1L;

	@Override
	 public boolean offer(T t) {
	  return offerFirst(t);
	 }

	 @Override
	 public T remove() {
	  return removeFirst();
	 }
}
