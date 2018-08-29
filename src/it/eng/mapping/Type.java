package it.eng.mapping;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = TypeSerializer.class)
public class Type {
	private String name;
	@JsonIgnore
	private String nodeId;
	private TypeDetails typeDetails=new TypeDetails();
	
	public Type() {}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public TypeDetails getTypeDetails() {
		return typeDetails;
	}

	public void setTypeDetails(TypeDetails typeDetails) {
		this.typeDetails = typeDetails;
	}

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}
	
	
	

}
