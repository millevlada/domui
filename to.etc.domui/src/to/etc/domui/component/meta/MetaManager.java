package to.etc.domui.component.meta;

import java.lang.reflect.*;
import java.math.*;
import java.util.*;

import to.etc.domui.component.input.*;
import to.etc.domui.component.meta.impl.*;
import to.etc.domui.dom.html.*;
import to.etc.domui.server.*;
import to.etc.domui.util.*;
import to.etc.webapp.*;
import to.etc.webapp.nls.*;

/**
 * Accessor class to the generalized metadata thingies.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Jun 16, 2008
 */
final public class MetaManager {
	static private List<DataMetaModel> m_modelList = new ArrayList<DataMetaModel>();

	static private Set<Class< ? >> SIMPLE = new HashSet<Class< ? >>();

	static private Map<Class< ? >, DefaultClassMetaModel> m_classMap = new HashMap<Class< ? >, DefaultClassMetaModel>();

	private MetaManager() {}

	static synchronized public void registerModel(DataMetaModel model) {
		List<DataMetaModel> mm = new ArrayList<DataMetaModel>(m_modelList);
		mm.add(model);
		m_modelList = mm;
	}

	static private synchronized List<DataMetaModel> getList() {
		if(m_modelList.size() == 0)
			registerModel(new DefaultDataMetaModel());
		return m_modelList;
	}

	static public ClassMetaModel findClassMeta(Class< ? > clz) {
		DefaultClassMetaModel dmm;
		synchronized(MetaManager.class) {
			dmm = m_classMap.get(clz);
			if(dmm == null) {
				if(clz.getName().contains("$$")) {
					//-- Enhanced class (Hibernate). Get base class instead
					clz = clz.getSuperclass();
					dmm = m_classMap.get(clz);
				}
				if(dmm == null) {
					dmm = new DefaultClassMetaModel(clz); // Create base class info
					m_classMap.put(clz, dmm); // Save
				}
			}
		}

		//-- Double lock mechanism externalized to prevent long locks on central metadata table
		synchronized(dmm) {
			if(!dmm.isInitialized()) {
				for(DataMetaModel mm : getList()) { // Let all providers add their information.
					mm.updateClassMeta(dmm);
				}

				//-- Finalize all properties.
				for(PropertyMetaModel pmm : dmm.getProperties())
					finalizeProperty(pmm);

				dmm.initialized();
			}
		}
		return dmm;
	}

	/**
	 * Finalizes the metamodel for a property when all metadata providers have had their
	 * go.
	 *
	 * @param pmm
	 */
	private static void finalizeProperty(PropertyMetaModel pmm) {
	}

	static public PropertyMetaModel findPropertyMeta(Class< ? > clz, String name) {
		ClassMetaModel cm = findClassMeta(clz);
		if(cm == null)
			return null;
		return cm.findProperty(name);
	}

	static public PropertyMetaModel getPropertyMeta(Class< ? > clz, String name) {
		PropertyMetaModel pmm = findPropertyMeta(clz, name);
		if(pmm == null)
			throw new ProgrammerErrorException("The property '" + clz.getName() + "." + name + "' is not known.");
		return pmm;
	}

	static public boolean isSimpleClass(Class< ? > clz) {
		return SIMPLE.contains(clz);
	}

	/**
	 * Handles the permission sets like "viewpermission" and "editpermission". If
	 * the array contains null the field can be seen by all users. If it has a value
	 * the first-level array is a set of ORs; the second level are ANDs. Meaning that
	 * an array in the format:
	 * <pre>
	 * { {"admin"}
	 * , {"editroles", "user"}
	 * , {"tester"}
	 * };
	 * </pre>
	 * this means that the field is visible for a user with the roles:
	 * <pre>
	 * 	"admin" OR "tester" OR ("editroles" AND "user")
	 * </pre>
	 *
	 * @param roleset
	 * @param ctx
	 * @return
	 */
	static public boolean isAccessAllowed(String[][] roleset, IRequestContext ctx) {
		if(roleset == null)
			return true; // No restrictions
		for(String[] orset : roleset) {
			boolean ok = true;
			for(String perm : orset) {
				if(!ctx.hasPermission(perm)) {
					ok = false;
					break;
				}
			}
			//-- Were all "AND" conditions true then accept
			if(ok)
				return true;
		}
		return false;
	}

	static private INodeContentRenderer< ? > createComboLabelRenderer(Class< ? extends ILabelStringRenderer< ? >> lsr) {
		final ILabelStringRenderer<Object> lr = (ILabelStringRenderer<Object>) DomApplication.get().createInstance(lsr);
		return new INodeContentRenderer<Object>() {
			public void renderNodeContent(NodeBase component, NodeContainer node, Object object, Object parameters) {
				String text = lr.getLabelFor(object);
				if(text != null)
					node.setText(text);
			}
		};
	}

	static private boolean hasDisplayProperties(List<DisplayPropertyMetaModel> list) {
		return list != null && list.size() > 0;
	}

	static private INodeContentRenderer< ? > TOSTRING_RENDERER = new INodeContentRenderer<Object>() {
		public void renderNodeContent(NodeBase component, NodeContainer node, Object object, Object parameters) {
			if(object != null)
				node.setText(object.toString());
		}
	};

	/**
	 * This creates a default combo option value renderer using whatever metadata is available.
	 * @param pmm	If not-null this takes precedence. This then <b>must</b> be the property that
	 * 				is to be filled from the list-of-values in the combo. The property is used
	 * 				to override the presentation only. Formally speaking, pmm.getActualType() must
	 * 				be equal to the combo's list item type, and the renderer expects items of that
	 * 				type.
	 *
	 * @param cmm
	 * @return
	 */
	static public INodeContentRenderer< ? > createDefaultComboRenderer(PropertyMetaModel pmm, ClassMetaModel cmm) {
		//-- Property-level metadata is the 1st choice
		if(pmm != null) {
			cmm = MetaManager.findClassMeta(pmm.getActualType()); // Always use property's class model.

			if(pmm.getComboNodeRenderer() != null)
				return DomApplication.get().createInstance(pmm.getComboNodeRenderer());
			if(pmm.getComboLabelRenderer() != null)
				return createComboLabelRenderer(pmm.getComboLabelRenderer());

			/*
			 * Check for Display properties on both the property itself or on the target class, and use the 1st one
			 * found. The names in these properties refer to the names in the /target/ class,
			 * so they do not contain the name of "this" property; so we need the classmodel of the target type and
			 * the idempotent accessor.
			 */
			List<DisplayPropertyMetaModel> dpl = pmm.getComboDisplayProperties(); // From property;
			if(!hasDisplayProperties(dpl))
				dpl = cmm.getComboDisplayProperties(); // Try the class then;
			if(hasDisplayProperties(dpl)) {
				/*
				 * The property has DisplayProperties.
				 */
				List<ExpandedDisplayProperty> xpl = ExpandedDisplayProperty.expandDisplayProperties(dpl, cmm, null);
				return new DisplayPropertyNodeContentRenderer(cmm, xpl);
			}
			return TOSTRING_RENDERER; // Just tostring it..
		}

		/*
		 * If a ClassMetaModel is present this means the value object will have that ClassMetaModel.
		 */
		if(cmm != null) {
			if(cmm.getComboNodeRenderer() != null)
				return DomApplication.get().createInstance(cmm.getComboNodeRenderer());
			if(cmm.getComboLabelRenderer() != null)
				return createComboLabelRenderer(cmm.getComboLabelRenderer());

			if(hasDisplayProperties(cmm.getComboDisplayProperties())) {
				/*
				 * The value class has display properties; expand them. Since this is the value class we need no root accessor (== identity/same accessor).
				 */
				List<ExpandedDisplayProperty> xpl = ExpandedDisplayProperty.expandDisplayProperties(cmm.getComboDisplayProperties(), cmm, null);
				return new DisplayPropertyNodeContentRenderer(cmm, xpl);
			}
		}
		return TOSTRING_RENDERER; // Just tostring it..
	}

	/**
	 * This is a complex EQUAL routine which compares objects. Each of the objects can be null. Objects
	 * are considered equal when they are the same reference; if a.equal(b) holds or, when the objects
	 * are both objects for which a PK is known, when the PK's are equal.
	 * @param a
	 * @param b
	 * @param cmm
	 * @return
	 */
	static public boolean areObjectsEqual(Object a, Object b, ClassMetaModel cmm) {
		if(a == b)
			return true;
		if(a == null || b == null)
			return false;
		if(a.equals(b))
			return true;

		//-- Classes must be the same type but we allow for proxying
		Class< ? > acl = a.getClass();
		Class< ? > bcl = b.getClass();
		if(!acl.isAssignableFrom(bcl) && !bcl.isAssignableFrom(acl))
			return false;

		//-- Try Object key identity, if available
		if(cmm == null)
			cmm = findClassMeta(a.getClass());
		if(cmm.getPrimaryKey() != null) {
			try {
				Object pka = cmm.getPrimaryKey().getAccessor().getValue(a);
				Object pkb = cmm.getPrimaryKey().getAccessor().getValue(b);
				return DomUtil.isEqual(pka, pkb);
			} catch(Exception x) {
				x.printStackTrace();
				return false;
			}
		}
		return false;
	}

	static public boolean areObjectsEqual(Object a, Object b) {
		return areObjectsEqual(a, b, null);
	}


	/**
	 * Locate the enum's default label.
	 * @param <T>
	 * @param val
	 * @return
	 */
	static public <T extends Enum< ? >> String findEnumLabel(T val) {
		if(val == null)
			return null;
		ClassMetaModel cmm = findClassMeta(val.getClass());
		return cmm.getDomainLabel(NlsContext.getLocale(), val);
	}


	/**
	 * Creates a List of Pair's for each domain value in a class which represents a domain (like an enum or Boolean). The
	 * list is ready to be used by ComboFixed.
	 * @param clz
	 * @return
	 */
	static public <T extends Enum< ? >> List<ValueLabelPair<T>> createEnumList(Class<T> clz) {
		List<ValueLabelPair<T>> res = new ArrayList<ValueLabelPair<T>>();
		ClassMetaModel cmm = MetaManager.findClassMeta(clz);
		Object[] values = cmm.getDomainValues();
		if(values == null)
			throw new IllegalStateException("The class " + clz + " does not have a discrete list of Domain Values");
		for(Object value : values) {
			String label = cmm.getDomainLabel(NlsContext.getLocale(), value);
			if(label == null)
				label = value == null ? "" : value.toString();
			res.add(new ValueLabelPair<T>((T) value, label));
		}
		return res;
	}

	static public PropertyMetaModel internalCalculateDottedPath(ClassMetaModel cmm, String name) {
		int pos = name.indexOf('.'); // Dotted name?
		if(pos == -1)
			return cmm.findSimpleProperty(name); // Use normal resolution directly on the class.

		//-- We must create a synthetic property.
		int ix = 0;
		int len = name.length();
		ClassMetaModel ccmm = cmm; // Current class meta-model for property reached
		List<PropertyMetaModel> acl = new ArrayList<PropertyMetaModel>(10);
		for(;;) {
			String sub = name.substring(ix, pos); // Get path component,
			ix = pos + 1;

			PropertyMetaModel pmm = ccmm.findSimpleProperty(sub); // Find base property,
			if(pmm == null)
				throw new IllegalStateException("Undefined property '" + sub + "' on classMetaModel=" + ccmm);
			acl.add(pmm); // Next access path,
			ccmm = MetaManager.findClassMeta(pmm.getActualType());

			if(ix >= len)
				break;
			pos = name.indexOf('.', ix);
			if(pos == -1)
				pos = len;
		}

		//-- Resolved to target. Return a complex proxy.
		return new PathPropertyMetaModel<Object>(name, acl.toArray(new PropertyMetaModel[acl.size()]));
	}

	/**
	 * Parse the property path and return the list of properties in the path. This explicitly allows
	 * traversing child relations provided generic type information is present to denote the child's type.
	 * @param m
	 * @param compoundName
	 * @return
	 */
	static public List<PropertyMetaModel> parsePropertyPath(ClassMetaModel m, String compoundName) {
		int ix = 0;
		int len = compoundName.length();
		List<PropertyMetaModel> res = new ArrayList<PropertyMetaModel>();
		ClassMetaModel cmm = m;
		while(ix < len) {
			int pos = compoundName.indexOf('.', ix);
			String name;
			if(pos == -1) {
				name = compoundName.substring(ix); // Last segment
				ix = len;
			} else {
				name = compoundName.substring(ix, pos); // Intermediary segment,
				ix = pos + 1;
			}

			PropertyMetaModel pmm = cmm.findSimpleProperty(name);
			if(pmm == null)
				throw new MetaModelException(Msgs.BUNDLE, Msgs.MM_COMPOUND_PROPERTY_NOT_FOUND, compoundName, name, cmm.toString());

			//-- If this is a child property it represents some collection; use the collection's type as next thing.
			if(pmm.getRelationType() == PropertyRelationType.DOWN || Collection.class.isAssignableFrom(pmm.getActualType()) || pmm.getActualType().isArray()) {
				ClassMetaModel nextmm = null;

				//-- This must be some kind of collectable subtype or we're in sh*t.
				Type vtype = pmm.getGenericActualType();
				if(vtype != null) {
					Class< ? > vclass = findCollectionType(vtype); // Try to get value class type.
					if(vclass != null)
						nextmm = findClassMeta(vclass);
				}
				if(nextmm == null && ix >= len)
					throw new MetaModelException(Msgs.BUNDLE, Msgs.MM_UNKNOWN_COLLECTION_TYPE, compoundName, name, vtype);
				cmm = nextmm;
			} else {
				cmm = MetaManager.findClassMeta(pmm.getActualType());
			}
			res.add(pmm);
		}
		return res;
	}

	/**
	 * This tries to determine the value class for a property defined as some kind
	 * of Collection&lt;T&gt; or T[]. If the type cannot be determined this returns
	 * null.
	 *
	 * @param genericType
	 * @return
	 */
	static public Class< ? > findCollectionType(Type genericType) {
		if(genericType instanceof Class<?>) {
			Class<?> cl = (Class<?>) genericType;
			if(cl.isArray()) {
				return cl.getComponentType();
			}
		}
		if(genericType instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) genericType;
			Type raw = pt.getRawType();

			//-- This must be a collection type of class.
			if(raw instanceof Class< ? >) {
				Class< ? > cl = (Class< ? >) raw;
				if(Collection.class.isAssignableFrom(cl)) {
					Type[] tar = pt.getActualTypeArguments();
					if(tar != null && tar.length == 1) { // Collection<T> required
						return (Class< ? >) tar[0];
					}
				}
			}
		}
		return null;
	}

	/**
	 * Returns T if instance.propertyname is a duplicate in some other instance in the list.
	 * @param <T>
	 * @param items
	 * @param instance
	 * @param propertyname
	 * @return
	 * @throws Exception
	 */
	static public <T> boolean hasDuplicates(List<T> items, T instance, String propertyname) throws Exception {
		ClassMetaModel cmm = findClassMeta(instance.getClass());
		PropertyMetaModel pmm = getPropertyMeta(instance.getClass(), propertyname);
		Object vi = pmm.getAccessor().getValue(instance);
		ClassMetaModel vcmm = vi == null ? null : findClassMeta(vi.getClass());
		for(T v : items) {
			if(areObjectsEqual(instance, v, cmm))
				continue;
			Object vl = pmm.getAccessor().getValue(v);
			if(areObjectsEqual(vi, vl, vcmm)) {
				return true;
			}
		}
		return false;
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Generic data model utility functions.				*/
	/*--------------------------------------------------------------*/
	/**
	 *
	 * @param t
	 * @return
	 */
	static public String identify(Object t) {
		if(t == null)
			return "null";
		ClassMetaModel cmm = MetaManager.findClassMeta(t.getClass());
		if(cmm.isPersistentClass() && cmm.getPrimaryKey() != null) {
			try {
				Object k = cmm.getPrimaryKey().getAccessor().getValue(t);
				return t.getClass().getName() + "#" + k + " @" + System.identityHashCode(t);
			} catch(Exception x) {}
		}
		return t.toString() + " @" + System.identityHashCode(t);
	}

	/**
	 * Return the primary key field for a given instance. This throws IllegalArgumentException's when the
	 * instance passed is not persistent or has an unknown primary key. If the primary key is just null
	 * this returns null.
	 *
	 * @param instance
	 * @return
	 */
	static public Object getPrimaryKey(Object instance) throws Exception {
		return getPrimaryKey(instance, null);
	}

	static public Object getPrimaryKey(Object instance, ClassMetaModel cmm) throws Exception {
		if(instance == null)
			throw new IllegalArgumentException("Instance cannot be null");
		if(cmm == null)
			cmm = findClassMeta(instance.getClass());
		if(! cmm.isPersistentClass())
			throw new IllegalArgumentException("The instance " + identify(instance) + " is not a persistent class");
		PropertyMetaModel pmm = cmm.getPrimaryKey();
		if(pmm == null)
			throw new IllegalArgumentException("The instance " + identify(instance) + " has an undefined primary key (cannot be obtained by metadata)");
		return pmm.getAccessor().getValue(instance);
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Expanding properties.								*/
	/*--------------------------------------------------------------*/

	static {
		SIMPLE = new HashSet<Class< ? >>();
		SIMPLE.add(Integer.class);
		SIMPLE.add(Integer.TYPE);
		SIMPLE.add(Long.class);
		SIMPLE.add(Long.TYPE);
		SIMPLE.add(Character.class);
		SIMPLE.add(Character.TYPE);
		SIMPLE.add(Short.class);
		SIMPLE.add(Short.TYPE);
		SIMPLE.add(Byte.class);
		SIMPLE.add(Byte.TYPE);
		SIMPLE.add(Double.class);
		SIMPLE.add(Double.TYPE);
		SIMPLE.add(Float.class);
		SIMPLE.add(Float.TYPE);
		SIMPLE.add(Boolean.class);
		SIMPLE.add(Boolean.TYPE);
		SIMPLE.add(BigDecimal.class);
		SIMPLE.add(String.class);
		SIMPLE.add(BigInteger.class);
	}

}
