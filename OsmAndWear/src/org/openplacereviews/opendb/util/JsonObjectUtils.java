//Revision d1a1f6e81d0716a47cbddf5754ee77fa5fc6d1d8
package org.openplacereviews.opendb.util;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Class uses for work with Json Object represent as Map.
 */
public class JsonObjectUtils {

	
	private static final int GET_OPERATION = 0;
	private static final int SET_OPERATION = 1;
	private static final int DELETE_OPERATION = 2;
	protected static final Log LOGGER = LogFactory.getLog(JsonObjectUtils.class);
	
	private static class OperationAccess {
		private final int operation;
		private final Object value;
		
		private OperationAccess(int op, Object v) {
			this.operation = op;
			this.value = v;
		}
		
	}

	/**
	 * Retrieve value from jsonMap by field sequence.
	 * @param jsonMap       source json object deserialized in map
	 * @param fieldSequence Sequence to field value.
	 *                      Example: person.car.number have to be ["person", "car[2]", "number"]
	 * @return Field value
	 */
	public static Object getField(Map<String, Object> jsonMap, String[] fieldSequence) {
		return accessField(jsonMap, fieldSequence, new OperationAccess(GET_OPERATION, null));
	}

	/**
	 * Set value to json field (path to field presented as sequence of string)
	 *
	 * @param jsonMap       source json object deserialized in map
	 * @param fieldSequence Sequence to field value.
	 *                      *                      Example: person.car.number have to be ["person", "car[2]", "number"]
	 * @param field         field value
	 * @return 
	 */
	public static Object setField(Map<String, Object> jsonMap, List<String> fieldSequence, Object field) {
		return setField(jsonMap, fieldSequence.toArray(new String[fieldSequence.size()]), field);
	}

	/**
	 * Set value to json field (path to field presented as sequence of string)
	 *
	 * @param jsonObject       source json object deserialized in map
	 * @param fieldSequence Sequence to field value.
	 *                      *                      Example: person.car.number have to be ["person", "car[2]", "number"]
	 * @param field         field value
	 * @return 
	 */
	public static Object setField(Map<String, Object> jsonObject, String[] fieldSequence, Object field) {
		return accessField(jsonObject, fieldSequence, new OperationAccess(SET_OPERATION, field));
	}
	
	
	/**
	 * Retrieve value from jsonMap by field sequence.
	 *
	 * @param jsonObject       source json object deserialized in map
	 * @param fieldSequence Sequence to field value.
	 *                      Example: person.car.number have to be ["person", "car[2]", "number"]
	 * @return Field value
	 */
	public static Object getField(Map<String, Object> jsonObject, List<String> fieldSequence) {
		return getField(jsonObject, fieldSequence.toArray(new String[fieldSequence.size()]));
	}

	/**
	 * Delete field value from json Map (field path presented as sequence of string)
	 *
	 * @param jsonMap       source json object deserialized in map
	 * @param fieldSequence Sequence to field value.
	 *                      Example: person.car.number have to be ["person", "car[2]", "number"]
	 * @return 
	 */
	public static Object deleteField(Map<String, Object> jsonMap, List<String> fieldSequence) {
		return accessField(jsonMap, fieldSequence.toArray(new String[fieldSequence.size()]), new OperationAccess(DELETE_OPERATION, null));
	}

	
	@SuppressWarnings("unchecked")
	private static Object accessField(Map<String, Object> jsonObject, String[] fieldSequence, OperationAccess op) {
		if (fieldSequence == null || fieldSequence.length == 0) {
			throw new IllegalArgumentException("Field sequence is empty. Set value to root not possible.");
		}
		String fieldName = null;
		Map<String, Object> jsonObjLocal = jsonObject;
		List<Object> jsonListLocal = null;
		int indexToAccess = -1;
		for(int i = 0; i < fieldSequence.length; i++) {
			boolean last = i == fieldSequence.length - 1;
			fieldName = fieldSequence[i];
			int indOpArray = -1;
			for(int ic = 0; ic < fieldName.length(); ) {
				if(ic > 0 && (fieldName.charAt(ic) == '[' || fieldName.charAt(ic) == ']') && 
						fieldName.charAt(ic - 1) == '\\') {
					// replace '\[' with '['
					fieldName = fieldName.substring(0, ic - 1) + fieldName.substring(ic);
				} else if(fieldName.charAt(ic) == '[') {
					indOpArray = ic;
					break;
				} else {
					ic++;
				}
			}
			jsonListLocal = null; // reset
			if(indOpArray == -1) {
				if(!last) {
					Map<String, Object> fieldAccess = (Map<String, Object>) jsonObjLocal.get(fieldName);
					if(fieldAccess == null) {
						if(op.operation == GET_OPERATION) {
							// don't modify during get operation
							return null;
						}
						Map<String, Object> newJsonMap = new TreeMap<>();
						jsonObjLocal.put(fieldName, newJsonMap);
						jsonObjLocal = newJsonMap;
					} else {
						jsonObjLocal = fieldAccess;
					}
				}
			} else {
				String arrayFieldName = fieldName.substring(0, indOpArray);
				if(arrayFieldName.contains("]")) {
					throw new IllegalArgumentException(String.format("Illegal field array modifier %s", fieldSequence[i]));
				}
				jsonListLocal = (List<Object>) jsonObjLocal.get(arrayFieldName);
				if (jsonListLocal == null) {
					if (op.operation == GET_OPERATION) {
						// don't modify during get operation
						return null;
					}
					jsonListLocal = new ArrayList<Object>();
					jsonObjLocal.put(arrayFieldName, jsonListLocal);
				}
				while (indOpArray != -1) {
					fieldName = fieldName.substring(indOpArray + 1);
					int indClArray = fieldName.indexOf("]");
					if (indClArray == -1) {
						throw new IllegalArgumentException(String.format("Illegal field array modifier %s", fieldSequence[i]));
					}
					if(indClArray == fieldName.length() - 1) {
						indOpArray = -1;
					} else if(fieldName.charAt(indClArray + 1) == '[') {
						indOpArray = indClArray + 1;
					} else {
						throw new IllegalArgumentException(String.format("Illegal field array modifier %s", fieldSequence[i]));
					}
					int index = Integer.parseInt(fieldName.substring(0, indClArray));
					if (last && indOpArray == -1) {
						indexToAccess = index;
					} else {
						Object obj = null;
						if (index < jsonListLocal.size() && index >= 0) {
							obj = jsonListLocal.get(index);
						} else if (op.operation == SET_OPERATION && (index == -1 || index == jsonListLocal.size())) {
							index = jsonListLocal.size();
							jsonListLocal.add(null);
						} else {
							throw new IllegalArgumentException(
									String.format("Illegal access to array at position %d", index));
						}

						if (obj == null) {
							if (op.operation == GET_OPERATION) {
								// don't modify during get operation
								return null;
							}
							if (indOpArray == -1) {
								obj = new TreeMap<>();
							} else {
								obj = new ArrayList<Object>();
							}
							jsonListLocal.set(index, obj);
						}
						if(indOpArray != -1) {
							jsonListLocal = (List<Object>) obj;
						} else {
							jsonObjLocal = (Map<String, Object>) obj;
							jsonListLocal = null;
						}
					}
				}

			}
		}
		if(jsonListLocal != null) {
			return accessListField(op, jsonListLocal, indexToAccess);
		} else {
			return accessObjField(op, jsonObjLocal, fieldName);
		}
	}
	
	private static Object accessObjField(OperationAccess op, Map<String, Object> jsonObjLocal, String fieldName) {
		Object prevValue;
		if (op.operation == DELETE_OPERATION) {
			prevValue = jsonObjLocal.remove(fieldName);
		} else if (op.operation == SET_OPERATION) {
			prevValue = jsonObjLocal.put(fieldName, op.value);
		} else {
			prevValue = jsonObjLocal.get(fieldName);
		}
		return prevValue;
	}

	private static Object accessListField(OperationAccess op, List<Object> jsonListLocal, int indexToAccess) {
		Object prevValue;
		int lastIndex = indexToAccess;
		if (op.operation == DELETE_OPERATION) {
			if (lastIndex >= jsonListLocal.size() || lastIndex < 0) {
				prevValue = null;
			} else {
				prevValue = jsonListLocal.remove(lastIndex);
			}
		} else if (op.operation == SET_OPERATION) {
			if (lastIndex == jsonListLocal.size() || lastIndex == -1) {
				prevValue = null;
				jsonListLocal.add(op.value);
			} else if (lastIndex >= jsonListLocal.size() || lastIndex < 0) {
				throw new IllegalArgumentException(String.format("Illegal access to %d position in array with size %d",
						lastIndex, jsonListLocal.size()));
			} else {
				prevValue = jsonListLocal.set(lastIndex, op.value);
			}
		} else {
			if (lastIndex >= jsonListLocal.size() || lastIndex < 0) {
				prevValue = null;
			} else {
				prevValue = jsonListLocal.get(lastIndex);
			}
		}
		return prevValue;
	}

	@SuppressWarnings("unchecked")
	public static List<Object> getIndexObjectByField(Object obj, List<String> field, List<Object> res) {
		if(obj == null) {
			return res;
		}
		if(field.size() == 0) {
			if(res == null) {
				res = new ArrayList<Object>();
			}
			res.add(obj);
			return res;
		}
		if (obj instanceof Map) {
			String fieldFirst = field.get(0);
			Object value = ((Map<String, Object>) obj).get(fieldFirst);
			return getIndexObjectByField(value, field.subList(1, field.size()), res);
		} else if(obj instanceof Collection) {
			for(Object o : ((Collection<Object>)obj)) {
				res = getIndexObjectByField(o, field, res);
			}
		} else {
			// we need extract but there no field 
			LOGGER.warn(String.format("Can't access field %s for object %s", field, obj));
		}
		return res;
	}


}
