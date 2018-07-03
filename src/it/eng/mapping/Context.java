package it.eng.mapping;

import java.util.ArrayList;
import java.util.List;

public class Context {
	
	private String id;
	private String type;
	private String service;
	private String subservice;
	private Boolean polling;
	private List<Mapping> mappings=new ArrayList<Mapping>();
	
	public Context() {}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	
	public Boolean getPolling() {
		return polling;
	}

	public void setPolling(Boolean polling) {
		this.polling = polling;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public String getSubservice() {
		return subservice;
	}

	public void setSubservice(String subservice) {
		this.subservice = subservice;
	}

	public List<Mapping> getMappings() {
		return mappings;
	}

	public void setMappings(List<Mapping> mappings) {
		this.mappings = mappings;
	}
	
	
	
}
