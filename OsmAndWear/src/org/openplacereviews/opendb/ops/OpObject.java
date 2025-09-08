//Revision d1a1f6e81d0716a47cbddf5754ee77fa5fc6d1d8
package org.openplacereviews.opendb.ops;

import com.google.gson.*;
import org.openplacereviews.opendb.util.JsonObjectUtils;
// OSMAND ANDROID CHANGE BEGIN:
// removed unused imports
// OSMAND ANDROID CHANGE END

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class OpObject {
	
	public static final String F_NAME = "name";
	public static final String F_ID = "id";
	public static final String F_COMMENT = "comment";
	public static final String TYPE_OP = "sys.op";
	public static final String TYPE_BLOCK = "sys.block";
	public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
	// transient info about validation timing etc
	public static final String F_EVAL = "eval";
	public static final String F_VALIDATION = "validation";
	public static final String F_TIMESTAMP_ADDED = "timestamp";
	public static final String F_PARENT_TYPE = "parentType";
	public static final String F_PARENT_HASH = "parentHash";
	public static final String F_CHANGE = "change";
	public static final String F_CURRENT = "current";
	// voting
	public static final String F_OP = "op";
	public static final String F_STATE = "state";
	public static final String F_OPEN = "open";
	public static final String F_FINAL = "final";
	public static final String F_VOTE = "vote";
	public static final String F_VOTES = "votes";
	public static final String F_SUBMITTED_OP_HASH = "submittedOpHash";
	public static final String F_USER = "user";

	public static final OpObject NULL = new OpObject(true);

	public static SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
	static {
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	protected Map<String, Object> fields = new TreeMap<>();
	protected transient Map<String, Object> cacheFields;
	protected boolean isImmutable;
	
	protected transient String parentType;
	protected transient String parentHash;
	protected transient boolean deleted;
	
	
	public OpObject() {}
	
	public OpObject(boolean deleted) {
		this.deleted = deleted;
	}
	
	public OpObject(OpObject cp) {
		this(cp, false);
	}
	
	public OpObject(OpObject cp, boolean copyCacheFields) {
		createOpObjectCopy(cp, copyCacheFields);
	}

	@SuppressWarnings("unchecked")
	private OpObject createOpObjectCopy(OpObject opObject, boolean copyCacheFields) {
		this.parentType = opObject.parentType;
		this.parentHash = opObject.parentHash;
		this.deleted = opObject.deleted;
		this.fields = (Map<String, Object>) copyingObjects(opObject.fields, copyCacheFields);
		if (opObject.cacheFields != null && copyCacheFields) {
			this.cacheFields = (Map<String, Object>) copyingObjects(opObject.cacheFields, copyCacheFields);
		}
		this.isImmutable = false;

		return this;
	}
	
	public boolean isDeleted() {
		return deleted;
	}

	@SuppressWarnings("unchecked")
	private Object copyingObjects(Object object, boolean copyCacheFields) {
		if (object instanceof Number) {
			return object;
		} else if (object instanceof String) {
			return object;
		} else if (object instanceof Boolean) {
			return object;
		} else if (object instanceof List) {
			List<Object> copy = new ArrayList<>();
			List<Object> list = (List<Object>) object;
			for (Object o : list) {
				copy.add(copyingObjects(o, copyCacheFields));
			}
			return copy;
		} else if (object instanceof Map) {
			Map<Object, Object> copy = new LinkedHashMap<>();
			Map<Object, Object> map = (Map<Object, Object>) object;
			for (Object o : map.keySet()) {
				copy.put(o, copyingObjects(map.get(o), copyCacheFields));
			}
			return copy;
		}
		// OSMAND ANDROID CHANGE BEGIN:
		// removed instanceOf OpExprEvaluator
		// OSMAND ANDROID CHANGE END:
		else if (object instanceof OpObject) {
			return new OpObject((OpObject) object);
		} else {
			throw new UnsupportedOperationException("Type of object is not supported");
		}
	}
	
	public void setParentOp(OpOperation op) {
		setParentOp(op.type, op.getRawHash());
	}
	
	public void setParentOp(String parentType, String parentHash) {
		this.parentType = parentType;
		this.parentHash = parentHash;
	}
	
	public String getParentHash() {
		return parentHash;
	}
	
	public String getParentType() {
		return parentType;
	}
	
	public List<String> getId() {
		return getStringList(F_ID);
	}
	
	public void setId(String id) {
		addOrSetStringValue(F_ID, id);
	}
	
	public boolean isImmutable() {
		return isImmutable;
	}
	
	public OpObject makeImmutable() {
		isImmutable = true;
		return this;
	}

	public Object getFieldByExpr(String field) {
		if (field.contains(".") || field.contains("[") || field.contains("]")) {
			return JsonObjectUtils.getField(this.fields, generateFieldSequence(field));
		}

		return fields.get(field);
	}

	
	/**
	 * generateFieldSequence("a") - [a]
	 * generateFieldSequence("a.b") - [a, b]
	 * generateFieldSequence("a.b.c.de") - [a, b, c, de]
	 * generateFieldSequence("a.bwerq.c") - [a, bwerq, c]
	 * generateFieldSequence("a.bwerq...c") - [a, bwerq, c] 
	 * generateFieldSequence("a.bwereq..c..") - [a, bwerq, c]
	 * generateFieldSequence("a.{b}") - [a, b]
	 * generateFieldSequence("a.{b.c.de}") - [a, b.c.de]
	 * generateFieldSequence("a.{b.c.de}") - [a, b.c.de]
	 * generateFieldSequence("a.{b{}}") - [a, b{}]
	 * generateFieldSequence("a.{b{}d.q}") - [a, b{}d.q]
	 */
	private static List<String> generateFieldSequence(String field) {
		int STATE_OPEN_BRACE = 1;
		int STATE_OPEN = 0;
		int state = STATE_OPEN;
		int start = 0;
		List<String> l = new ArrayList<String>();
		for(int i = 0; i < field.length(); i++) {
			boolean split = false;
			if (i == field.length() - 1) {
				if (state == STATE_OPEN_BRACE) {
					if(field.charAt(i) == '}') {
						split = true;
					} else {
						throw new IllegalArgumentException("Illegal field expression: " + field);
					}
				} else {
					if(field.charAt(i) != '.') {
						i++;
					}
					split = true;
				}
			} else {
				if (field.charAt(i) == '.' && state == STATE_OPEN) {
					split = true;
				} else if (field.charAt(i) == '}' && field.charAt(i + 1) == '.' && state == STATE_OPEN_BRACE) {
					split = true;
				} else if (field.charAt(i) == '{' && state == STATE_OPEN) {
					if(start != i) {
						throw new IllegalArgumentException("Illegal field expression (wrap {} is necessary): " + field);
					}
					state = STATE_OPEN_BRACE;
					start = i + 1;
				}
			}
			if(split) {
				if (i != start) {
					l.add(field.substring(start, i));
				}
				start = i + 1;
				state = STATE_OPEN;
			}
		}
		return l;
	}

	public void setFieldByExpr(String field, Object object) {
		if (field.contains(".") || field.contains("[") || field.contains("]")) {
			List<String> fieldSequence = generateFieldSequence(field);
			if (object == null) {
				JsonObjectUtils.deleteField(this.fields, fieldSequence);
			} else {
				JsonObjectUtils.setField(this.fields, fieldSequence, object);
			}
		} else if (object == null) {
			fields.remove(field);
		} else {
			fields.put(field, object);
		}
	}

	
	public Object getCacheObject(String f) {
		if(cacheFields == null) {
			return null;
		}
		return cacheFields.get(f);
	}
	
	public void putCacheObject(String f, Object o) {
		if (isImmutable()) {
			if (cacheFields == null) {
				cacheFields = new ConcurrentHashMap<String, Object>();
			}
			cacheFields.put(f, o);
		}
	}
	
	public void setId(String id, String id2) {
		List<String> list = new ArrayList<String>();
		list.add(id);
		list.add(id2);
		putObjectValue(F_ID, list);
	}
	
	public String getName() {
		return getStringValue(F_NAME);
	}
	
	public String getComment() {
		return getStringValue(F_COMMENT);
	}
	
	public Map<String, Object> getRawOtherFields() {
		return fields;
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, String> getStringMap(String field) {
		return (Map<String, String>) fields.get(field);
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, List<String>> getMapStringList(String field) {
		return (Map<String, List<String>>) fields.get(field);
	}
	
	@SuppressWarnings("unchecked")
	public List<Map<String, String>> getListStringMap(String field) {
		return (List<Map<String, String>>) fields.get(field);
	}
	
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> getListStringObjMap(String field) {
		return (List<Map<String, Object>>) fields.get(field);
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getStringObjMap(String field) {
		return (Map<String, Object>) fields.get(field);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getField(T def, String... fields) {
		Map<String, Object> p = this.fields;
		for(int i = 0; i < fields.length - 1 ; i++) {
			p = (Map<String, Object>) p.get(fields[i]);
			if(p == null) {
				return def;
			}
		}
		T res = (T) p.get(fields[fields.length - 1]);
		if(res == null) {
			return def;
		}
		return res;
	}

	@SuppressWarnings("unchecked")
	public Map<List<String>, Object> getStringListObjMap(String field) {
		return (Map<List<String>, Object>) fields.get(field);
	}
	

	public long getDate(String field) {
		String date = getStringValue(field);
		// OSMAND ANDROID CHANGE BEGIN:
		// removed check OUtils.isEmpty(date)
		// OSMAND ANDROID CHANGE END
		try {
			return dateFormat.parse(date).getTime();
		} catch (ParseException e) {
			return 0;
		}
	}

	
	public void setDate(String field, long time) {
		putStringValue(field, dateFormat.format(new Date(time)));
	}
	
	public Number getNumberValue(String field) {
		return (Number) fields.get(field);
	}
	
	public int getIntValue(String key, int def) {
		Number o = getNumberValue(key);
		return o == null ? def : o.intValue();
	}
	
	public long getLongValue(String key, long def) {
		Number o = getNumberValue(key);
		return o == null ? def : o.longValue();
	}
	
	public String getStringValue(String field) {
		Object o = fields.get(field);
		if (o instanceof String || o == null) {
			return (String) o;
		}
		return o.toString();
	}
	
	@SuppressWarnings("unchecked")
	public List<String> getStringList(String field) {
		// cast to list if it is single value
		Object o = fields.get(field);
		if(o == null || o.toString().isEmpty()) {
			return Collections.emptyList();
		}
		if(o instanceof String) {
			return Collections.singletonList(o.toString());
		}
		return (List<String>) o;
	}

	public Object getObjectValue(String field) {
		return fields.get(field);
	}

	public void putStringValue(String key, String value) {
		checkNotImmutable();
		if(value == null) {
			fields.remove(key);
		} else {
			fields.put(key, value);
		}
	}
	
	/**
	 * Operates as a single value if cardinality is less than 1
	 * or as a list of values if it stores > 1 value
	 * @param key
	 * @param value
	 */
	@SuppressWarnings("unchecked")
	public void addOrSetStringValue(String key, String value) {
		checkNotImmutable();
		Object o = fields.get(key);
		if(o == null) {
			fields.put(key, value);
		} else if(o instanceof List) {
			((List<String>) o).add(value);
		} else  {
			List<String> list = new ArrayList<String>();
			list.add(o.toString());
			list.add(value);
			fields.put(key, list);
		}
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, Object> getChangedEditFields() {
		return (Map<String, Object>) fields.get(F_CHANGE);
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, Object> getCurrentEditFields() {
		return (Map<String, Object>) fields.get(F_CURRENT);
	}
	
	public void putObjectValue(String key, Object value) {
		checkNotImmutable();
		if(value == null) {
			fields.remove(key);
		} else {
			fields.put(key, value);
		}
	}
	
	public void checkNotImmutable() {
		if(isImmutable) {
			throw new IllegalStateException("Object is immutable");
		}
		
	}
	
	public void checkImmutable() {
		if(!isImmutable) {
			throw new IllegalStateException("Object is mutable");
		}
	}

	public Object remove(String key) {
		checkNotImmutable();
		return fields.remove(key);
	}
	
	public Map<String, Object> getMixedFieldsAndCacheMap() {
		TreeMap<String, Object> mp = new TreeMap<>(fields);
		if(cacheFields != null || parentType != null || parentHash != null) {
			TreeMap<String, Object> eval = new TreeMap<String, Object>();
			
			if(parentType != null) {
				eval.put(F_PARENT_TYPE, parentType);
			}
			if(parentHash != null) {
				eval.put(F_PARENT_HASH, parentHash);
			}
			if (cacheFields != null) {
				Iterator<Entry<String, Object>> it = cacheFields.entrySet().iterator();
				while (it.hasNext()) {
					Entry<String, Object> e = it.next();
					Object v = e.getValue();
					if (v instanceof Map || v instanceof String || v instanceof Number) {
						eval.put(e.getKey(), v);
					}
				}
			}
			if(eval.size() > 0) {
				mp.put(F_EVAL, eval);
			}
		}
		return mp;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fields == null) ? 0 : fields.hashCode());
		return result;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + fields + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OpObject other = (OpObject) obj;
		if (fields == null) {
			return other.fields == null;
		} else return fields.equals(other.fields);
	}

	public static class OpObjectAdapter implements JsonDeserializer<OpObject>,
			JsonSerializer<OpObject> {
		
		private final boolean fullOutput;

		public OpObjectAdapter(boolean fullOutput) {
			this.fullOutput = fullOutput;
		}

		@Override
		public OpObject deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			OpObject bn = new OpObject();
			bn.fields = context.deserialize(json, TreeMap.class);
			// remove cache
			bn.fields.remove(F_EVAL);
			return bn;
		}

		@Override
		public JsonElement serialize(OpObject src, Type typeOfSrc, JsonSerializationContext context) {
			return context.serialize(fullOutput ? src.getMixedFieldsAndCacheMap() : src.fields);
		}


	}

	

	


}
