import static org.opcfoundation.ua.utils.EndpointUtil.selectByMessageSecurityMode;
import static org.opcfoundation.ua.utils.EndpointUtil.selectByProtocol;
import static org.opcfoundation.ua.utils.EndpointUtil.sortBySecurityLevel;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.opcfoundation.ua.application.Client;
import org.opcfoundation.ua.application.SessionChannel;
import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.NodeId;

import org.opcfoundation.ua.common.ServiceFaultException;
import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.Argument;
import org.opcfoundation.ua.core.Attributes;
import org.opcfoundation.ua.core.BrowseDescription;
import org.opcfoundation.ua.core.BrowseDirection;
import org.opcfoundation.ua.core.BrowseResponse;
import org.opcfoundation.ua.core.BrowseResult;
import org.opcfoundation.ua.core.BrowseResultMask;
import org.opcfoundation.ua.core.EndpointDescription;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.MessageSecurityMode;
import org.opcfoundation.ua.core.NodeClass;
import org.opcfoundation.ua.core.ReadRequest;
import org.opcfoundation.ua.core.ReadResponse;
import org.opcfoundation.ua.core.ReadValueId;
import org.opcfoundation.ua.core.ReferenceDescription;
import org.opcfoundation.ua.core.TimestampsToReturn;
import org.opcfoundation.ua.transport.ServiceChannel;
import org.opcfoundation.ua.transport.security.Cert;
import org.opcfoundation.ua.transport.security.KeyPair;
import org.opcfoundation.ua.transport.security.PrivKey;
import org.opcfoundation.ua.utils.CertificateUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalified.tree.TreeNode;
import com.scalified.tree.TreeNodeException;
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

public class MappingTool_2103 {
	private static final String PRIVKEY_PASSWORD = "Opc.Ua";

	private static final String ENDPOINT_KEY = "-e";
	private static final String FILENAME_KEY = "-f";

	private static PropertiesUtil propertiesUtil=new PropertiesUtil();

	private static Map<String, String> dataTypes=new HashMap<String, String>();



	public static void main(String[] args) throws ServiceResultException, IOException {


		// TODO Auto-generated method stub

		try {
		String url = "";
		String filename = "";

		for(int i=0; i<args.length; i+=2)
		{
			String key = args[i];
			String value = args[i+1];
			

			switch (key)
			{
				case ENDPOINT_KEY : url = value; break;
				case FILENAME_KEY : filename = value;break;

			}
		}
		if (url.isEmpty()) {
			System.out.println("No endpoint specified.");
			return;
		}


		///////////// CLIENT //////////////
		// Load Client's Application Instance Certificate from file
		//KeyPair myClientApplicationInstanceCertificate = ExampleKeys.getCert("Client");
		// Create Client
		// Try to load an application certificate with the specified application name.
		// In case it is not found, a new certificate is created.
		final KeyPair pair = getCert("Test");

		// Create the client using information provided by the created certificate
		final Client myClient = Client.createClientApplication(pair);

		//KeyPair myHttpsCertificate = ExampleKeys.getHttpsCert("Client");
		//myClient.getApplication().getHttpsSettings().setKeyPair(myHttpsCertificate);
		//////////////////////////////////////

		////////// DISCOVER ENDPOINT /////////
		// Discover server's endpoints, and choose one
		//String publicHostname = InetAddress.getLocalHost().getHostName();
		//String url = "opc.tcp://localhost:4334/UA/MyLittleServer"; // ServerExample1
		//url="opc.tcp://opcua.demo-this.com:51210/UA/SampleServer";
		//url="opc.tcp://commsvr.com:51234/UA/CAS_UA_Server";
		// "https://"+publicHostname+":8443/UAExample"; // ServerExample1
		// "opc.tcp://"+publicHostname+":51210/"; // :51210=Sample Server
		// "https://"+publicHostname+":51212/SampleServer/"; // :51212=Sample Server
		// "opc.tcp://"+publicHostname+":62541/"; // :62541=DataAccess Server
		EndpointDescription[] endpoints = myClient.discoverEndpoints(url);
		// Filter out all but opc.tcp protocol endpoints
		if (url.startsWith("opc.tcp")) {
			endpoints = selectByProtocol(endpoints, "opc.tcp");
			// Filter out all but Signed & Encrypted endpoints
			endpoints = selectByMessageSecurityMode(endpoints, MessageSecurityMode.None);

			// Filter out all but Basic128 cryption endpoints
			// endpoints = selectBySecurityPolicy(endpoints, SecurityPolicy.BASIC128RSA15);
			// Sort endpoints by security level. The lowest level at the
			// beginning, the highest at the end of the array
			endpoints = sortBySecurityLevel(endpoints);
		} else {
			endpoints = selectByProtocol(endpoints, "https");
		}

		// Choose one endpoint
		EndpointDescription endpoint = endpoints[endpoints.length - 1];
		//////////////////////////////////////

		endpoint.setSecurityMode(MessageSecurityMode.None);
		///////////// EXECUTE //////////////
		SessionChannel mySession = myClient.createSessionChannel(endpoint);
		// mySession.activate("username", "123");
		mySession.activate();
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
		System.out.println(ref.getBrowseName());
	}
}*/


		
		if (propertiesUtil.analyzePropertiesFile(filename)!=0) {
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
		configuration.setDeviceRegistrationDuration(propertiesUtil.getDeviceRegistrationDuration());
		ObjectMapper mapper = new ObjectMapper();


		//Object to JSON in String
		String jsonInString = mapper.writeValueAsString(configuration);
		System.out.println("********************************************************************");
		System.out.println(jsonInString);


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
		System.out.println("ref="+ref.toString());

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
					childNodeId="ns="+ref.getNodeId().getNamespaceIndex()+";"+childNodeId.split(";")[1];
					dataTypes.put(childNodeId,ref.getBrowseName().toString());
					checkTypes(NodeId.parseNodeId(childNodeId), mySession);

				}			
			}
		}

		TreeNode<OpcUaNode> objectTree = new ArrayMultiTreeNode<>(new OpcUaNode());
		AddressSpaceBrowsing addressSpaceBrowsing=new AddressSpaceBrowsing(filename);

//


		System.out.println("************** OBJECT FOLDER ****************");


		//String rootFolder=Identifiers.RootFolder.toString();
		//NodeId nodeIdRoot=NodeId.parseNodeId(rootFolder);

		//TreeNode<OpcUaNode> objectTree = new ArrayMultiTreeNode<>(new OpcUaNode());

		//AddressSpaceBrowsing addressSpaceBrowsing=new AddressSpaceBrowsing(filename);
		addressSpaceBrowsing.browse(Identifiers.ObjectsFolder,mySession,0, 6, objectTree);
		//String typesFolder="ns=4;i=1132";
		//NodeId nodeIdTypes=NodeId.parseNodeId(typesFolder);


		System.out.println("************** ITERATING CLEAN OBJECT TREE ****************");


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




			//Remove methods from subtrees
			//for (TreeNode<OpcUaNode> child : methods) {
				//node.remove(child);

			//}


			//Iterate methods
			for (TreeNode<OpcUaNode> method : methods) {
				for (TreeNode<OpcUaNode> child : method) {

					if (child.level()>0) {

						if (child.data().getType().equalsIgnoreCase("Variable")) {
							System.out.println("-------) "+child.data()); // any other action goes here

						}
						//System.out.println("child) "+child.data()); // any other action goes here
					}
					else
						System.out.println("METHOD) "+child.data()); // any other action goes here
				}
			}

			
		}


//


		System.out.println("************** TYPES FOLDER ****************");

		//Creating the tree nodes
		TreeNode<OpcUaNode> typeTree = new ArrayMultiTreeNode<>(new OpcUaNode());



		addressSpaceBrowsing.browseType(/*Identifiers.TypesFolder*/Identifiers.BaseObjectType,mySession,0, 5, typeTree);
		boolean noTypes=true;
		if (typeTree.isLeaf()) {
			System.out.println("##############   NO TYPES   ###############");
			noTypes=true;
		}
		else {
			noTypes=false;
			/*System.out.println("************** ITERATING TYPES TREE ****************");

			//Iterating over the tree elements using foreach
			for (TreeNode<OpcUaNode> node : typeTree.root()) {

				System.out.println(node.level()+") "+node.data()); // any other action goes here
			}*/


		}



		//Iterate on datatype mapping
		/*System.out.println("##############   DATATYPE MAPPING ITERATION   ###############");
		for (String key:propertiesUtil.getDataTypeMapping().keySet()) {
			System.out.println("["+key+"]="+propertiesUtil.getDataTypeMapping().get(key));

		}*/



		if (noTypes) {

			//List<Type> types= new ArrayList<Type>();



			//Iterating over the tree elements using foreach
			for (TreeNode<OpcUaNode> node : objectTree.subtrees()) {



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


				//Type type=new Type();
				//type.getTypeDetails().setService(propertiesUtil.getFiwareService());
				//type.getTypeDetails().setSubservice(propertiesUtil.getFiwareServicePath());
				//Iterating over the tree elements using foreach
				String objectName=null;
				String objectId=null;
				//String methodId=null;
				for (TreeNode<OpcUaNode> child : node) {

					if (child.level()>1) {
						if ((child.data().getType().equalsIgnoreCase("variable"))&&(!child.parent().data().getType().equalsIgnoreCase("method"))) {
							System.out.println("----) "+child.data()); // any other action goes here
							//String objectPrefix=getPrefixByChild(mySession, child); 

							Attribute attribute=new Attribute();
							attribute.setName(child.data().getDisplayName());
							attribute.setType(child.data().getDataType());
							//type.getTypeDetails().getActive().add(attribute);
							configuration.getTypes().get(objectName).getActive().add(attribute);

							Mapping mapping=new Mapping();
							mapping.setOpcua_id(child.data().getNodeId());
							mapping.setOcb_id(/*objectPrefix+*/child.data().getDisplayName());
							context.getMappings().add(mapping);


						}

						if ((child.data().getType().equalsIgnoreCase("variable"))&&(child.parent().data().getType().equalsIgnoreCase("method"))) {
							System.out.println("--MV--) "+child.data()); // any other action goes here
							ContextSubscription contextSubscription=null;
							for (ContextSubscription cs:configuration.getContextSubscriptions()) {
								if (cs.getId().equalsIgnoreCase(objectName)) {
									System.out.println("found");
									contextSubscription=cs;
									break;
								}
							}
							Mapping mapping=null;
							for (Mapping mp:contextSubscription.getMappings()) {
								if (mp.getObject_id().equalsIgnoreCase(objectId)) {
									System.out.println("found MP");
									mapping=mp;
								}
							}


							DataValue[] dataValues=child.data().getValue();
							if (dataValues!=null) {
							for (DataValue dataValue:dataValues) {
								Argument[] inputArguments=(Argument[]) dataValue.getValue().getValue();

								for (int i=0; i<inputArguments.length; i++ ){
									System.out.println("inputArguments["+i+"]="+inputArguments[i].getName());
									String childNodeId=null;
									String identifier=inputArguments[i].getDataType().toString();
									if (identifier.contains(";")) {
										identifier=identifier.split(";")[1];
									}


									childNodeId="ns="+inputArguments[i].getDataType().getNamespaceIndex()+";"+identifier;
									OpcUaUtil opcUaUtil=new OpcUaUtil();
									System.out.println("test="+dataTypes.get(childNodeId));
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
							objectId=child.data().getNodeId();
						}

						if (child.data().getType().equalsIgnoreCase("method")) {
							//methodId=child.data().getNodeId();
							System.out.println("--M--) "+child.data()); // any other action goes here
							//String objectPrefix=getPrefixByChild(mySession, child); 
							Attribute attribute=new Attribute();
							attribute.setName(child.data().getDisplayName());
							attribute.setType("command");
							//type.getTypeDetails().getActive().add(attribute);
							configuration.getTypes().get(objectName).getCommands().add(attribute);

							ContextSubscription cs=new ContextSubscription();

							cs.setId(objectName);
							cs.setType(objectName);


							Mapping mapping=new Mapping();
							mapping.setOcb_id(child.data().getDisplayName());
							mapping.setObject_id(child.parent().data().getNodeId());
							mapping.setOpcua_id(child.data().getNodeId());

							cs.getMappings().add(mapping);

							configuration.getContextSubscriptions().add(cs);

						}
						//System.out.println("child) "+child.data()); // any other action goes here
					}
					else {
						//type.setName(child.data().getName());
						objectName=child.data().getDisplayName();
						objectId=child.data().getNodeId();
						context.setId(child.data().getDisplayName());
						context.setType(child.data().getDisplayName());
						context.setService(propertiesUtil.getFiwareService());
						context.setSubservice(propertiesUtil.getFiwareServicePath());

						configuration.getTypes().put(child.data().getDisplayName(), new TypeDetails());
						configuration.getTypes().get(child.data().getDisplayName()).setNodeId(child.data().getNodeId());

						configuration.getTypes().get(child.data().getDisplayName()).setService(propertiesUtil.getFiwareService());
						configuration.getTypes().get(child.data().getDisplayName()).setSubservice(propertiesUtil.getFiwareServicePath());


						System.out.println("-) "+child.data()); // any other action goes here
					}
				}
				//configuration.getTypes().add(type);
				configuration.getContexts().add(context);

			}




			System.out.println("**************************FINAL***************************");
			System.out.println(mapper.writeValueAsString(configuration));

			try (FileWriter file = new FileWriter("config.json")) {
				file.write(mapper.writeValueAsString(configuration));
				System.out.println("Successfully Copied JSON Object to File...");
			}

			//configuration.setTypes(types);
		}else {


			List<String> nodeIdsTypes=new ArrayList<String>();
			List<String> nodeNamesTypes=new ArrayList<String>();

			List<String> nodeIdsObjects=new ArrayList<String>();


			Boolean hasSubType=false;
			//Iterating over the tree elements using foreach
			for (TreeNode<OpcUaNode> node : typeTree.subtrees()) {
				for (TreeNode<OpcUaNode> child : node) {
					if (child.level()>1) {
						if (child.data().getTypeDefinition().equals(Identifiers.HasSubtype.toString())){
							hasSubType=true;
							//System.out.println(child.data().getNodeId() +"is a subtype");
							break;
						}
					}
				}
			}
			
			
			
			
			
			hasSubType=false;
			
			
			if (!hasSubType) {
			//Iterating over the tree elements using foreach
			for (TreeNode<OpcUaNode> node : typeTree.subtrees()) {


				//Iterating over the tree elements using foreach
				String objectName=null;
				for (TreeNode<OpcUaNode> child : node) {

					
					
					
					
					if (child.level()>1) {
						if (child.data().getType().equalsIgnoreCase("variable")) {
							System.out.println("----) "+child.data()); // any other action goes here
							String objectPrefix=getPrefixByChild(mySession, child); 

							if (nodeIdsTypes.contains(objectPrefix+child.data().getName())) {
								System.out.println("cassando Type "+objectPrefix+child.data().getName());

								continue;
							}
							Attribute attribute=new Attribute();
							attribute.setName(objectPrefix+child.data().getName());
							attribute.setType(child.data().getDataType());
							//type.getTypeDetails().getActive().add(attribute);
							configuration.getTypes().get(objectName).getActive().add(attribute);

							nodeIdsTypes.add(objectPrefix+child.data().getName());


						}
						//System.out.println("child) "+child.data()); // any other action goes here
					}
					else {
						//type.setName(child.data().getName());
						objectName=child.data().getDisplayName();
						nodeNamesTypes.add(objectName);

						configuration.getTypes().put(child.data().getDisplayName(), new TypeDetails());
						configuration.getTypes().get(child.data().getDisplayName()).setNodeId(child.data().getNodeId());

						configuration.getTypes().get(child.data().getDisplayName()).setService(propertiesUtil.getFiwareService());
						configuration.getTypes().get(child.data().getDisplayName()).setSubservice(propertiesUtil.getFiwareServicePath());


						System.out.println("-) "+child.data()); // any other action goes here
					}
				}


			}

			}else {
				
				
				
				
				
				System.out.println("...finding subtypes");
				//Finding subtypes
				List<TreeNode<OpcUaNode>> subTypes=new ArrayList<TreeNode<OpcUaNode>>();
				for (TreeNode<OpcUaNode> node : typeTree.subtrees()) {
					for (TreeNode<OpcUaNode> child : node) {
						if (child.level()>1) {
							if (child.data().getTypeDefinition().equals(Identifiers.HasSubtype.toString())){
								//Verify last subtype
								Boolean noSubType=true;
								for (TreeNode<OpcUaNode> subChild : child.subtrees()) {

									if (subChild.data().getTypeDefinition().equals(Identifiers.HasSubtype.toString())) {
										noSubType=false;
										break;
									}

								}
								if (noSubType) {
									System.out.println(child.data().getNodeId() +" is a subtype");
									subTypes.add(child);
								}
								//break;
							}
						}
					}
				}
				
				
				subTypes=new ArrayList<TreeNode<OpcUaNode>>();
				
				for (TreeNode<OpcUaNode> subType : subTypes) {
					System.out.println("SUBTYPE");
					TreeNode<OpcUaNode> node=subType;


					if (node.parent()==null)
						break;
					TreeNode<OpcUaNode>  parent=node.parent().clone();

					
					//Cancello dall'albero parent i subtype
					List<TreeNode<OpcUaNode>> deletedSubTypes=new ArrayList<TreeNode<OpcUaNode>>();
					for (TreeNode<OpcUaNode> subType2 : subTypes) {
						if (parent.hasSubtree(subType2)) {
							parent.remove(subType2);
							deletedSubTypes.add(subType2);
						}
					}
				
					
					
					while (!parent.isRoot()) {
						int rootLevel=parent.level();
						System.out.println("----) "+parent.data()); // any other action goes here
						if (parent.isRoot())
							break;
						for (TreeNode<OpcUaNode> child : parent) {
							if (child.level()>rootLevel) {
								subType.add(child.clone());
								System.out.println("ADD child--"+child.level()+"--) "+child.data()); // any other action goes here

							}
						}
						if (node.parent()==null)
							break;
						parent=node.parent().clone();
					}
					
					
					
					//Ripristino nell'albero parent i subtype
					for (TreeNode<OpcUaNode> subType3 : deletedSubTypes) {
						
							parent.add(subType3);
						
					}
				
				}
				
				System.out.println("---------");
				try {
				for (TreeNode<OpcUaNode> subType : subTypes) {
					//System.out.println("---------"+subType.subtrees().size());
					objectTree.add(subType);

					for (TreeNode<OpcUaNode> subtree : subType.subtrees()) {

						for (TreeNode<OpcUaNode> child : subtree) {
							

						if (child.level()>1) {
						//	if (child.data().getType().equalsIgnoreCase("variable")) {
								System.out.println("---"+child.level()+"-) "+child.data()); // any other action goes here
						//	}
						}else
							System.out.println("-"+child.level()+") "+child.data());
					}
				}}}catch(TreeNodeException e) {
					System.out.println("error");
					//e.printStackTrace();
				}
				
				
				
				
				
				
/*
				//Iterating over the tree elements using foreach
				for (TreeNode<OpcUaNode> node : objectTree.subtrees()) {
					
					
					
					String objectName=null;
					int rootLevel=node.level();
					objectName=node.data().getDisplayName();
					nodeNamesTypes.add(objectName);

					configuration.getTypes().put(node.data().getDisplayName(), new TypeDetails());
					configuration.getTypes().get(node.data().getDisplayName()).setNodeId(node.data().getNodeId());

					configuration.getTypes().get(node.data().getDisplayName()).setService(propertiesUtil.getFiwareService());
					configuration.getTypes().get(node.data().getDisplayName()).setSubservice(propertiesUtil.getFiwareServicePath());


					
					
					
					//Iterating over the tree elements using foreach
					for (TreeNode<OpcUaNode> nodeObjectChild : node.subtrees()) {

					//Iterating over the tree elements using foreach
					
					for (TreeNode<OpcUaNode> child : nodeObjectChild) {

						
						

					
						
						if (child.level()>rootLevel) {
							if (child.data().getType().equalsIgnoreCase("variable")) {
								System.out.println("----) "+child.data()); // any other action goes here
								String objectPrefix=getPrefixByChild(mySession, child); 

								//if (nodeIdsTypes.contains(objectPrefix+child.data().getName())) {
								//	System.out.println("cassando Type "+objectPrefix+child.data().getName());
//
								//	continue;
								//}
								Attribute attribute=new Attribute();
								attribute.setName(objectPrefix+child.data().getName());
								attribute.setType(child.data().getDataType());
								//type.getTypeDetails().getActive().add(attribute);
								configuration.getTypes().get(objectName).getActive().add(attribute);

								nodeIdsTypes.add(objectPrefix+child.data().getName());


							}
							//System.out.println("child) "+child.data()); // any other action goes here
						}
						else {
							//type.setName(child.data().getName());
							
						}
					}


				}

				
			
			}
*/			
System.out.println("*************************##########################");

			//Iterating over the tree elements using foreach
			for (TreeNode<OpcUaNode> nodeObject : objectTree.subtrees()) {


				Context context=new Context();
				//Iterating over the tree elements using foreach
				for (TreeNode<OpcUaNode> nodeObjectChild : nodeObject.subtrees()) {
				//Iterating over the tree elements using foreach
				for (TreeNode<OpcUaNode> child : nodeObjectChild) {
					//Cambio logica
				//	System.out.println("child.data="+child.data());
					if ((child.data().getTypeDefinition()!=null)&&(context.getType()==null)&&(nodeNamesTypes.contains(child.data().getTypeDefinition()))) {
						context=new Context();

						
						if ((child.parent().data().getTypeDefinition()!=null)&&(nodeNamesTypes.contains(child.parent().data().getTypeDefinition()))) {
							System.out.println("Parent has a type known...ignore child...");
							continue;
						}
						
						
							
						context.setType(child.data().getTypeDefinition() );


						context.setId(child.data().getName());
						context.setService(propertiesUtil.getFiwareService());
						context.setSubservice(propertiesUtil.getFiwareServicePath());
						for (TreeNode<OpcUaNode> child2 : child) {
							if (child2.data().getType().equalsIgnoreCase("variable")) {
								System.out.println("--var) "+child2.data()); // any other action goes here


								String objectPrefix=getPrefixByChild(mySession, child2); 
								if (nodeIdsObjects.contains(objectPrefix+child2.data().getName())) {
									System.out.println("cassando Obj "+objectPrefix+child2.data().getName());

									continue;
								}

								Mapping mapping=new Mapping();
								mapping.setOpcua_id(child2.data().getNodeId());
								mapping.setOcb_id(objectPrefix+child2.data().getName());
								context.getMappings().add(mapping);
								nodeIdsObjects.add(objectPrefix+child2.data().getName());


							}
						}
						configuration.getContexts().add(context);
						context=new Context();
						nodeIdsObjects.clear();
					}

					/*					
					if (child.level()>1) {
						if (child.data().getType().equalsIgnoreCase("variable")) {
							System.out.println("--var) "+child.data()); // any other action goes here


							String objectPrefix=getPrefixByChild(mySession, child); 
							if (nodeIdsObjects.contains(objectPrefix+child.data().getName())) {
								System.out.println("cassando Obj "+objectPrefix+child.data().getName());

								continue;
							}

							Mapping mapping=new Mapping();
							mapping.setOpcua_id(child.data().getNodeId());
							mapping.setOcb_id(objectPrefix+child.data().getName());
							context.getMappings().add(mapping);
							nodeIdsObjects.add(objectPrefix+child.data().getName());


						}else 
						if (child.data().getType().equalsIgnoreCase("object")) {
							System.out.println("--obj) "+child.data()); // any other action goes here

							if ((child.data().getTypeDefinition()!=null)&&(context.getType()==null))
								context.setType(child.data().getTypeDefinition() );
							if ((child.data().getTypeDefinition().equalsIgnoreCase(context.getType())))
								context.setId(child.data().getName());


						}

						//System.out.println("child) "+child.data()); // any other action goes here
					}
					else {
						//type.setName(child.data().getName());
						context.setId(child.data().getName());


						context.setType(child.data().getTypeDefinition() );
						context.setService(propertiesUtil.getFiwareService());
						context.setSubservice(propertiesUtil.getFiwareServicePath());


						System.out.println("-) "+child.data()); // any other action goes here
					}*/
				}
				}}
				//configuration.getContexts().add(context);


			}





			System.out.println("**************************FINAL***************************");
			System.out.println(mapper.writeValueAsString(configuration));

			try (FileWriter file = new FileWriter("config.json")) {
				file.write(mapper.writeValueAsString(configuration));
				System.out.println("Successfully Copied JSON Object to File...");
			}

			//configuration.setTypes(types);

		}



		///////////// SHUTDOWN /////////////
		//Close channel
		mySession.closeAsync();
		//////////////////////////////////////
		System.exit(0);
		//System.out.println(res3);

		//////////// TEST-STACK ////////////
		// Create Channel
		ServiceChannel myChannel = myClient.createServiceChannel(endpoint);
		// Create Test Request
		NodeId nodeId=NodeId.parseNodeId("ns=1;s=PumpSpeed");
		ReadValueId[] nodesToRead = {new ReadValueId(nodeId, Attributes.Value, null, null)};
		ReadRequest req = new ReadRequest(null, 0.0, TimestampsToReturn.Both, nodesToRead);
		System.out.println("REQUEST: " + req);

		// Invoke service
		ReadResponse res = mySession.Read(req);
		// Print result
		System.out.println("RESPONSE: " + res);
		//////////////////////////////////////


		///////////// SHUTDOWN /////////////
		// Close channel
		myChannel.closeAsync();
		//////////////////////////////////////
		System.exit(0);
		
		}catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	private static String getPrefixByChild( SessionChannel session, TreeNode<OpcUaNode> child) throws ServiceFaultException, ServiceResultException {
		// TODO Auto-generated method stub
		
		String prefix="";
		while ((child.parent()!=null)&&(child.parent().level()>1)) {
			//Get DisplayName
			//String name=getAttribute(session, child.data().getNodeId(),Attributes.DisplayName);
			if (child.parent().data()!=null){
				if (child.parent().data().getTypeDefinition()!=null) {
					if (child.parent().parent()!=null){
						if (child.parent().parent().data().getTypeDefinition()==null) {
							child=child.parent();
							continue;
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
				childNodeId="ns="+ref.getNodeId().getNamespaceIndex()+";"+childNodeId.split(";")[1];
				dataTypes.put(childNodeId,ref.getBrowseName().toString());
				checkTypes(NodeId.parseNodeId(childNodeId), mySession);

			}			
		}

	}	

	
}
