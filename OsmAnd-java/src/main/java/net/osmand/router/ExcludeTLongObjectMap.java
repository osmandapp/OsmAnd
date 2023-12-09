package net.osmand.router;

import java.util.Collection;
import java.util.Map;

import gnu.trove.function.TObjectFunction;
import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.procedure.TLongObjectProcedure;
import gnu.trove.procedure.TLongProcedure;
import gnu.trove.procedure.TObjectProcedure;
import gnu.trove.set.TLongSet;

public class ExcludeTLongObjectMap<T> implements TLongObjectMap<T> {

	int size = 0;
	long[] keys;
	TLongObjectMap<T> map;

	public ExcludeTLongObjectMap(TLongObjectMap<T> map, long... keys) {
		this.map = map;
		this.keys = keys;
	}

	@Override
	public long getNoEntryKey() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	@Override
	public boolean containsKey(long key) {
		if (checkException(key)) {
			return false;
		}
		return map.containsKey(key);
	}

	private boolean checkException(long key) {
		for(long k : keys) {
			if(key == k) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public T get(long key) {
		if(checkException(key)) {
			return null;
		}
		return map.get(key);
	}

	@Override
	public T put(long key, T value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public T putIfAbsent(long key, T value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public T remove(long key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map<? extends Long, ? extends T> m) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public TLongSet keySet() {
		throw new UnsupportedOperationException();
	}

	@Override
	public long[] keys() {
		throw new UnsupportedOperationException();
	}

	@Override
	public long[] keys(long[] array) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<T> valueCollection() {
		throw new UnsupportedOperationException();
	}


	@Override
	public TLongObjectIterator<T> iterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean forEachKey(TLongProcedure procedure) {
		throw new UnsupportedOperationException();
	}


	@Override
	public void transformValues(TObjectFunction<T, T> function) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(TLongObjectMap<T> map) {
		throw new UnsupportedOperationException();
	}

	@Override
	public T[] values() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T[] values(T[] array) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean forEachValue(TObjectProcedure<T> procedure) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean forEachEntry(TLongObjectProcedure<T> procedure) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainEntries(TLongObjectProcedure<T> procedure) {
		throw new UnsupportedOperationException();
	}

}