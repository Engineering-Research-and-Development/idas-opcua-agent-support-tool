package it.eng.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public  class PropertiesUtil {
	
	
	private List<Integer> namespaceIgnore=new ArrayList<Integer>();
	private String contextBrokerHost;


	private String contextBrokerPort;
	private String serverBaseRoot;
	private String serverPort;
	private String deviceRegistryType;
	private String mongodbHost;
	private String mongodbPort;
	private String mongodbDb;
	private int mongodbRetries;
	private int mongodbRetryTime;
	private String fiwareService;
	private String fiwareServicePath;
	private String providerUrl;
	private String deviceRegistrationDuration;
	private String logLevel;
	private String pollingExpiration;
	private String pollingDaemonFrequency;
	private String polling;



	private Map<String, String> dataTypeMapping=new HashMap<String, String>();
	
	
	

	public PropertiesUtil() {}
	
	public int analyzePropertiesFile(String filename) {
		try {
		Properties prop = new Properties();
		  prop.load(new BufferedReader(new FileReader(filename)));
		/*Properties prop = new Properties();
		InputStream input = null;

		try {
			
		//	input = new FileInputStream("config.properties");
			//String filename = "/config.properties";
    		input = PropertiesUtil.class.getClassLoader().getResourceAsStream(filename);
    		if(input==null){
    	            System.out.println("Sorry, unable to find " + filename);
    		    return -1;
    		}
    			prop.load(input);*/
			// get the property value and print it out
			String [] nss=prop.getProperty("namespace-ignore").split(",");
			for (String ns:nss) {
				namespaceIgnore.add(Integer.parseInt(ns));
			}
			contextBrokerHost=prop.getProperty("context-broker-host");
			logLevel=prop.getProperty("log-level");
			contextBrokerPort=prop.getProperty("context-broker-port");
			setServerBaseRoot(prop.getProperty("server-base-root"));
			serverPort=prop.getProperty("server-port");
			deviceRegistryType=prop.getProperty("device-registry-type");
			mongodbHost=prop.getProperty("mongodb-host");
			mongodbPort=prop.getProperty("mongodb-port");
			mongodbDb=prop.getProperty("mongodb-db");
			mongodbRetries=Integer.parseInt(prop.getProperty("mongodb-retries"));
			mongodbRetryTime=Integer.parseInt(prop.getProperty("mongodb-retry-time"));
			fiwareService=prop.getProperty("fiware-service");
			fiwareServicePath=prop.getProperty("fiware-service-path");
			providerUrl=prop.getProperty("provider-url");
			deviceRegistrationDuration=prop.getProperty("device-registration-duration");
			pollingExpiration=prop.getProperty("pollingExpiration");
			pollingDaemonFrequency=prop.getProperty("pollingDaemonFrequency");
			polling=prop.getProperty("polling");
			
			
			for (Enumeration<?> e = prop.propertyNames(); e.hasMoreElements(); ) {
			    String name = (String)e.nextElement();
			    String value = prop.getProperty(name);
			    // now you have name and value
			    if (name.startsWith("OPC-datatype")) {
			        // this is the OPC-datatype name. Write yor code here
			    		dataTypeMapping.put(name, value);
			    }
			}
			

		} catch (Exception ex) {
			//ex.printStackTrace();
			return -1;
		} finally {
			/*if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}*/
		}

		return 0;
	}

	public List<Integer> getNamespaceIgnore() {
		return namespaceIgnore;
	}

	public void setNamespaceIgnore(List<Integer> namespaceIgnore) {
		this.namespaceIgnore = namespaceIgnore;
	}
	
	public String getContextBrokerHost() {
		return contextBrokerHost;
	}

	public void setContextBrokerHost(String contextBrokerHost) {
		this.contextBrokerHost = contextBrokerHost;
	}

	public String getContextBrokerPort() {
		return contextBrokerPort;
	}

	public void setContextBrokerPort(String contextBrokerPort) {
		this.contextBrokerPort = contextBrokerPort;
	}

	

	public String getServerPort() {
		return serverPort;
	}

	public void setServerPort(String serverPort) {
		this.serverPort = serverPort;
	}

	public String getMongodbHost() {
		return mongodbHost;
	}

	public void setMongodbHost(String mongodbHost) {
		this.mongodbHost = mongodbHost;
	}

	public String getMongodbPort() {
		return mongodbPort;
	}

	public void setMongodbPort(String mongodbPort) {
		this.mongodbPort = mongodbPort;
	}

	public String getMongodbDb() {
		return mongodbDb;
	}

	public void setMongodbDb(String mongodbDb) {
		this.mongodbDb = mongodbDb;
	}

	public int getMongodbRetries() {
		return mongodbRetries;
	}

	public void setMongodbRetries(int mongodbRetries) {
		this.mongodbRetries = mongodbRetries;
	}

	public int getMongodbRetryTime() {
		return mongodbRetryTime;
	}

	public void setMongodbRetryTime(int mongodbRetryTime) {
		this.mongodbRetryTime = mongodbRetryTime;
	}

	public String getFiwareService() {
		return fiwareService;
	}

	public void setFiwareService(String fiwareService) {
		this.fiwareService = fiwareService;
	}

	public String getFiwareServicePath() {
		return fiwareServicePath;
	}

	public void setFiwareServicePath(String fiwareServicePath) {
		this.fiwareServicePath = fiwareServicePath;
	}

	public String getProviderUrl() {
		return providerUrl;
	}

	public void setProviderUrl(String providerUrl) {
		this.providerUrl = providerUrl;
	}
	
	public String getPollingDaemonFrequency() {
		return pollingDaemonFrequency;
	}

	public void setPollingDaemonFrequency(String pollingDaemonFrequency) {
		this.pollingDaemonFrequency = pollingDaemonFrequency;
	}
	
	public String getPollingExpiration() {
		return pollingExpiration;
	}

	public void setPollingExpiration(String pollingExpiration) {
		this.pollingExpiration = pollingExpiration;
	}

	public String getDeviceRegistrationDuration() {
		return deviceRegistrationDuration;
	}

	public void setDeviceRegistrationDuration(String deviceRegistrationDuration) {
		this.deviceRegistrationDuration = deviceRegistrationDuration;
	}

	public String getDeviceRegistryType() {
		return deviceRegistryType;
	}

	public void setDeviceRegistryType(String deviceRegistryType) {
		this.deviceRegistryType = deviceRegistryType;
	}

	public String getServerBaseRoot() {
		return serverBaseRoot;
	}

	public void setServerBaseRoot(String serverBaseRoot) {
		this.serverBaseRoot = serverBaseRoot;
	}
	
	public Map<String, String> getDataTypeMapping() {
		return dataTypeMapping;
	}

	public void setDataTypeMapping(Map<String, String> dataTypeMapping) {
		this.dataTypeMapping = dataTypeMapping;
	}

	public String getLogLevel() {
		return logLevel;
	}

	public void setLogLevel(String logLevel) {
		this.logLevel = logLevel;
	}
	
	public String getPolling() {
		return polling;
	}

	public void setPolling(String polling) {
		this.polling = polling;
	}
}
