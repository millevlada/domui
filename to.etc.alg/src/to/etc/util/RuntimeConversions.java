package to.etc.util;

import java.lang.reflect.*;
import java.math.*;
import java.util.*;

/**
 * This static class contains a sh..tload of code which converts
 * runtime objects into other objects, using generic rules for doing that. This
 * code is shared between the EL interpreter, the NEMA template engine and the
 * like to allow conversion of objects to other types.
 *
 * <p>Created on May 25, 2005
 * @author <a href="mailto:jal@mumble.to">Frits Jalvingh</a>
 */
public class RuntimeConversions {
	private RuntimeConversions() {
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Conversion to primitive types.						*/
	/*--------------------------------------------------------------*/
	/**
	 * Convert an object to an integer. This accepts most of the base classes.
	 * @param o
	 * @return
	 */
	static public int convertToInt(Object o) {
		if(o == null)
			return 0;
		if(o instanceof Number)
			return ((Number) o).intValue();
		if(o instanceof Boolean)
			return ((Boolean) o).booleanValue() ? 1 : 0;
		if(o instanceof String) {
			try {
				return Integer.parseInt(((String) o).trim());
			} catch(Exception e) {
				throw new RuntimeConversionException("The string '" + o + "' cannot be converted to an integer");
			}
		}
		throw new RuntimeConversionException("Cannot convert the type \"" + o.getClass() + "\" to an integer.");
	}

	/**
	 * Convert an object to a long.
	 * @param o
	 * @return
	 */
	static public long convertToLong(Object o) {
		if(o == null)
			return 0;
		if(o instanceof Number)
			return ((Number) o).longValue();
		if(o instanceof Boolean)
			return ((Boolean) o).booleanValue() ? 1 : 0;
		if(o instanceof String) {
			try {
				return Long.parseLong(((String) o).trim());
			} catch(Exception e) {
				throw new RuntimeConversionException("The string '" + o + "' cannot be converted to a long");
			}
		}
		throw new RuntimeConversionException("Cannot convert the type \"" + o.getClass() + "\" to a long.");
	}

	/**
	 * Convert an object to a byte.
	 * @param o
	 * @return
	 */
	static public byte convertToByte(Object o) {
		if(o == null)
			return 0;
		if(o instanceof Number)
			return ((Number) o).byteValue();
		if(o instanceof Boolean)
			return ((Boolean) o).booleanValue() ? (byte) 1 : (byte) 0;
		if(o instanceof String) {
			try {
				return Byte.parseByte(((String) o).trim());
			} catch(Exception e) {
				throw new RuntimeConversionException("The string '" + o + "' cannot be converted to a byte");
			}
		}
		throw new RuntimeConversionException("Cannot convert the type \"" + o.getClass() + "\" to a byte.");
	}

	/**
	 * Convert an object to a Short.
	 * @param o
	 * @return
	 */
	static public short convertToShort(Object o) {
		if(o == null)
			return 0;
		if(o instanceof Number)
			return ((Number) o).shortValue();
		if(o instanceof Boolean)
			return ((Boolean) o).booleanValue() ? (short) 1 : (short) 0;
		if(o instanceof String) {
			try {
				return Short.parseShort(((String) o).trim());
			} catch(Exception e) {
				throw new RuntimeConversionException("The string '" + o + "' cannot be converted to a short");
			}
		}
		throw new RuntimeConversionException("Cannot convert the type \"" + o.getClass() + "\" to a short.");
	}

	/**
	 * Convert an object to a double.
	 * @param o
	 * @return
	 */
	static public double convertToDouble(Object o) {
		if(o == null)
			return 0;
		if(o instanceof Number)
			return ((Number) o).doubleValue();
		if(o instanceof Boolean)
			return ((Boolean) o).booleanValue() ? 1.0 : 0.0;
		if(o instanceof String) {
			try {
				return Double.parseDouble(((String) o).trim());
			} catch(Exception e) {
				throw new RuntimeConversionException("The string '" + o + "' cannot be converted to a double");
			}
		}
		throw new RuntimeConversionException("Cannot convert the type \"" + o.getClass() + "\" to a double.");
	}

	/**
	 * Convert an object to a char.
	 * @param o
	 * @return
	 */
	static public char convertToChar(Object o) {
		if(o == null)
			return 0;
		if(o instanceof Number)
			return (char) ((Number) o).intValue();
		if(o instanceof Boolean)
			return ((Boolean) o).booleanValue() ? 't' : 'f';
		if(o instanceof String) {
			String s = (String) o;
			if(s.length() == 0)
				return 0;
			return s.charAt(0);
		}
		throw new RuntimeConversionException("Cannot convert the type \"" + o.getClass() + "\" to a char.");
	}

	/**
	 * Convert an object to a boolean.
	 * @param o
	 * @return
	 */
	static public boolean convertToBool(Object o) {
		if(o == null)
			return false;
		if(o instanceof Boolean)
			return ((Boolean) o).booleanValue();
		if(o instanceof Number)
			return ((Number) o).intValue() != 0;
		if(o instanceof String) {
			try {
				return Boolean.parseBoolean(((String) o).trim());
			} catch(Exception e) {
				throw new RuntimeConversionException("The string '" + o + "' cannot be converted to a boolean");
			}
		}
		throw new RuntimeConversionException("Cannot convert the type \"" + o.getClass() + "\" to a boolean.");
	}


	/*--------------------------------------------------------------*/
	/*	CODING:	Object / wrapper conversions						*/
	/*--------------------------------------------------------------*/
	/**
	 * Converts a string. The null string is converted to the empty string.
	 * @param o
	 * @return
	 */
	static public String convertToString(Object o) {
		if(o == null)
			return "";
		if(o instanceof String)
			return (String) o;
		return o.toString();
	}

	static public BigDecimal convertToBigDecimal(Object in) {
		if(in == null)
			return BigDecimal.valueOf(0);

		if(in instanceof BigDecimal)
			return (BigDecimal) in;
		if(in instanceof BigInteger)
			return new BigDecimal((BigInteger) in);
		if(in instanceof Number)
			return new BigDecimal(((Number) in).doubleValue());
		throw new RuntimeConversionException(in, "BigDecimal");
	}

	static public BigInteger convertToBigInteger(Object in) {
		if(in == null)
			return BigInteger.valueOf(0);
		if(in instanceof BigInteger)
			return (BigInteger) in;
		if(in instanceof String) // String containing #
			return new BigInteger((String) in); // Coerce $ to
		if(in instanceof BigDecimal)
			return ((BigDecimal) in).toBigInteger();
		else if(in instanceof Number)
			return BigInteger.valueOf(((Number) in).longValue());
		throw new RuntimeConversionException(in, "BigInteger");
	}

	static public Double convertToDoubleWrapper(Object in) {
		if(in == null)
			return WrapperCache.getDouble(0.0);
		if(in instanceof Double)
			return (Double) in;
		if(in instanceof Number)
			return WrapperCache.getDouble(((Number) in).doubleValue());
		if(in instanceof String) {
			try {
				return Double.valueOf((String) in);
			} catch(Exception x) {}
		}
		throw new RuntimeConversionException(in, "Double");
	}

	static public Long convertToLongWrapper(Object in) {
		if(in == null)
			return WrapperCache.getLong(0);
		if(in instanceof Long)
			return (Long) in;
		if(in instanceof Number)
			return WrapperCache.getLong(((Number) in).longValue());
		if(in instanceof String) {
			try {
				return Long.valueOf((String) in);
			} catch(Exception x) {
				x.printStackTrace();
			}
		}
		throw new RuntimeConversionException(in, "Long");
	}

	static public Integer convertToIntegerWrapper(Object in) {
		if(in == null)
			return WrapperCache.getInteger(0);
		if(in instanceof Integer)
			return (Integer) in;
		if(in instanceof Number)
			return WrapperCache.getInteger(((Number) in).intValue());
		if(in instanceof String) {
			String s = ((String) in).trim();
			if(s.length() == 0)
				return WrapperCache.getInteger(0);
			try {
				int val = Integer.parseInt(s);
				return WrapperCache.getInteger(val);
			} catch(Exception x) {}
		}
		throw new RuntimeConversionException(in, "Integer");
	}

	static public Short convertToShortWrapper(Object in) {
		if(in == null)
			return WrapperCache.getShort((short) 0);
		if(in instanceof Short)
			return (Short) in;
		if(in instanceof Number)
			return WrapperCache.getShort(((Number) in).shortValue());
		if(in instanceof String) {
			try {
				return Short.valueOf((String) in);
			} catch(Exception x) {}
		}
		throw new RuntimeConversionException(in, "Short");
	}

	static public Character convertToCharacterWrapper(Object in) {
		if(in == null)
			return WrapperCache.getCharacter((char) 0);
		if(in instanceof Character)
			return (Character) in;
		if(in instanceof Number)
			return WrapperCache.getCharacter((char) ((Number) in).intValue());
		if(in instanceof String) {
			String s = (String) in;
			if(s.length() == 0)
				return WrapperCache.getCharacter((char) 0);
			return WrapperCache.getCharacter(s.charAt(0));
		}
		throw new RuntimeConversionException(in, "Character");
	}

	static public Byte convertToByteWrapper(Object in) {
		if(in == null)
			return WrapperCache.getByte((byte) 0);
		if(in instanceof Byte)
			return (Byte) in;
		if(in instanceof Number)
			return WrapperCache.getByte(((Number) in).byteValue());
		if(in instanceof String) {
			try {
				return Byte.valueOf((String) in);
			} catch(Exception x) {}
		}
		throw new RuntimeConversionException(in, "Byte");
	}

	static public Boolean convertToBooleanWrapper(Object in) {
		if(in == null)
			return Boolean.FALSE;
		if(in instanceof Boolean)
			return (Boolean) in;
		return Boolean.valueOf(convertToBool(in));
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Conversions to a user-specified type.				*/
	/*--------------------------------------------------------------*/
	/**
	 * Converts an input type to whatever type is needed.
	 */
	static public final Object convertTo(Object o, Class to) {
		if(to == Object.class || to == null)
			return o;
		if(o != null) {
			//-- Try if the class types match
			Class from = o.getClass();
			if(from == to) // Same class-> ok
				return o;
			if(to.isAssignableFrom(from)) // Can we assign 'o' to the TO class?
				return o; // Then just be done
		}

		//-- Handle all known conversions in turn, most used conversions 1st
		if(to == Integer.class || to == Integer.TYPE)
			return convertToIntegerWrapper(o);
		if(to == String.class)
			return convertToString(o);
		if(to == Boolean.class || to == Boolean.TYPE)
			return convertToBooleanWrapper(o);
		if(to == Long.class || to == Long.TYPE)
			return convertToLongWrapper(o);
		if(to == Short.class || to == Short.TYPE)
			return convertToShortWrapper(o);
		if(to == Byte.class || to == Byte.TYPE)
			return convertToByteWrapper(o);
		if(to == Character.class || to == Character.TYPE)
			return convertToCharacterWrapper(o);
		if(to == Double.class || to == Double.TYPE)
			return convertToDoubleWrapper(o);

		if(o == null && !to.isPrimitive()) // Accept null for all non-primitives
			return o;

		throw new RuntimeConversionException(o, to.getName());
	}

	static public final Object convertToComplex(Object source, Class totype) {
		if(source != null) {
			if(totype.isAssignableFrom(source.getClass()))
				return source;

			//-- Handle special objects: JSON lists assigned to arrays.
			if(totype.isArray())
				return convertToArray(totype, source);
			if(totype.isEnum())
				return convertToEnum(totype, source);

		}
		//-- As a last resort: handle basic conversions as specified by EL. This throws up if impossible
		return convertTo(source, totype);
	}

	static public Object convertToArray(Class totype, Object src) {
		if(totype.isAssignableFrom(src.getClass()))
			return src;
		Class ccl = totype.getComponentType(); // Array of what, exactly?
		Class scl = src.getClass();
		if(scl.isArray()) {
			//-- Try to convert every component of source array to dest array using complex semantics
			int len = Array.getLength(src); // #elements in source,
			Object res = Array.newInstance(ccl, len); // Create new result
			for(int i = len; --i >= 0;) { // Convert all members.
				Object val = Array.get(src, i); // Get source item
				Array.set(res, i, convertToComplex(val, ccl));
			}
			return res;
		}

		if(src instanceof Collection) {
			Collection c = (Collection) src;
			Object res = Array.newInstance(ccl, c.size()); // Create new result
			int i = 0;
			for(Object o : c) {
				Array.set(res, i++, convertToComplex(o, ccl));
			}
			return res;
		}

		throw new RuntimeConversionException(src, totype.getName());
	}


	/*--------------------------------------------------------------*/
	/*	CODING:	Iterator conversion.								*/
	/*--------------------------------------------------------------*/
	/**
	 * This creates an iterator which can iterate over the object passed. The
	 * object must contain some thing which allows for iteration. The following
	 * types are supported:
	 * <ul>
	 * 	<li>array: any array type is handled by walking over it using a generated
	 * 		ArrayIterator. If the array is of a primitive type each element of the
	 * 		array gets wrapped.</li>
	 *	<li>Container: any container returns it's iterator.</li>
	 *	<li>Map: a Map returns it's values().iterator()
	 * </ul>
	 * Any other type will throw a RuntimeConversionException.
	 * converts
	 */
	static public Iterator makeIterator(Object val) throws Exception {
		if(val == null)
			throw new RuntimeConversionException("Cannot convert null to an iterator.");
		if(val instanceof Collection)
			return ((Collection) val).iterator();
		if(val instanceof Map)
			return ((Map) val).values().iterator();
		Class acl = val.getClass();
		if(acl.isArray())
			return new ArrayIterator(val);
		throw new RuntimeConversionException("Cannot convert a " + acl.getName() + " to an iterator.");
	}

	static private final class ArrayIterator implements Iterator {
		private Object	m_array;

		private int		m_len;

		private int		m_index;

		public ArrayIterator(Object arr) {
			m_array = arr;
			m_len = Array.getLength(arr);
		}

		public boolean hasNext() {
			return m_index < m_len;
		}

		public Object next() {
			if(m_index >= m_len)
				return null;
			return Array.get(m_array, m_index++);
		}

		public void remove() {
			throw new IllegalStateException("Cannot remove items from an array");
		}
	}

	/**
	 * Returns T if the object passed can be iterated over using the makeIterator
	 * call.
	 *
	 * @param val
	 * @return
	 * @see #makeIterator(Object val)
	 */
	static public boolean isIterable(Object val) {
		if(val == null)
			return false;
		if(val instanceof Collection || val instanceof Map)
			return true;
		Class cla = val.getClass();
		if(cla.isArray())
			return true;
		return false;
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Conversions to Listable.							*/
	/*--------------------------------------------------------------*/
	static private final Listable	NO_VALUES	= new Listable() {
													public Object get(int ix) throws Exception {
														return null;
													}

													public int size() throws Exception {
														return 0;
													}
												};

	/**
	 * Converts any collection-like structure to a Listable. In addition, maps are
	 * converted to Mappable too.
	 * @param o
	 * @return
	 */
	static public Listable convertToListable(Object o) {
		if(o == null)
			return NO_VALUES;
		if(o instanceof Listable)
			return (Listable) o;
		if(o instanceof List)
			return new ListableListWrapper((List) o);
		if(o instanceof Collection) {
			Collection col = (Collection) o;
			return new ListableArrayWrapper(col.toArray());
		}
		if(o instanceof Map) {
			return new ListableMapWrapper((Map) o);
		}
		Class acl = o.getClass();
		if(acl.isArray()) {
			//-- Allow only object arrays, not thingies of primitive type
			if(acl.getComponentType().isPrimitive())
				throw new RuntimeConversionException("Cannot convert an array of a primitive type to Listable");
			return new ListableArrayWrapper((Object[]) o);
		}

		throw new RuntimeConversionException("Cannot convert a " + acl.getName() + " to a Listable.");
	}

	static private class ListableArrayWrapper implements Listable {
		private Object[]	m_val;

		public ListableArrayWrapper(Object[] val) {
			m_val = val;
		}

		public Object get(int ix) {
			return m_val[ix];
		}

		public int size() {
			return m_val.length;
		}
	}

	static private final class ListableMapWrapper extends ListableArrayWrapper implements Mappable {
		private Map	m_map;

		public ListableMapWrapper(Map m) {
			super(m.values().toArray());
			m_map = m;
		}

		public Object get(Object key) {
			return m_map.get(key);
		}

		public Iterator getKeyIterator() {
			return m_map.keySet().iterator();
		}

		public Iterator getValueIterator() {
			return m_map.values().iterator();
		}
	}

	static private final class ListableListWrapper implements Listable {
		private List	m_val;

		public ListableListWrapper(List val) {
			m_val = val;
		}

		public Object get(int ix) {
			return m_val.get(ix);
		}

		public int size() {
			return m_val.size();
		}
	}


	/*--------------------------------------------------------------*/
	/*	CODING:	Conversions to Mappable.							*/
	/*--------------------------------------------------------------*/
	static private final Mappable	EMPTY_MAP	= new Mappable() {
													public Object get(Object key) {
														return null;
													}

													public int size() {
														return 0;
													}

													public Iterator getKeyIterator() {
														throw new UnsupportedOperationException("empty mappable");
													}

													public Iterator getValueIterator() {
														throw new UnsupportedOperationException("empty mappable");
													}
												};

	static private class MapWrap implements Mappable {
		private Map	m_map;

		MapWrap(Map m) {
			m_map = m;
		}

		public Object get(Object key) {
			return m_map.get(key);
		}

		public int size() {
			return m_map.size();
		}

		public Iterator getKeyIterator() {
			return m_map.keySet().iterator();
		}

		public Iterator getValueIterator() {
			return m_map.values().iterator();
		}
	}

	static public Mappable convertToMappable(Object o) {
		if(o == null)
			return EMPTY_MAP;
		if(o instanceof Mappable)
			return (Mappable) o;
		if(o instanceof Map)
			return new MapWrap((Map) o);
		throw new RuntimeConversionException("Cannot convert a " + o.getClass().getName() + " to a Mappable.");
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Special conversions.								*/
	/*--------------------------------------------------------------*/
	static public java.util.Date convertToDate(Object o) {
		if(o == null)
			return null;
		if(o instanceof java.util.Date)
			return (java.util.Date) o;
		if(o instanceof Calendar)
			return ((Calendar) o).getTime();
		throw new RuntimeConversionException("Cannot convert a " + o.getClass().getName() + " to a java.util.Date");
	}

	static public Enum convertToEnum(Class<Enum> cl, Object o) {
		if(o == null)
			return null;
		if(o instanceof Enum)
			return (Enum) o;
		if(o instanceof String) {
			String value = (String) o;
			value = value.trim();
			Enum[] ar = cl.getEnumConstants();
			if(ar == null)
				throw new IllegalStateException("!? No enum constants for enum class " + cl.getCanonicalName());
			for(Enum en : ar) {
				if(en.name().equalsIgnoreCase(value))
					return en;
			}
			throw new IllegalStateException("The value '" + value + "' is not a valid enum name for the enum '" + cl.getCanonicalName());
		}
		throw new RuntimeConversionException("Cannot convert a " + o.getClass().getName() + " to a " + cl.getName());
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Collection and array set assignment.				*/
	/*--------------------------------------------------------------*/
	/**
	 * Returns T if this is a supported collection type. Collection
	 * types are Array, List, Set and Collection and concrete
	 * implementations of those.
	 */
	static public boolean isCollectionType(Class cl) {
		if(cl.isArray())
			return true;
		if(Collection.class.isAssignableFrom(cl)) // Is some generic collection?
			return true;
		return false;
	}

	/**
	 * Create a concrete instance of some collection type, i.e. something
	 * implementing Collection.
	 * @param colltype
	 * @return
	 */
	static public Object createConcreteCollection(Class ct) {
		if(ct.isInterface() || Modifier.isAbstract(ct.getModifiers())) {
			//-- Handle concrete cases here
			if(List.class.isAssignableFrom(ct))
				return new ArrayList();
			else if(Set.class.isAssignableFrom(ct))
				return new HashSet();
			else if(Collection.class.isAssignableFrom(ct))
				return new ArrayList();
			else
				throw new RuntimeConversionException("Unsupported abstract/interface target class " + ct.getName());
		}

		//-- Not an abstract type. Make sure it is some kind of Collection
		if(!Collection.class.isAssignableFrom(ct))
			throw new RuntimeConversionException("Cannot create a Collection instance from " + ct.getName());
		try {
			return ct.newInstance();
		} catch(Exception x) {
			throw new RuntimeConversionException("Error creating an instance of " + ct.getName() + ": " + x, x);
		}
	}


}
