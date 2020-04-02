package it.eng.mapping;

import java.util.ArrayList;
import java.util.List;

public class Mapping {
	private String ocb_id;
	private String opcua_id;
	private String object_id;
	private List<InputArgument> inputArguments=new ArrayList<InputArgument>();
	
	public Mapping() {}

	public String getOcb_id() {
		return ocb_id;
	}

	public void setOcb_id(String ocb_id) {
		this.ocb_id = ocb_id;
	}

	public String getOpcua_id() {
		return opcua_id;
	}

	public void setOpcua_id(String opcua_id) {
		this.opcua_id = opcua_id;
	}

	public String getObject_id() {
		return object_id;
	}

	public void setObject_id(String object_id) {
		this.object_id = object_id;
	}

	public List<InputArgument> getInputArguments() {
		return inputArguments;
	}

	public void setInputArguments(List<InputArgument> inputArguments) {
		this.inputArguments = inputArguments;
	}

	@Override
	public String toString() {
		return "Mapping [ocb_id=" + ocb_id + ", opcua_id=" + opcua_id + ", object_id=" + object_id + ", inputArguments="
				+ inputArguments + "]";
	}
	
	
}
