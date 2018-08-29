package it.eng.mapping;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class TypeDetails {
	private String service;
	private String subservice;
	private List<Attribute> active=new ArrayList<Attribute>();
	private List<Attribute> lazy=new ArrayList<Attribute>();
	private List<Attribute> commands=new ArrayList<Attribute>();
	@JsonIgnore
	private String nodeId;
	
	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public TypeDetails() {}

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

	public List<Attribute> getActive() {
		return active;
	}

	public void setActive(List<Attribute> active) {
		this.active = active;
	}

	public List<Attribute> getLazy() {
		return lazy;
	}

	public void setLazy(List<Attribute> lazy) {
		this.lazy = lazy;
	}

	public List<Attribute> getCommands() {
		return commands;
	}

	public void setCommands(List<Attribute> commands) {
		this.commands = commands;
	}
	
	
}
