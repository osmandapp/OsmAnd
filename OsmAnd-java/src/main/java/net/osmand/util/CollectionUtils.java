package net.osmand.util;

import static net.osmand.util.Algorithms.isEmpty;

import net.osmand.CallbackWithObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class CollectionUtils {

	/********************* Collection Operations **************************/

	@SafeVarargs
	public static <T> List<T> asOneList(Collection<T> ... collections) {
		List<T> result = new ArrayList<>();
		for (Collection<T> collection : collections) {
			result.addAll(collection);
		}
		return result;
	}

	public static <T> List<T> addToList(Collection<T> original, T element) {
		List<T> copy = new ArrayList<>(original);
		copy.add(element);
		return copy;
	}

	public static <T> List<T> addAllToList(Collection<T> original, Collection<T> elements) {
		List<T> copy = new ArrayList<>(original);
		copy.addAll(elements);
		return copy;
	}

	public static <T> List<T> setInList(Collection<T> original, int position, T element) {
		List<T> copy = new ArrayList<>(original);
		copy.set(position, element);
		return copy;
	}

	public static <T> List<T> removeFromList(Collection<T> original, T element) {
		List<T> copy = new ArrayList<>(original);
		copy.remove(element);
		return copy;
	}

	public static <T> List<T> removeAllFromList(Collection<T> original, Collection<T> elements) {
		List<T> copy = new ArrayList<>(original);
		copy.removeAll(elements);
		return copy;
	}

	public static <T> void addAllIfNotContains(Collection<T> collection, Collection<T> elements) {
		for (T element : elements) {
			addIfNotContains(collection, element);
		}
	}

	public static <T> void addIfNotContains(T element, Collection<T> ... collections) {
		for (Collection<T> collection : collections) {
			addIfNotContains(collection, element);
		}
	}

	public static <T> void addIfNotContains(Collection<T> collection, T element) {
		if (!collection.contains(element)) {
			collection.add(element);
		}
	}

	public static <T> T searchElementWithCondition(Collection<T> collection, CallbackWithObject<T> condition) {
		for (T element : collection) {
			if (condition.processResult(element)) {
				return element;
			}
		}
		return null;
	}

	public static <T> List<T> filterElementsWithCondition(Collection<T> collection, CallbackWithObject<T> condition) {
		List<T> result = new ArrayList<>();
		for (T element : collection) {
			if (condition.processResult(element)) {
				result.add(element);
			}
		}
		return result;
	}

	/********************* Inclusion Checks *******************************/

	public static <T> boolean containsAny(Collection<T> collection, T... objects) {
		for (T object : objects) {
			if (collection.contains(object)) {
				return true;
			}
		}
		return false;
	}

	public static boolean startsWithAny(String s, String ... args) {
		if (!isEmpty(s) && args != null) {
			for (String arg : args) {
				if (s.startsWith(arg)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean containsAny(String s, String ... args) {
		if (!isEmpty(s) && args != null) {
			for (String arg : args) {
				if (s.contains(arg)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean endsWithAny(String s, String ... args) {
		if (!isEmpty(s) && args != null) {
			for (String arg : args) {
				if (s.endsWith(arg)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean equalsToAny(Object o, Object ... args) {
		if (args != null) {
			for (Object o1 : args) {
				if (Objects.equals(o, o1)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean anyIsNull(Object ... args) {
		if (args != null) {
			for (Object o : args) {
				if (o == null) {
					return true;
				}
			}
		}
		return false;
	}

	/********************* Array Manipulations ****************************/

	public static <T> void reverseArray(T[] array) {
		for (int i = 0; i < array.length / 2; i++) {
			T temp = array[i];
			array[i] = array[array.length - i - 1];
			array[array.length - i - 1] = temp;
		}
	}

	public static boolean containsInArrayL(long[] array, long value) {
		return Arrays.binarySearch(array, value) >= 0;
	}

	public static long[] addToArrayL(long[] array, long value, boolean skipIfExists) {
		long[] result;
		if (array == null) {
			result = new long[]{ value };
		} else if (skipIfExists && Arrays.binarySearch(array, value) >= 0) {
			result = array;
		} else {
			result = new long[array.length + 1];
			System.arraycopy(array, 0, result, 0, array.length);
			result[result.length - 1] = value;
			Arrays.sort(result);
		}
		return result;
	}

	public static long[] removeFromArrayL(long[] array, long value) {
		long[] result;
		if (array != null) {
			int index = Arrays.binarySearch(array, value);
			if (index >= 0) {
				result = new long[array.length - 1];
				System.arraycopy(array, 0, result, 0, index);
				if (index < result.length) {
					System.arraycopy(array, index + 1, result, index, array.length - (index + 1));
				}
				return result;
			} else {
				return array;
			}
		} else {
			return array;
		}
	}

	public static String arrayToString(int[] a) {
		if (a == null || a.length == 0) {
			return null;
		}
		StringBuilder b = new StringBuilder();
		for (int value : a) {
			if (b.length() > 0) {
				b.append(",");
			}
			b.append(value);
		}
		return b.toString();
	}

	public static int[] stringToArray(String array) throws NumberFormatException {
		if (array == null || array.length() == 0) {
			return null;
		}
		String[] items = array.split(",");
		int[] res = new int[items.length];
		for (int i = 0; i < items.length; i++) {
			res[i] = Integer.parseInt(items[i]);
		}
		return res;
	}

}
