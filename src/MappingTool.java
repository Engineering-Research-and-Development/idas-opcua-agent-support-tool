import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.opcfoundation.ua.application.Application;
import org.opcfoundation.ua.application.Client;
import org.opcfoundation.ua.application.Session;
import org.opcfoundation.ua.application.SessionChannel;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.NodeId;

import org.opcfoundation.ua.common.ServiceFaultException;
import org.opcfoundation.ua.common.ServiceResultException;

import org.opcfoundation.ua.core.Argument;
import org.opcfoundation.ua.core.BrowseDescription;
import org.opcfoundation.ua.core.BrowseDirection;
import org.opcfoundation.ua.core.BrowseResponse;
import org.opcfoundation.ua.core.BrowseResult;
import org.opcfoundation.ua.core.BrowseResultMask;
import org.opcfoundation.ua.core.EndpointDescription;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.MessageSecurityMode;
import org.opcfoundation.ua.core.NodeClass;

import org.opcfoundation.ua.core.ReferenceDescription;
import org.opcfoundation.ua.transport.SecureChannel;
import org.opcfoundation.ua.transport.security.Cert;
import org.opcfoundation.ua.transport.security.KeyPair;
import org.opcfoundation.ua.transport.security.PrivKey;
import org.opcfoundation.ua.utils.CertificateUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalified.tree.TreeNode;
import com.scalified.tree.multinode.ArrayMultiTreeNode;

import it.eng.mapping.Attribute;
import it.eng.mapping.Configuration;
import it.eng.mapping.Context;
import it.eng.mapping.ContextBroker;
import it.eng.mapping.ContextSubscription;
import it.eng.mapping.DeviceRegistry;
import it.eng.mapping.InputArgument;
import it.eng.mapping.Mapping;
import it.eng.mapping.Mongodb;
import it.eng.mapping.Server;
import it.eng.mapping.TypeDetails;
import it.eng.util.OpcUaUtil;
import it.eng.util.PropertiesUtil;

public class MappingTool {
	private static final String PRIVKEY_PASSWORD = "Opc.Ua";

	private static final String ENDPOINT_KEY = "-e";
	private static final String FILENAME_KEY = "-f";

	private static final String USERNAME = "-u";
	private static final String PASSWORD = "-p";


	private static PropertiesUtil propertiesUtil=new PropertiesUtil();

	private static Map<String, String> dataTypes=new HashMap<String, String>();
	static Logger logger = LogManager.getLogger(MappingTool.class.getName());



	public static void main(String[] args) throws ServiceResultException, IOException {
		// TODO Auto-generated method stub
		logger.info("Welcome to ENGINEERING INGEGNERIA INFORMATICA FIWARE OPC UA AGENT MAPPING TOOL");
		try {




			String url = "";
			String filename = "";
			String username = "";
			String password = "";

			for(int i=0; i<args.length; i+=2)
			{
				String key = args[i];
				String value = args[i+1];


				switch (key)
				{
				case ENDPOINT_KEY : url = value; break;
				case FILENAME_KEY : filename = value;break;
				case USERNAME : username = value;break;
				case PASSWORD : password = value;break;

				}
			}
			if (url.isEmpty()) {
				logger.debug("No endpoint specified.");
				return;
			}

			SessionChannel mySession = null;
			Client myClient;
			EndpointDescription endpoint;

			Application myClientApplication = new Application();
			// Load Client's Application Instance Certificate from file
			KeyPair myClientApplicationInstanceCertificate = null;

			// Create Client Application Instance Certificate
			{
				String certificateCommonName = "OPC UA Mapping Tool";
				//System.out.println("Generating new Certificate for Client using CN: " + certificateCommonName);
				//ring applicationUri = myClientApplication.getApplicationUri();
				String applicationUri = myClientApplication.getApplicationUri();
				String organisation = "Engineering Ingegneria Informatica S.P.A.";
				int validityTime = 365;
				myClientApplicationInstanceCertificate = CertificateUtils
						.createApplicationInstanceCertificate(certificateCommonName, organisation, applicationUri, validityTime);
			}


			// Create Client
			myClient = new Client(myClientApplication);
			myClientApplication.addApplicationInstanceCertificate(myClientApplicationInstanceCertificate);
			//////////////////////////////////////


			// Create the client using information provided by the created certificate

			/////////// DISCOVER ENDPOINT ////////
			// Discover server's endpoints, and choose one
			EndpointDescription[] endpoints = myClient.discoverEndpoints(url);//51210=Sample



			//Localhost handling 
			final URL urlStructure = new URL("http://"+url.substring(10));
			final String host = urlStructure.getHost();
			for (EndpointDescription ep:endpoints) {
				if (ep.getEndpointUrl().contains("localhost")) {
					logger.debug("changing localhost with "+host);
					ep.setEndpointUrl(ep.getEndpointUrl().replace("localhost",host));
				}
			}                  
			// Server
			// Filter out all but opc.tcp protocol endpoints
			//endpoints = EndpointUtil.selectByProtocol(endpoints, "opc.tcp");
			// Filter out all but Signed & Encrypted endpoints

			//endpoints = EndpointUtil.selectByMessageSecurityMode(endpoints, MessageSecurityMode.SignAndEncrypt);

			// Filter out all but Basic128 cryption endpoints
			//endpoints = EndpointUtil.selectBySecurityPolicy(endpoints, SecurityPolicy.BASIC256);

			// Sort endpoints by security level. The lowest level at the beginning, the highest at the end
			// of the array
			//endpoints = EndpointUtil.sortBySecurityLevel(endpoints);
			// Choose one endpoint
			endpoint = endpoints[endpoints.length - 1];

			//////////////////////////////////////

			///////////// EXECUTE //////////////
			if(username!="" && password!="") {
				SecureChannel secureChannel = myClient.createSecureChannel(endpoint);
				Session mySession2 = myClient.createSession(secureChannel);
				mySession2.getEndpoint().setServerCertificate(endpoint.getServerCertificate());
				mySession = mySession2.createSessionChannel(secureChannel, myClient);
				mySession.activate(username,password);
			}
			else {


				try {
					EndpointDescription endpointNoSecured=null;
					for (EndpointDescription ep:endpoints) {
						if (ep.getSecurityMode()==MessageSecurityMode.None) {
							endpointNoSecured=ep;
							break;
						}
					}
					if (endpointNoSecured==null)
					{
						logger.debug("Cannot open the OPC UA session");
						System.exit(1);
					}
					SecureChannel mySecureChannel = myClient.createSecureChannel(url, endpointNoSecured);
					Session mySession2 = myClient.createSession(mySecureChannel);
					mySession = mySession2.createSessionChannel(mySecureChannel, myClient);
					mySession.activate();
				}
				catch (Exception exc) {
					//exc.printStackTrace();
					if (mySession==null) {
						logger.debug("Cannot open the OPC UA session");
						System.exit(1);
					}
				}

				/*
				 * try { mySession = myClient.createSessionChannel(endpoint);
				 * mySession.activate(); }catch (ServiceResultException s) { try { mySession =
				 * myClient.createSessionChannel(endpoints[0]); mySession.activate(); } catch
				 * (Exception e) { try { EndpointDescription endpointNoSecured=null; for
				 * (EndpointDescription ep:endpoints) { if
				 * (ep.getSecurityMode()==MessageSecurityMode.None) { endpointNoSecured=ep;
				 * break; } } if (endpointNoSecured==null) {
				 * logger.debug("Cannot open the OPC UA session"); System.exit(1); }
				 * SecureChannel mySecureChannel = myClient.createSecureChannel(url,
				 * endpointNoSecured); Session mySession2 =
				 * myClient.createSession(mySecureChannel); mySession =
				 * mySession2.createSessionChannel(mySecureChannel, myClient);
				 * mySession.activate(); } catch (Exception exc) { //exc.printStackTrace(); if
				 * (mySession==null) { logger.debug("Cannot open the OPC UA session");
				 * System.exit(1); } } }
				 * 
				 * }
				 */


			}
			//////////////////////////////////////
			// Browse Root
			/*BrowseDescription browse = new BrowseDescription();
browse.setNodeId(Identifiers.RootFolder);
browse.setBrowseDirection(BrowseDirection.Forward);
browse.setIncludeSubtypes(true);
browse.setNodeClassMask(NodeClass.Object, NodeClass.Variable);
browse.setResultMask(BrowseResultMask.All);
BrowseResponse res3 = mySession.Browse(null, null, null, browse);
for (BrowseResult res:res3.getResults()) {

	for (ReferenceDescription ref:res.getReferences()) {
		logger.debug(ref.getBrowseName());
	}
}*/




			if (propertiesUtil.analyzePropertiesFile(filename)!=0) {
				logger.debug("EXIT");
				System.exit(1);
			}


			Configuration configuration=new Configuration();
			configuration.setLogLevel(propertiesUtil.getLogLevel());
			ContextBroker contextBroker=new ContextBroker();
			contextBroker.setHost(propertiesUtil.getContextBrokerHost());
			contextBroker.setPort(Integer.parseInt(propertiesUtil.getContextBrokerPort()));
			configuration.setContextBroker(contextBroker);
			Server server=new Server();
			server.setBaseRoot(propertiesUtil.getServerBaseRoot());
			server.setPort(Integer.parseInt(propertiesUtil.getServerPort()));
			configuration.setServer(server);
			DeviceRegistry deviceRegistry=new DeviceRegistry();
			deviceRegistry.setType(propertiesUtil.getDeviceRegistryType());
			configuration.setDeviceRegistry(deviceRegistry);
			Mongodb mongodb=new Mongodb();
			mongodb.setHost(propertiesUtil.getMongodbHost());
			mongodb.setPort(propertiesUtil.getMongodbPort());
			mongodb.setDb(propertiesUtil.getMongodbDb());
			mongodb.setRetries(propertiesUtil.getMongodbRetries());
			mongodb.setRetryTime(propertiesUtil.getMongodbRetryTime());
			configuration.setMongodb(mongodb);
			configuration.setService(propertiesUtil.getFiwareService());
			configuration.setSubservice(propertiesUtil.getFiwareServicePath());
			configuration.setProviderUrl(propertiesUtil.getProviderUrl());
			configuration.setPollingDaemonFrequency(propertiesUtil.getPollingDaemonFrequency());
			configuration.setPollingExpiration(propertiesUtil.getPollingExpiration());
			configuration.setDeviceRegistrationDuration(propertiesUtil.getDeviceRegistrationDuration());


			//Nodes Filtering IN
			if (propertiesUtil.getNodesFilteringIn()!=null) {
				for (String nodeFiltering:propertiesUtil.getNodesFilteringIn().split(",")) {
					configuration.getNodesFilteringIn().add(nodeFiltering);
				}
			}
			//Nodes Filtering OUT
			if (propertiesUtil.getNodesFilteringOut()!=null) {
				for (String nodeFiltering:propertiesUtil.getNodesFilteringOut().split(",")) {
					configuration.getNodesFilteringOut().add(nodeFiltering);
				}
			}


			ObjectMapper mapper = new ObjectMapper();


			//Object to JSON in String
			String jsonInString = mapper.writeValueAsString(configuration);

			if (propertiesUtil.getConfiguration()!=null) {
				if (propertiesUtil.getConfiguration().equalsIgnoreCase("api")) {
					TypeDetails typeDetails=new TypeDetails();
					typeDetails.setActive(new ArrayList<Attribute>());
					typeDetails.setService(propertiesUtil.getFiwareService());
					typeDetails.setSubservice(propertiesUtil.getFiwareServicePath());
					configuration.getTypes().put("Device", typeDetails);
					logger.debug("**************************FINAL***************************");
					logger.debug(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(configuration));
					try (FileWriter file = new FileWriter("conf/config.json")) {
						file.write(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(configuration));
						logger.info("Successfully Copied JSON Object to File...");
						file.close();
						///////////// SHUTDOWN /////////////
						//Close channel
						mySession.closeAsync();
						//////////////////////////////////////
						System.exit(0);


					}catch(Exception e) {
						e.printStackTrace();
						System.exit(1);
					}


				}
			}









			logger.debug("********************************************************************");
			logger.debug(jsonInString);


			/*// Browse Root
BrowseDescription browse = new BrowseDescription();
browse.setNodeId(NodeId.parseNodeId("ns=4;i=1132"));
browse.setBrowseDirection(BrowseDirection.Forward);
browse.setIncludeSubtypes(true);
browse.setNodeClassMask(NodeClass.Object, NodeClass.Variable);
browse.setResultMask(BrowseResultMask.All);
BrowseResponse res3 = mySession.Browse(null, null, null, browse);
for (BrowseResult res:res3.getResults()) {
	for (ReferenceDescription ref:res.getReferences()) {
		logger.debug("ref="+ref.toString());

	}
}
			 */




			if (dataTypes.size()==0) {
				BrowseDescription browse = new BrowseDescription();
				browse.setNodeId(Identifiers.BaseDataType);
				browse.setBrowseDirection(BrowseDirection.Forward);
				browse.setIncludeSubtypes(true);

				browse.setNodeClassMask(NodeClass.DataType);
				browse.setResultMask(BrowseResultMask.All);
				BrowseResponse res3 = mySession.Browse(null, null, null, browse);
				for (BrowseResult res:res3.getResults()) {
					for (ReferenceDescription ref:res.getReferences()) 			
					{
						String childNodeId=ref.getNodeId().toString();//"ns="+ref.getNodeId().getNamespaceIndex()+";i="+ref.getNodeId().getValue().toString();
						if (childNodeId.split(";").length==1)
							childNodeId="ns="+0+";"+childNodeId.split(";")[0];
						else
							childNodeId="ns="+ref.getNodeId().getNamespaceIndex()+";"+childNodeId.split(";")[1];
						dataTypes.put(childNodeId,ref.getBrowseName().toString());
						checkTypes(NodeId.parseNodeId(childNodeId), mySession);

					}			
				}
			}





			logger.debug("************** OBJECT FOLDER ****************");


			//String rootFolder=Identifiers.RootFolder.toString();
			//NodeId nodeIdRoot=NodeId.parseNodeId(rootFolder);

			TreeNode<OpcUaNode> objectTree = new ArrayMultiTreeNode<>(new OpcUaNode());

			AddressSpaceBrowsing addressSpaceBrowsing=new AddressSpaceBrowsing(filename);
			addressSpaceBrowsing.browse(Identifiers.ObjectsFolder,mySession,0, 6, objectTree);
			//String typesFolder="ns=4;i=1132";
			//NodeId nodeIdTypes=NodeId.parseNodeId(typesFolder);

			/*
logger.debug("************** ITERATING OBJECT TREE ****************");

//Iterating over the tree elements using foreach
for (TreeNode<OpcUaNode> node : objectTree.root()) {

	logger.debug(node.level()+") "+node.data()); // any other action goes here
}
			 */

			logger.debug("************** ITERATING CLEAN OBJECT TREE ****************");


			//Iterating over the tree elements using foreach
			for (TreeNode<OpcUaNode> node : objectTree.subtrees()) {



				List<TreeNode<OpcUaNode>> methods=new ArrayList<TreeNode<OpcUaNode>>();
				// Iterating over the tree elements using Iterator
				Iterator<TreeNode<OpcUaNode>> iterator = node.iterator();
				while (iterator.hasNext()) {
					TreeNode<OpcUaNode> child = iterator.next();
					//Fetch all methods in order to remove them from subtrees
					if (child.data().getType().equalsIgnoreCase("method")) {
						methods.add(child);
					}
				}




				/*//Remove methods from subtrees
			for (TreeNode<OpcUaNode> child : methods) {
				//node.remove(child);

			}*/


				//Iterate methods
				for (TreeNode<OpcUaNode> method : methods) {
					for (TreeNode<OpcUaNode> child : method) {

						if (child.level()>0) {

							if (child.data().getType().equalsIgnoreCase("Variable")) {
								logger.debug("-------) "+child.data()); // any other action goes here

							}
							//logger.debug("child) "+child.data()); // any other action goes here
						}
						else
							logger.debug("METHOD) "+child.data()); // any other action goes here
					}
				}

				/*//Iterating over the tree elements using foreach
			for (TreeNode<OpcUaNode> child : node) {
				if (child.level()>1) {

					if (child.data().getType().equalsIgnoreCase("variable")) {
						logger.debug("----) "+child.data()); // any other action goes here

					}
					//logger.debug("child) "+child.data()); // any other action goes here
				}
				else
					logger.debug("-) "+child.data()); // any other action goes here

			}*/

			}





			logger.debug("************** TYPES FOLDER ****************");

			//Creating the tree nodes
			TreeNode<OpcUaNode> typeTree = new ArrayMultiTreeNode<>(new OpcUaNode());



			addressSpaceBrowsing.browseType(/*Identifiers.TypesFolder*/Identifiers.BaseObjectType,mySession,0, 5, typeTree);
			boolean noTypes=true;
			if (typeTree.isLeaf()) {
				logger.debug("##############   NO TYPES   ###############");
				noTypes=true;
			}
			else {
				noTypes=false;
				/*logger.debug("************** ITERATING TYPES TREE ****************");

			//Iterating over the tree elements using foreach
			for (TreeNode<OpcUaNode> node : typeTree.root()) {

				logger.debug(node.level()+") "+node.data()); // any other action goes here
			}*/


			}



			//Iterate on datatype mapping
			logger.debug("##############   DATATYPE MAPPING ITERATION   ###############");
			for (String key:propertiesUtil.getDataTypeMapping().keySet()) {
				logger.debug("["+key+"]="+propertiesUtil.getDataTypeMapping().get(key));

			}



			logger.debug("***objectTree) "+objectTree.toString());
			//NO TYPES DEFINED

			if (noTypes) {

				//List<Type> types= new ArrayList<Type>();




				//Iterating over the tree elements using foreach
				for (TreeNode<OpcUaNode> node : objectTree.subtrees()) {

					logger.debug("***NODE) "+node.toString());


					/*List<TreeNode<OpcUaNode>> methods=new ArrayList<TreeNode<OpcUaNode>>();
				// Iterating over the tree elements using Iterator
				Iterator<TreeNode<OpcUaNode>> iterator = node.iterator();
				while (iterator.hasNext()) {
					TreeNode<OpcUaNode> child = iterator.next();
					//Fetch all methods in order to remove them from subtrees
					if (child.data().getType().equalsIgnoreCase("method")) {
						methods.add(child);
					}
				}




				//Remove methods from subtrees
				for (TreeNode<OpcUaNode> child : methods) {
					node.remove(child);

				}

					 */

					Context context=new Context();
					context.setPolling(propertiesUtil.getPolling());
					ContextSubscription cs=new ContextSubscription();
					//Type type=new Type();
					//type.getTypeDetails().setService(propertiesUtil.getFiwareService());
					//type.getTypeDetails().setSubservice(propertiesUtil.getFiwareServicePath());
					//Iterating over the tree elements using foreach
					String objectName=null;
					//	String objectId=null;
					//String methodId=null;
					for (TreeNode<OpcUaNode> child : node) {

						if (child.level()>1) {
							logger.debug("--) "+child.data());
							if ((child.data().getType().equalsIgnoreCase("variable"))&&(!child.parent().data().getType().equalsIgnoreCase("method"))) {
								//Filtering
								if (configuration.getNodesFilteringOut().contains(child.data().getNodeId())) {
									logger.debug("--FILTER OUT--) "+child.data().getNodeId()); 
									continue;
								}
								if ((configuration.getNodesFilteringIn().size()>0)&(!configuration.getNodesFilteringIn().contains(child.data().getNodeId()))) {
									logger.debug("--FILTER IN ENABLED NOT IN LIST--) "+child.data().getNodeId()); 
									continue;
								}

								// any other action goes here
								String objectPrefix=getPrefixByChild(mySession, child); 
								Attribute attribute=new Attribute();
								attribute.setName(objectPrefix+child.data().getName());
								if (propertiesUtil.getDataTypeMapping().get("OPC-datatype-"+child.data().getDataType())!=null)
									attribute.setType(propertiesUtil.getDataTypeMapping().get("OPC-datatype-"+child.data().getDataType()));
								else
									attribute.setType(child.data().getDataType());


								//type.getTypeDetails().getActive().add(attribute);
								configuration.getTypes().get(objectName).getActive().add(attribute);

								Mapping mapping=new Mapping();
								mapping.setOpcua_id(child.data().getNodeId());
								mapping.setOcb_id(objectPrefix+child.data().getName());
								context.getMappings().add(mapping);
								logger.debug("***mapping2="+mapping.toString());



							}

							if ((child.data().getType().equalsIgnoreCase("variable"))&&(child.parent().data().getType().equalsIgnoreCase("method"))) {
								//Filtering
								if (configuration.getNodesFilteringOut().contains(child.data().getNodeId())) {
									logger.debug("--FILTER OUT--) "+child.data().getNodeId()); 
									continue;
								}
								if ((configuration.getNodesFilteringIn().size()>0)&(!configuration.getNodesFilteringIn().contains(child.data().getNodeId()))) {
									logger.debug("--FILTER IN ENABLED NOT IN LIST--) "+child.data().getNodeId()); 
									continue;
								}

								logger.debug("--MV--) "+child.data()); // any other action goes here
								ContextSubscription contextSubscription=null;
								for (ContextSubscription csLoop:configuration.getContextSubscriptions()) {
									if (csLoop.getId().equalsIgnoreCase(propertiesUtil.getPrefix()+objectName)) {
										//logger.debug("found");
										contextSubscription=csLoop;
										break;
									}
								}
								Mapping mapping=null;
								for (Mapping mp:contextSubscription.getMappings()) {
									if (mp.getOpcua_id().equalsIgnoreCase(child.parent().data().getNodeId())) {
										//logger.debug("found MP");
										mapping=mp;
										break;
									}
								}


								DataValue[] dataValues=child.data().getValue();
								if (dataValues!=null) {
									for (DataValue dataValue:dataValues) {
										Argument[] inputArguments=(Argument[]) dataValue.getValue().getValue();

										for (int i=0; i<inputArguments.length; i++ ){
											//logger.debug("inputArguments["+i+"]="+inputArguments[i].getName());
											String childNodeId=null;
											String identifier=inputArguments[i].getDataType().toString();
											if (identifier.contains(";")) {
												identifier=identifier.split(";")[1];
											}


											childNodeId="ns="+inputArguments[i].getDataType().getNamespaceIndex()+";"+identifier;
											OpcUaUtil opcUaUtil=new OpcUaUtil();
											logger.debug("test="+dataTypes.get(childNodeId));
											InputArgument inputArgument=new InputArgument();
											inputArgument.setDataType(opcUaUtil.getDataTypes().get(dataTypes.get(childNodeId)));
											inputArgument.setType(inputArguments[i].getName());

											mapping.getInputArguments().add(inputArgument);
										}
									}
								}
								/*for (child.data().getValue())
							InputArgument inputArgument=new InputArgument();
							inputArgument.setType(child.data().);


							mapping.getInputArguments().add(inputArgument);*/

							}

							if (child.data().getType().equalsIgnoreCase("object")){
								//objectId=child.data().getNodeId();
							}

							if (child.data().getType().equalsIgnoreCase("method")) {
								//methodId=child.data().getNodeId();
								logger.debug("--M--) "+child.data()); // any other action goes here
								String objectPrefix=getPrefixByChild(mySession, child); 
								Attribute attribute=new Attribute();
								attribute.setName(objectPrefix+child.data().getName());
								attribute.setType("command");
								//type.getTypeDetails().getActive().add(attribute);
								configuration.getTypes().get(objectName).getCommands().add(attribute);

								//	ContextSubscription cs=new ContextSubscription();
								boolean foundContextSub=false;
								for (ContextSubscription csLoop:configuration.getContextSubscriptions()) {
									if (csLoop.getId().equalsIgnoreCase(propertiesUtil.getPrefix()+objectName)) {
										cs=csLoop;
										foundContextSub=true;
										break;
									}
								}
								cs.setId(propertiesUtil.getPrefix()+objectName);
								cs.setType(objectName);


								Mapping mapping=new Mapping();
								mapping.setOcb_id(objectPrefix+child.data().getName());
								mapping.setObject_id(child.parent().data().getNodeId());
								mapping.setOpcua_id(child.data().getNodeId());

								cs.getMappings().add(mapping);
								logger.debug("***mapping3="+mapping.toString());

								if (!foundContextSub)
									configuration.getContextSubscriptions().add(cs);

							}
							logger.debug("child) "+child.data()); // any other action goes here
						}
						else {
							//type.setName(child.data().getName());
							objectName=child.data().getName();
							//objectId=child.data().getNodeId();
							context.setId(propertiesUtil.getPrefix()+child.data().getName());
							context.setType(child.data().getName());
							context.setService(propertiesUtil.getFiwareService());
							context.setSubservice(propertiesUtil.getFiwareServicePath());

							configuration.getTypes().put(child.data().getName(), new TypeDetails());
							configuration.getTypes().get(child.data().getName()).setNodeId(child.data().getNodeId());

							configuration.getTypes().get(child.data().getName()).setService(propertiesUtil.getFiwareService());
							configuration.getTypes().get(child.data().getName()).setSubservice(propertiesUtil.getFiwareServicePath());


							logger.debug("-) "+child.data()); // any other action goes here
						}
					}
					//configuration.getTypes().add(type);
					logger.debug("Added the following mappings:");
					for (Mapping mapping:context.getMappings()) {
						logger.debug("* nodeId="+mapping.getOpcua_id()+" ---> ngsiId="+mapping.getOcb_id());
					}
					configuration.getContexts().add(context);

				}




				logger.debug("**************************FINAL***************************");
				logger.debug(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(configuration));

				try (FileWriter file = new FileWriter("./conf/config.json")) {
					file.write(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(configuration));
					logger.info("Successfully Copied JSON Object to File...");
				}

				//configuration.setTypes(types);
			}
			//TYPES DEFINED 
			else {


				List<String> nodeIdsTypes=new ArrayList<String>();
				List<String> nodeNamesTypes=new ArrayList<String>();

				List<String> nodeIdsObjects=new ArrayList<String>();

				logger.debug("***typeTree) "+typeTree.toString());
				//Iterating over the tree elements using foreach
				for (TreeNode<OpcUaNode> node : typeTree.subtrees()) {

					logger.debug("***NODE1) "+node.toString());

					//Iterating over the tree elements using foreach
					String objectName=null;
					String objectId=null;



					for (TreeNode<OpcUaNode> child : node) {





						if (child.level()>1) {






							if ((child.data().getType().equalsIgnoreCase("variable"))&&(!child.parent().data().getType().equalsIgnoreCase("method"))) {
								//Filtering
								if (configuration.getNodesFilteringOut().contains(child.data().getNodeId())) {
									logger.debug("--FILTER OUT--) "+child.data().getNodeId()); 
									continue;
								}
								if ((configuration.getNodesFilteringIn().size()>0)&(!configuration.getNodesFilteringIn().contains(child.data().getNodeId()))) {
									logger.debug("--FILTER IN ENABLED NOT IN LIST--) "+child.data().getNodeId()); 
									continue;
								}
								logger.debug("-VARNOTMETH---) "+child.data()); // any other action goes here

								String objectPrefix=getPrefixByChild(mySession, child); 

								if (nodeIdsTypes.contains(objectPrefix+child.data().getName())) {
									logger.debug("cutting Type "+objectPrefix+child.data().getName());

									continue;
								}
								Attribute attribute=new Attribute();
								attribute.setName(objectPrefix+child.data().getName());
								if (propertiesUtil.getDataTypeMapping().get("OPC-datatype-"+child.data().getDataType())!=null)
									attribute.setType(propertiesUtil.getDataTypeMapping().get("OPC-datatype-"+child.data().getDataType()));
								else
									attribute.setType(child.data().getDataType());
								//type.getTypeDetails().getActive().add(attribute);
								configuration.getTypes().get(objectName).getActive().add(attribute);

								nodeIdsTypes.add(objectPrefix+child.data().getName());

							}





							if (child.data().getType().equalsIgnoreCase("object")){
								objectId=child.data().getNodeId();
							}


							if ((child.data().getType().equalsIgnoreCase("variable"))&&(child.parent().data().getType().equalsIgnoreCase("method"))) {
								//Filtering
								if (configuration.getNodesFilteringOut().contains(child.data().getNodeId())) {
									logger.debug("--FILTER OUT--) "+child.data().getNodeId()); 
									continue;
								}
								if ((configuration.getNodesFilteringIn().size()>0)&(!configuration.getNodesFilteringIn().contains(child.data().getNodeId()))) {
									logger.debug("--FILTER IN ENABLED NOT IN LIST--) "+child.data().getNodeId()); 
									continue;
								}
								logger.debug("--MV--) "+child.data()); // any other action goes here
								ContextSubscription contextSubscription=null;
								for (ContextSubscription cs:configuration.getContextSubscriptions()) {
									if (cs.getId().equalsIgnoreCase(propertiesUtil.getPrefix()+objectName)) {
										logger.debug("found");
										contextSubscription=cs;
										break;
									}
								}
								Mapping mapping=null;
								if (contextSubscription!=null) {
									for (Mapping mp:contextSubscription.getMappings()) {
										if (mp.getObject_id().equalsIgnoreCase(objectId)) {
											logger.debug("found MP");
											mapping=mp;
										}
									}
								}


								DataValue[] dataValues=child.data().getValue();
								if (dataValues!=null) {
									for (DataValue dataValue:dataValues) {
										Argument[] inputArguments=(Argument[]) dataValue.getValue().getValue();

										for (int i=0; i<inputArguments.length; i++ ){
											logger.debug("inputArguments["+i+"]="+inputArguments[i].getName());
											String childNodeId=null;
											String identifier=inputArguments[i].getDataType().toString();
											if (identifier.contains(";")) {
												identifier=identifier.split(";")[1];
											}


											childNodeId="ns="+inputArguments[i].getDataType().getNamespaceIndex()+";"+identifier;
											OpcUaUtil opcUaUtil=new OpcUaUtil();
											logger.debug("test="+dataTypes.get(childNodeId));
											InputArgument inputArgument=new InputArgument();
											inputArgument.setDataType(opcUaUtil.getDataTypes().get(dataTypes.get(childNodeId)));
											inputArgument.setType(inputArguments[i].getName());

											mapping.getInputArguments().add(inputArgument);
										}
									}
								}
								/*for (child.data().getValue())
							InputArgument inputArgument=new InputArgument();
							inputArgument.setType(child.data().);


							mapping.getInputArguments().add(inputArgument);*/

							}






							if (child.data().getType().equalsIgnoreCase("method")) {
								//methodId=child.data().getNodeId();
								logger.debug("--M--) "+child.data()); // any other action goes here

								String objectPrefix=getPrefixByChild(mySession, child); 

								if (nodeIdsTypes.contains(objectPrefix+child.data().getName())) {
									logger.debug("cutting Type "+objectPrefix+child.data().getName());

									continue;
								}



								//String objectPrefix=getPrefixByChild(mySession, child); 
								Attribute attribute=new Attribute();
								attribute.setName(objectPrefix+child.data().getName());
								attribute.setType("command");
								//type.getTypeDetails().getActive().add(attribute);
								configuration.getTypes().get(objectName).getCommands().add(attribute);
								nodeIdsTypes.add(objectPrefix+child.data().getName());



							}
							logger.debug("child) "+child.data()); // any other action goes here















							/*if (child.data().getType().equalsIgnoreCase("variable")) {
							logger.debug("----) "+child.data()); // any other action goes here
							String objectPrefix=getPrefixByChild(mySession, child); 

							if (nodeIdsTypes.contains(objectPrefix+child.data().getName())) {
								logger.debug("cutting Type "+objectPrefix+child.data().getName());

								continue;
							}
							Attribute attribute=new Attribute();
							attribute.setName(objectPrefix+child.data().getName());
							attribute.setType(child.data().getDataType());
							//type.getTypeDetails().getActive().add(attribute);
							configuration.getTypes().get(objectName).getActive().add(attribute);

							nodeIdsTypes.add(objectPrefix+child.data().getName());


						}*/
							logger.debug("child) "+child.data()); // any other action goes here
						}
						else {
							//type.setName(child.data().getName());
							objectName=child.data().getName();
							
							nodeNamesTypes.add(objectName);

							configuration.getTypes().put(child.data().getName(), new TypeDetails());
							configuration.getTypes().get(child.data().getName()).setNodeId(child.data().getNodeId());

							configuration.getTypes().get(child.data().getName()).setService(propertiesUtil.getFiwareService());
							configuration.getTypes().get(child.data().getName()).setSubservice(propertiesUtil.getFiwareServicePath());


							logger.debug("-) "+child.data()); // any other action goes here
						}
					}


				}






				//Iterating over the tree elements using foreach
				for (TreeNode<OpcUaNode> nodeObject : objectTree.subtrees()) {
					logger.debug("***nodeObject1) "+nodeObject.toString());
					Context context=new Context();
					String objectName=null;
					String objectType=null; 
					//Iterating over the tree elements using foreach
					for (TreeNode<OpcUaNode> child : nodeObject) {
						//Cambio logica
						logger.debug("***2="+child.toString());

						if ((child.data().getTypeDefinition()!=null)&&(context.getType()==null)&&(nodeNamesTypes.contains(child.data().getTypeDefinition()))) {
							context=new Context();


							if ((child.parent().data().getTypeDefinition()!=null)&&(nodeNamesTypes.contains(child.parent().data().getTypeDefinition()))) {
								logger.debug("Parent has a type known...ignore child...");
								continue;
							}



							context.setType(child.data().getTypeDefinition() );		
							context.setId(propertiesUtil.getPrefix()+child.data().getName());
							context.setService(propertiesUtil.getFiwareService());
							context.setSubservice(propertiesUtil.getFiwareServicePath());
							for (TreeNode<OpcUaNode> child2 : child) {
								logger.debug("***child2) "+child2);

								if (child2.data().getType().equalsIgnoreCase("variable")) {
									logger.debug("--var) "+child2.data()); // any other action goes here


									String objectPrefix=getPrefixByChild(mySession, child2); 
									if (nodeIdsObjects.contains(objectPrefix+child2.data().getName())) {
										logger.debug("cutting Obj "+objectPrefix+child2.data().getName());

										continue;
									}



									// any other action goes here
									objectPrefix=getPrefixByChild(mySession, child); 
									Attribute attribute=new Attribute();
									attribute.setName(objectPrefix+child.data().getName());
									if (propertiesUtil.getDataTypeMapping().get("OPC-datatype-"+child.data().getDataType())!=null)
										attribute.setType(propertiesUtil.getDataTypeMapping().get("OPC-datatype-"+child.data().getDataType()));
									else
										attribute.setType(child.data().getDataType());


									//type.getTypeDetails().getActive().add(attribute);
									configuration.getTypes().get(objectName).getActive().add(attribute);

									Mapping mapping=new Mapping();
									mapping.setOpcua_id(child2.data().getNodeId());
									mapping.setOcb_id(objectPrefix+child2.data().getName());
									context.getMappings().add(mapping);
									nodeIdsObjects.add(objectPrefix+child2.data().getName());
									logger.debug("***mapping4="+mapping.toString());


								}






							}
							//Add context to configuration
							configuration.getContexts().add(context);
							logger.debug("Added the following mappings:");
							for (Mapping mapping:context.getMappings()) {
								logger.debug("* nodeId="+mapping.getOpcua_id()+" ---> ngsiId="+mapping.getOcb_id());
							}
							context=new Context();
							nodeIdsObjects.clear();
						}


						if (child.level()>1) {
							if (child.data().getType().equalsIgnoreCase("method")) {
								//methodId=child.data().getNodeId();
								logger.debug("***--M--) "+child.data()); // any other action goes here


								ContextSubscription cs=new ContextSubscription();

								cs.setId(propertiesUtil.getPrefix()+objectName);
								cs.setType(objectType);
								String objectPrefix=getPrefixByChild(mySession, child); 

								Mapping mapping=new Mapping();
								mapping.setOcb_id(objectPrefix+child.data().getName());
								mapping.setObject_id(child.parent().data().getNodeId());
								mapping.setOpcua_id(child.data().getNodeId());

								cs.getMappings().add(mapping);
								logger.debug("***mapping1="+mapping.toString());



								configuration.getContextSubscriptions().add(cs);

							}



							if (child.data().getType().equalsIgnoreCase("variable")) {
								logger.debug("--var) "+child.data()); // any other action goes here


								String objectPrefix=getPrefixByChild(mySession, child); 
								if (nodeIdsObjects.contains(objectPrefix+child.data().getName())) {
									logger.debug("cutting Obj "+objectPrefix+child.data().getName());

									continue;
								}

								// any other action goes here
								objectPrefix=getPrefixByChild(mySession, child); 
								Attribute attribute=new Attribute();
								attribute.setName(objectPrefix+child.data().getName());
								if (propertiesUtil.getDataTypeMapping().get("OPC-datatype-"+child.data().getDataType())!=null)
									attribute.setType(propertiesUtil.getDataTypeMapping().get("OPC-datatype-"+child.data().getDataType()));
								else
									attribute.setType(child.data().getDataType());


								//type.getTypeDetails().getActive().add(attribute);
								configuration.getTypes().get(objectName).getActive().add(attribute);


								Mapping mapping=new Mapping();
								mapping.setOpcua_id(child.data().getNodeId());
								mapping.setOcb_id(objectPrefix+child.data().getName());
								context.getMappings().add(mapping);
								nodeIdsObjects.add(objectPrefix+child.data().getName());
								logger.debug("***mapping5="+mapping.toString());


							}
							if (child.data().getType().equalsIgnoreCase("object")) {
								logger.debug("--obj) "+child.data()); // any other action goes here

								if ((child.data().getTypeDefinition()!=null)&&(context.getType()==null))
									context.setType(child.data().getTypeDefinition() );
								if ((child.data().getTypeDefinition().equalsIgnoreCase(context.getType())))
									context.setId(child.data().getName());


							}

							logger.debug("child) "+child.data()); // any other action goes here

						}else {
							/*
							 * if (nodeNamesTypes.contains(child.data().getTypeDefinition())) {
							 * objectName=child.data().getName();
							 * objectType=child.data().getTypeDefinition(); }
							 */

							objectName=child.data().getName();
							objectType=child.data().getTypeDefinition();
							nodeNamesTypes.add(objectName);
							
							context.setId(propertiesUtil.getPrefix()+child.data().getName());
							context.setType(child.data().getName());
							context.setService(propertiesUtil.getFiwareService());
							context.setSubservice(propertiesUtil.getFiwareServicePath());


							configuration.getTypes().put(child.data().getName(), new TypeDetails());
							configuration.getTypes().get(child.data().getName()).setNodeId(child.data().getNodeId());

							configuration.getTypes().get(child.data().getName()).setService(propertiesUtil.getFiwareService());
							configuration.getTypes().get(child.data().getName()).setSubservice(propertiesUtil.getFiwareServicePath());


						}
						/*else {
						//type.setName(child.data().getName());
						context.setId(child.data().getName());


						context.setType(child.data().getTypeDefinition() );
						context.setService(propertiesUtil.getFiwareService());
						context.setSubservice(propertiesUtil.getFiwareServicePath());


						logger.debug("-) "+child.data()); // any other action goes here
					}*/
					}

					//configuration.getContexts().add(context);
					//configuration.getTypes().add(type);
					configuration.getContexts().add(context);
					logger.debug("Added the following mappings:");
					for (Mapping mapping:context.getMappings()) {
						logger.debug("* nodeId="+mapping.getObject_id()+" ---> ngsiId="+mapping.getOcb_id());
					}

				}





				logger.debug("**************************FINAL***************************");
				logger.info(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(configuration));
				try (FileWriter file = new FileWriter("conf/config.json")) {
					file.write(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(configuration));
					logger.info("Successfully Copied JSON Object to File...");
				}

				//configuration.setTypes(types);

			}



			///////////// SHUTDOWN /////////////
			//Close channel
			mySession.closeAsync();
			//////////////////////////////////////
			System.exit(0);


		}catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	private static String getPrefixByChild( SessionChannel session, TreeNode<OpcUaNode> child) throws ServiceFaultException, ServiceResultException {
		// TODO Auto-generated method stub
		boolean log=false;
		boolean isVariable=false;
		if (child.parent()!=null) {
			if (child.parent().data().getType().equalsIgnoreCase("Variable"))
				isVariable=true;

		}
		if (child.data()!=null) {
			if (child.data().getName().contains("xTP")){
				log=true;
			}
		}
		String prefix="";

		while ((child.parent()!=null)&&(child.parent().level()>1)) {
			//Get DisplayName
			//String name=getAttribute(session, child.data().getNodeId(),Attributes.DisplayName);

			if (child.parent().data()!=null){
				if (log)
					if (child.parent().data().getTypeDefinition()!=null) {

						if (child.parent().parent()!=null){

							if (isVariable==false) {
								if (child.parent().parent().data().getTypeDefinition()==null) {
									if (log)
										child=child.parent();
									continue;
								}
							}
						}

					}
			}

			prefix=child.parent().data().getName()+"_"+prefix;

			child=child.parent();
		}
		return prefix;
	}
	/**
	 * Load file certificate and private key from applicationName.der & .pfx - or create ones if they do not exist
	 * @return the KeyPair composed of the certificate and private key
	 * @throws ServiceResultException
	 */
	public static KeyPair getCert(String applicationName)
			throws ServiceResultException
	{
		File certFile = new File(applicationName + ".der");
		File privKeyFile =  new File(applicationName+ ".pem");
		try {
			Cert myCertificate = Cert.load( certFile );
			PrivKey myPrivateKey = PrivKey.load( privKeyFile, PRIVKEY_PASSWORD );
			return new KeyPair(myCertificate, myPrivateKey); 
		} catch (CertificateException e) {
			throw new ServiceResultException( e );
		} catch (IOException e) {		
			try {
				String hostName = InetAddress.getLocalHost().getHostName();
				String applicationUri = "urn:"+hostName+":"+applicationName;
				KeyPair keys = CertificateUtils.createApplicationInstanceCertificate(applicationName, null, applicationUri, 3650, hostName);
				keys.getCertificate().save(certFile);
				keys.getPrivateKey().save(privKeyFile);
				return keys;
			} catch (Exception e1) {
				throw new ServiceResultException( e1 );
			}
		} catch (NoSuchAlgorithmException e) {
			throw new ServiceResultException( e );
		} catch (InvalidKeyException e) {
			throw new ServiceResultException( e );
		} catch (InvalidKeySpecException e) {
			throw new ServiceResultException( e );
		} catch (NoSuchPaddingException e) {
			throw new ServiceResultException( e );
		} catch (InvalidAlgorithmParameterException e) {
			throw new ServiceResultException( e );
		} catch (IllegalBlockSizeException e) {
			throw new ServiceResultException( e );
		} catch (BadPaddingException e) {
			throw new ServiceResultException( e );
		} catch (InvalidParameterSpecException e) {
			throw new ServiceResultException( e );
		}
	}


	/*private static String getAttribute(SessionChannel mySession, String nodeId, UnsignedInteger attribute) throws ServiceFaultException, ServiceResultException {
		// Read DataType Test
		NodeId nodeVar=NodeId.parseNodeId(nodeId);
		ReadResponse res5 = mySession.Read(null, null, TimestampsToReturn.Neither,
		    new ReadValueId(nodeVar, attribute, null, null));




		if (res5.getResults()[0].getStatusCode()==StatusCode.GOOD) {

			String value=res5.getResults()[0].getValue().toString();

			return value;
		}
		return "";

	}*/





	private static void checkTypes(NodeId nodeId, SessionChannel mySession) throws ServiceFaultException, ServiceResultException {

		BrowseDescription browse = new BrowseDescription();
		browse.setNodeId(nodeId);
		browse.setBrowseDirection(BrowseDirection.Forward);
		browse.setIncludeSubtypes(true);

		browse.setNodeClassMask(NodeClass.DataType);
		browse.setResultMask(BrowseResultMask.All);
		BrowseResponse res3 = mySession.Browse(null, null, null, browse);
		for (BrowseResult res:res3.getResults()) {
			if (res.getReferences()==null)
				continue;
			for (ReferenceDescription ref:res.getReferences()) 			
			{
				String childNodeId=ref.getNodeId().toString();//"ns="+ref.getNodeId().getNamespaceIndex()+";i="+ref.getNodeId().getValue().toString();
				if (childNodeId.split(";").length==1)
					childNodeId="ns="+0+";"+childNodeId.split(";")[0];
				else
					childNodeId="ns="+ref.getNodeId().getNamespaceIndex()+";"+childNodeId.split(";")[1];
				dataTypes.put(childNodeId,ref.getBrowseName().toString());
				checkTypes(NodeId.parseNodeId(childNodeId), mySession);

			}			
		}

	}	
	/*private static byte[] toArray(int value)
	{
		// Little Endian
		return new byte[] {(byte)value, (byte)(value>>8), (byte)(value>>16), (byte)(value>>24)};

		// Big-endian
//		return new byte[] {(byte)(value>>24), (byte)(value>>16), (byte)(value>>8), (byte)(value)};
	}*/
}
