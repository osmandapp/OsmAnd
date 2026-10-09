//Revision d1a1f6e81d0716a47cbddf5754ee77fa5fc6d1d8
package org.openplacereviews.opendb.ops;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.*;
// OSMAND ANDROID CHANGE BEGIN:
// removed dependency OUtils
// OSMAND ANDROID CHANGE END:
public class OpOperation extends OpObject {

	public static final String F_TYPE = "type";
	public static final String F_SIGNED_BY = "signed_by";
	public static final String F_HASH = "hash";

	public static final String F_SIGNATURE = "signature";

	public static final String F_REF = "ref";
	public static final String F_CREATE = "create";
	public static final String F_DELETE = "delete";
	public static final String F_EDIT = "edit";

	public static final String F_NAME = "name";
	public static final String F_COMMENT = "comment";

	private final List<OpObject> createdObjects = new LinkedList<OpObject>();
	private final List<OpObject> editedObjects = new LinkedList<OpObject>();
	protected String type;

	public OpOperation() {
	}

	public OpOperation(OpOperation cp, boolean copyCacheFields) {
		super(cp, copyCacheFields);
		this.type = cp.type;
		for(OpObject o : cp.createdObjects) {
			this.createdObjects.add(new OpObject(o, copyCacheFields));
		}
		for(OpObject o : cp.editedObjects) {
			this.editedObjects.add(new OpObject(o, copyCacheFields));
		}
	}

	public String getOperationType() {
		return type;
	}

	public void setType(String name) {
		checkNotImmutable();
		type = name;
		updateObjectsRef();
	}

	protected void updateObjectsRef() {
		for(OpObject o : createdObjects) {
			o.setParentOp(this);
		}
		for(OpObject o : editedObjects) {
			o.setParentOp(this);
		}
	}

	public String getType() {
		return type;
	}

	public OpOperation makeImmutable() {
		isImmutable = true;
		for(OpObject o : createdObjects) {
			o.makeImmutable();
		}
		for(OpObject o : editedObjects) {
			o.makeImmutable();
		}
		return this;
	}

	public void setSignedBy(String value) {
		putStringValue(F_SIGNED_BY, value);
	}

	public void addOtherSignedBy(String value) {
		addOrSetStringValue(F_SIGNED_BY, value);
	}

	public List<String> getSignedBy() {
		return getStringList(F_SIGNED_BY);
	}

	public String getHash() {
		return getStringValue(F_HASH);
	}

	public String getRawHash() {
		String rw = getStringValue(F_HASH);
		// drop algorithm and everything else
		if(rw != null) {
			rw = rw.substring(rw.lastIndexOf(':') + 1);
		}
		return rw;
	}

	public List<String> getSignatureList() {
		return getStringList(F_SIGNATURE);
	}

	public Map<String, List<String>> getRef() {
		return getMapStringList(F_REF);
	}

	@SuppressWarnings("unchecked")
	public List<List<String>> getDeleted() {
		List<List<String>> l = (List<List<String>>) fields.get(F_DELETE);
		if(l == null) {
			return Collections.emptyList();
		}
		return l;
	}

	public boolean hasDeleted() {
		return getDeleted().size() > 0;
	}

	public void addDeleted(List<String> id) {
		if(!fields.containsKey(F_DELETE)) {
			ArrayList<List<String>> lst = new ArrayList<>();
			lst.add(id);
			putObjectValue(F_DELETE, lst);
		} else {
			getDeleted().add(id);
		}
	}

	public List<OpObject> getCreated() {
		return createdObjects;
	}

	public void addCreated(OpObject o) {
		checkNotImmutable();
		createdObjects.add(o);
		if(type != null) {
			o.setParentOp(this);
		}
	}

	public boolean hasCreated() {
		return createdObjects.size() > 0;
	}

	public void addEdited(OpObject o) {
		checkNotImmutable();
		editedObjects.add(o);
		if (type != null) {
			o.setParentOp(this);
		}
	}

	public List<OpObject> getEdited() {
		return editedObjects;
	}

	public boolean hasEdited() {
		return editedObjects.size() > 0;
	}


	public String getName() {
		return getStringValue(F_NAME);
	}

	public String getComment() {
		return getStringValue(F_COMMENT);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((createdObjects == null) ? 0 : createdObjects.hashCode());
		result = prime * result + ((editedObjects == null) ? 0 : editedObjects.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		OpOperation other = (OpOperation) obj;
		if (createdObjects == null) {
			if (other.createdObjects != null)
				return false;
		} else if (!createdObjects.equals(other.createdObjects))
			return false;
		if (editedObjects == null) {
			if (other.editedObjects != null)
				return false;
		} else if (!editedObjects.equals(other.editedObjects))
			return false;
		if (type == null) {
			return other.type == null;
		} else return type.equals(other.type);
	}

	// OSMAND ANDROID CHANGE BEGIN:
	// removed unused classes and methods
	//      public static OpObjectDiffBuilder createDiffOperation(OpObject o)
	//      private static Object diffSet(Object vl)
	//      public static class OpObjectDiffBuilder{}
	// OSMAND ANDROID CHANGE END

	public static class OpOperationBeanAdapter implements JsonDeserializer<OpOperation>,
			JsonSerializer<OpOperation> {

		// plain serialization to calculate hash
		private final boolean excludeHashAndSignature;
		private final boolean fullOutput;

		public OpOperationBeanAdapter(boolean fullOutput, boolean excludeHashAndSignature) {
			this.excludeHashAndSignature = excludeHashAndSignature;
			this.fullOutput = fullOutput;
		}

		public OpOperationBeanAdapter(boolean fullOutput) {
			this.fullOutput = fullOutput;
			this.excludeHashAndSignature = false;
		}

		@Override
		public OpOperation deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			JsonObject jsonObj = json.getAsJsonObject();
			OpOperation op = new OpOperation();
			JsonElement tp = jsonObj.remove(F_TYPE);
			if(tp != null) {
				String opType = tp.getAsString();
				op.type = opType;
			} else {
				op.type = "";
			}
			JsonElement createdObjs = jsonObj.remove(F_CREATE);
			if(createdObjs != null) {
				JsonArray ar = createdObjs.getAsJsonArray();
				for(int i = 0; i < ar.size(); i++) {
					op.addCreated(context.deserialize(ar.get(i), OpObject.class));
				}
			}

			JsonElement editedObjs = jsonObj.remove(F_EDIT);
			if (editedObjs != null) {
				for (JsonElement editElem : editedObjs.getAsJsonArray()) {
					op.addEdited(context.deserialize(editElem, OpObject.class));
				}
			}

			jsonObj.remove(F_EVAL);
			op.fields = context.deserialize(jsonObj, TreeMap.class);
			return op;
		}

		@Override
		public JsonElement serialize(OpOperation src, Type typeOfSrc, JsonSerializationContext context) {
			TreeMap<String, Object> tm = new TreeMap<>(fullOutput ? src.getMixedFieldsAndCacheMap() : src.fields);
			if(excludeHashAndSignature) {
				tm.remove(F_SIGNATURE);
				tm.remove(F_HASH);
			}
			tm.put(F_TYPE, src.type);

			if (src.hasEdited()) {
				tm.put(F_EDIT, context.serialize(src.editedObjects));
			}

			if(src.hasCreated()) {
				tm.put(F_CREATE, context.serialize(src.createdObjects));
			}
			return context.serialize(tm);
		}
	}

}