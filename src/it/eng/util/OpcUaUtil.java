package it.eng.util;

import java.util.HashMap;
import java.util.Map;

public class OpcUaUtil {

	private Map<String, Integer> dataTypes=new HashMap<String, Integer>(){/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

	{
	    put("Null", 0);
	    put("Boolean", 1);
	    put("SByte", 2);
	    put("Byte", 3);
	    put("Int16", 4);
	    put("UInt16", 5);
	    put("Int32", 6);
	    put("UInt32", 7);
	    put("Int64", 8);
	    put("UInt64", 9);
	    put("Float", 10);
	    put("Double", 11);
	    put("String", 12);
	    put("DateTime", 13);
	    put("Guid", 14);
	    put("ByteString", 15);
	    put("XmlElement", 16);
	    put("NodeId", 17);
	    put("ExpandedNodeId", 18);
	    put("StatusCode", 19);
	    put("QualifiedName", 20);
	    put("LocalizedText", 21);
	    put("ExtensionObject", 22);
	    put("DataValue", 23);
	    put("Variant", 24);
	    put("DiagnosticInfo", 25);

	}};
	
	public OpcUaUtil() {}

	public Map<String, Integer> getDataTypes() {
		return dataTypes;
	}

	public void setDataTypes(Map<String, Integer> dataTypes) {
		this.dataTypes = dataTypes;
	}
	
	
	
}
