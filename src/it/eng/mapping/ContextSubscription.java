package it.eng.mapping;

import java.util.ArrayList;
import java.util.List;

public class ContextSubscription {
	private String id;
	private String type;
	private List<Mapping> mappings=new ArrayList<Mapping>();
	
	public ContextSubscription() {}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<Mapping> getMappings() {
		return mappings;
	}

	public void setMappings(List<Mapping> mappings) {
		this.mappings = mappings;
	}
	
	
	
}
