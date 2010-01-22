package to.etc.domui.component.meta;

import java.util.*;

import to.etc.webapp.*;


/**
 * Utility class used as utility method library for working with meta data.
 * @author vmijic
 * Created on 4 Aug 2009
 */
public class MetaUtils {
	/**
	 * Use in component hint when rendering componet maxsize should not calculate extra space for minus sign.   
	 */
	public static final String NO_MINUS = "no-minus";

	/**
	 * Use in component hint when rendering componet maxsize should not calculate extra space for thousent separators.   
	 */
	public static final String NO_SEPARATOR = "no-separator";

	/**
	 * Use in component hint when rendering of TextArea component is required.   
	 */
	public static final String TEXT_AREA = "textarea";

	/**
	 * Use in component hint when setting default rows attribute for TextArea based components.   
	 */
	public static final String ROW = "row";

	/**
	 * Use in component hint when setting default cols attribute for TextArea based components.   
	 */
	public static final String COL = "col";

	/**
	 * Parses value of param defined in metadataLine string as Integer value.
	 * @return Integer value represented as string, in case that param is not defined or NumberFormatException exception returns default value.
	 */
	public static int parseIntParam(final String metadataLine, final String paramName, final int defaultValue) {
		String paramValue = parseStringParam(metadataLine, paramName);
		if(paramValue == null || paramValue.trim().length() == 0)
			return defaultValue;
		try {
			return Integer.parseInt(paramValue);
		} catch(NumberFormatException e) {
			throw new ProgrammerErrorException("Invalid number in metadata string: " + metadataLine + ", parameter=" + paramName + ", value=" + paramValue);
		}
	}

	public static String parseStringParam(final String metadataLine, final String paramName) {
		String paramValue = null;
		final String paramItem = paramName.toLowerCase() + '=';
		if(metadataLine.contains(paramItem)) {
			if(metadataLine.indexOf(";", metadataLine.indexOf(paramItem)) > 0) {
				paramValue = metadataLine.substring(metadataLine.indexOf(paramItem) + paramItem.length(), metadataLine.indexOf(";", metadataLine.indexOf(paramItem)));
			} else {
				paramValue = metadataLine.substring(metadataLine.indexOf(paramItem) + paramItem.length());
			}
		}

		return paramValue;
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	LookupForm and search metadata helper code.			*/
	/*--------------------------------------------------------------*/
	/**
	 *
	 * @param pl
	 * @return
	 */
	static public PropertyMetaModel findLastProperty(List<PropertyMetaModel> pl) {
		if(pl == null || pl.size() == 0)
			return null;
		return pl.get(pl.size() - 1);
	}

	static public PropertyMetaModel findLastProperty(SearchPropertyMetaModel spm) {
		return findLastProperty(spm.getPropertyPath());
	}

	static public PropertyMetaModel getLastProperty(List<PropertyMetaModel> pl) {
		PropertyMetaModel m = findLastProperty(pl);
		if(m == null)
			throw new IllegalStateException("No property in property list");
		return m;
	}

	static public PropertyMetaModel getLastProperty(SearchPropertyMetaModel spm) {
		PropertyMetaModel m = findLastProperty(spm);
		if(m == null)
			throw new IllegalStateException("The search property " + spm.getPropertyName() + " is not found");
		return m;
	}

	static public String findHintText(SearchPropertyMetaModel spm) {
		String hint = spm.getLookupHint();
		if(hint == null) {
			PropertyMetaModel pmm = findLastProperty(spm);
			if(pmm != null)
				hint = pmm.getDefaultHint();
		}
		return hint;
	}
}
