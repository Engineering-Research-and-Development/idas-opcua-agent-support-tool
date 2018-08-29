import org.opcfoundation.ua.builtintypes.DataValue;

public class OpcUaNode {
	private String nodeId;
	private String type;
	private String typeDefinition;
	private String dataType;
	private DataValue[] value;
	private String name;
	private String displayName;

	public OpcUaNode() {}

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	
	public DataValue[] getValue() {
		return value;
	}

	public void setValue(DataValue[] value) {
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	

	

	@Override
	public String toString() {
		return "OpcUaNode [nodeId=" + nodeId + ", type=" + type + ", typeDefinition=" + typeDefinition + ", dataType="
				+ dataType + ", value=" + value + ", name=" + name + ", displayName=" + displayName + "]";
	}

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getTypeDefinition() {
		return typeDefinition;
	}

	public void setTypeDefinition(String typeDefinition) {
		this.typeDefinition = typeDefinition;
	}
	
	
}
