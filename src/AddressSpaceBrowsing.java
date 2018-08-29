import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.opcfoundation.ua.application.SessionChannel;
import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.common.NamespaceTable;
import org.opcfoundation.ua.common.ServiceFaultException;
import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.Attributes;
import org.opcfoundation.ua.core.BrowseDescription;
import org.opcfoundation.ua.core.BrowseDirection;
import org.opcfoundation.ua.core.BrowseResponse;
import org.opcfoundation.ua.core.BrowseResult;
import org.opcfoundation.ua.core.BrowseResultMask;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.NodeClass;
import org.opcfoundation.ua.core.ReadResponse;
import org.opcfoundation.ua.core.ReadValueId;
import org.opcfoundation.ua.core.ReferenceDescription;
import org.opcfoundation.ua.core.TimestampsToReturn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.scalified.tree.TreeNode;
import com.scalified.tree.multinode.ArrayMultiTreeNode;

import it.eng.util.PropertiesUtil;

public class AddressSpaceBrowsing {
	private Set<String> nodeSet=new HashSet<String>();
	private PropertiesUtil propertiesUtil=new PropertiesUtil();
	private Map<String, String> dataTypes=new HashMap<String, String>();
	private Logger logger = LoggerFactory.getLogger(AddressSpaceBrowsing.class);


	public AddressSpaceBrowsing(String filename) {
		propertiesUtil.analyzePropertiesFile(filename);
		
	}
	
	private void checkTypes(NodeId nodeId, SessionChannel mySession) throws ServiceFaultException, ServiceResultException {
		
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

	
	public void browse(NodeId nodeId, SessionChannel mySession, int level, int stopLevel, TreeNode<OpcUaNode> tree ) throws ServiceFaultException, ServiceResultException {
		TreeNode<OpcUaNode> node =tree;
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
		
	
		
		
		
		//logger.info("2 nodeId="+nodeId.toString());
		if (level>stopLevel)
			return;
		// Browse Root
		BrowseDescription browse = new BrowseDescription();
		browse.setNodeId(nodeId);
		browse.setBrowseDirection(BrowseDirection.Forward);
		browse.setIncludeSubtypes(true);
		browse.setNodeClassMask(NodeClass.Object, NodeClass.Variable, NodeClass.Method);
		browse.setResultMask(BrowseResultMask.All);
		BrowseResponse res3 = mySession.Browse(null, null, null, browse);
		for (BrowseResult res:res3.getResults()) {
			if (res.getReferences()==null)
				continue;
			for (ReferenceDescription ref:res.getReferences()) 			
			{
				
				
				//Taglio le relazioni non standard (es. flow)
				if (ref.getReferenceTypeId().getNamespaceIndex()!=0)
					continue;




				try {
					
					//Check Namespace To Ignore
					if (propertiesUtil.getNamespaceIgnore().contains(ref.getNodeId().getNamespaceIndex())){
						logger.info("Found Namespace To Ignore ns="+ref.getNodeId().getNamespaceIndex());
						continue;
					}
					
					
					
					//String childNodeId=ref.getNodeId().toString().substring(ref.getNodeId().toString().lastIndexOf(';')+1);
				
					//logger.info("ref.getNodeId().getServerIndex()="+ref.getNodeId().getValue().toString());
					//logger.info("ref.getNodeId().getNamespaceIndex()="+ref.getNodeId().getNamespaceIndex());
					String childNodeId=ref.getNodeId().toString();//"ns="+ref.getNodeId().getNamespaceIndex()+";i="+ref.getNodeId().getValue().toString();
					childNodeId="ns="+ref.getNodeId().getNamespaceIndex()+";"+childNodeId.split(";")[1];
					
					
					
					
					if (ref.getNodeId().getNamespaceUri()!=NamespaceTable.OPCUA_NAMESPACE) {
						if (nodeSet.contains(childNodeId)) {
							continue;
						}
						nodeSet.add(childNodeId);
					}
					
					//Check NS
					if (ref.getNodeId().getNamespaceUri()!=NamespaceTable.OPCUA_NAMESPACE){
						for (int i=0; i<level; i++) {
							for (int j=0;j<5;j++)
								if (j%5==0)
									System.out.print("|");

								else
									System.out.print("_");
						}
						if (ref.getTypeDefinition().getNamespaceUri()!=NamespaceTable.OPCUA_NAMESPACE) {
								//mySession.ge client.getAddressSpace().getNode(
							
							logger.info(ref.getNodeClass()+": "+ref.getBrowseName()+"("+childNodeId+")"+" --> "+ref.getTypeDefinition().toString());
							/*logger.info("Browsing NodeId....");
							String typeFolder=ref.getTypeDefinition().toString();
							NodeId nodeIdType=NodeId.parseNodeId(typeFolder);
							logger.info("************** TYPE FOLDER ****************");
							browse(nodeIdType,mySession,0, 8);*/
							//browse(NodeId.parseNodeId(ref.getTypeDefinition().toString()), mySession, 0);
							
						}else
							logger.info(ref.getNodeClass()+": "+ref.getBrowseName()+" type "+getDataType(mySession, childNodeId)+"("+childNodeId+")");
						
						OpcUaNode opcUaNode=new OpcUaNode();
						
						opcUaNode.setName(ref.getBrowseName().toString());
						
						opcUaNode.setDisplayName(ref.getDisplayName().toString());
						opcUaNode.setNodeId(childNodeId);
						
						if (getDataType(mySession, childNodeId).equalsIgnoreCase("Argument")) {
							//if (ref.getTypeDefinition().getNamespaceUri()!=NamespaceTable.OPCUA_NAMESPACE) {
								
							//}
							/*Integer mask=Integer.parseInt(getAttribute(mySession, childNodeId, Attributes.UserAccessLevel));
							if (AccessLevel.getSet(mask).contains(AccessLevel.CurrentWrite)) {
								//logger.info("UserAccessLevel.CurrentWrite DETECTED");
								//if (opcUaNode.getValue()==null)
									opcUaNode.setValue(getDataValue(mySession, childNodeId));
							}*/
							if (ref.getBrowseName().toString().equalsIgnoreCase("InputArguments")) {
								opcUaNode.setValue(getDataValue(mySession, childNodeId));

							}


							
						}
						
						if (ref.getNodeClass().toString().equalsIgnoreCase("Method")) {
							if (ref.getTypeDefinition().getNamespaceUri()==NamespaceTable.OPCUA_NAMESPACE) {
								opcUaNode.setTypeDefinition(getAttribute(mySession, ref.getTypeDefinition().toString().split(";")[1], Attributes.DisplayName));
							}


						}
						
						
						if (ref.getTypeDefinition().getNamespaceUri()!=NamespaceTable.OPCUA_NAMESPACE) {
							opcUaNode.setTypeDefinition(getAttribute(mySession, ref.getTypeDefinition().toString(), Attributes.DisplayName));
						}
						opcUaNode.setType(ref.getNodeClass().toString());
						opcUaNode.setDataType(getDataType(mySession, childNodeId));
						node = new ArrayMultiTreeNode<>(opcUaNode);
						tree.add(node);
						
						browse(NodeId.parseNodeId(childNodeId), mySession, level+1, stopLevel, node);

					}
					//logger.info("1 nodeId="+childNodeId);
				}
				catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			}
		}
	}
	
	
	public void browseType(NodeId nodeId, SessionChannel mySession, int level, int stopLevel, TreeNode<OpcUaNode> tree ) throws ServiceFaultException, ServiceResultException {
		TreeNode<OpcUaNode> node =tree;
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
		
		
		
		
		

		
		if (level>stopLevel)
			return;
		// Browse Root
		BrowseDescription browse = new BrowseDescription();
		browse.setNodeId(nodeId);
		browse.setBrowseDirection(BrowseDirection.Forward);
		browse.setIncludeSubtypes(true);
		
		browse.setNodeClassMask(NodeClass.Object, NodeClass.ObjectType, NodeClass.Variable, NodeClass.Method);
		browse.setResultMask(BrowseResultMask.All);
		BrowseResponse res3 = mySession.Browse(null, null, null, browse);
		for (BrowseResult res:res3.getResults()) {
		
		
			//logger.info("node id ="+nodeId.toString()+ " has "+res.getReferences().length+" references");

			if (res.getReferences()==null)
				continue;
			
			
			for (ReferenceDescription ref:res.getReferences()) 			
			{
				
			
			
				try {
					
					//Check Namespace To Ignore
					if (propertiesUtil.getNamespaceIgnore().contains(ref.getNodeId().getNamespaceIndex())){
						logger.info("Found Namespace To Ignore ns="+ref.getNodeId().getNamespaceIndex());
						continue;
					}
					//String childNodeId=ref.getNodeId().toString().substring(ref.getNodeId().toString().lastIndexOf(';')+1);

					//logger.info("ref.getNodeId().getServerIndex()="+ref.getNodeId().getValue().toString());
					//logger.info("ref.getNodeId().getNamespaceIndex()="+ref.getNodeId().getNamespaceIndex());
					String childNodeId="ns="+ref.getNodeId().getNamespaceIndex()+";i="+ref.getNodeId().getValue().toString();
					String nodeIdValue=ref.getNodeId().toString().split(";")[1];
					childNodeId="ns="+ref.getNodeId().getNamespaceIndex()+";"+nodeIdValue;
					
					if (ref.getNodeId().getNamespaceUri()!=NamespaceTable.OPCUA_NAMESPACE) {
					
						/*

					if (nodeSet.contains(childNodeId)) {
					
						continue;
					}
					
					nodeSet.add(childNodeId);
					
					*/
					}
					
					//Check NS
					if (ref.getNodeId().getNamespaceUri()!=NamespaceTable.OPCUA_NAMESPACE){
						for (int i=0; i<level; i++) {
							for (int j=0;j<5;j++)
								if (j%5==0)
									System.out.print("|");

								else
									System.out.print("_");
						}
						if (ref.getReferenceTypeId().getNamespaceIndex()==0) {
							if (ref.getTypeDefinition().getNamespaceUri()!=NamespaceTable.OPCUA_NAMESPACE) {
								//mySession.ge client.getAddressSpace().getNode(

								logger.info(ref.getNodeClass()+": "+ref.getBrowseName()+"("+childNodeId+")"+" --> "+ref.getTypeDefinition().toString());
								/*logger.info("Browsing NodeId....");
							String typeFolder=ref.getTypeDefinition().toString();
							NodeId nodeIdType=NodeId.parseNodeId(typeFolder);
							logger.info("************** TYPE FOLDER ****************");
							browse(nodeIdType,mySession,0, 8);*/
								//browse(NodeId.parseNodeId(ref.getTypeDefinition().toString()), mySession, 0);

							}else
								logger.info(ref.getNodeClass()+": "+ref.getBrowseName()+" type "+getDataType(mySession, childNodeId)+"("+childNodeId+")");


							/*	if (childNodeId.contains("ns=4")) {
							if (ref.getNodeClass()==NodeClass.Variable) {

								logger.info("Variable \n");

								ReadValueId[] nodesToRead = {new ReadValueId(NodeId.parseNodeId(childNodeId), Attributes.Historizing, null, null)};
							    ReadRequest req = new ReadRequest(null, 0.0, TimestampsToReturn.Both, nodesToRead);
							    //logger.info("REQUEST: " + req);
							 // Invoke service
							    ReadResponse resp = mySession.Read(req);
							    // Print result
							    logger.info("RESPONSE: " + resp);
							}
						}*/



							OpcUaNode opcUaNode=new OpcUaNode();
							opcUaNode.setName(ref.getBrowseName().toString());
							opcUaNode.setDisplayName(ref.getDisplayName().toString());
							opcUaNode.setNodeId(childNodeId);
							opcUaNode.setType(ref.getNodeClass().toString());
							opcUaNode.setTypeDefinition(ref.getReferenceTypeId().toString());
							opcUaNode.setDataType(getDataType(mySession, childNodeId));
							node = new ArrayMultiTreeNode<>(opcUaNode);
							tree.add(node);
						}
						try {

							browseType(NodeId.parseNodeId(childNodeId), mySession, level+1, stopLevel, node);
						}
						catch(Exception e) {

							//browseType(NodeId.parseNodeId(updateNodeIdAsNumeric(childNodeId)), mySession, level+1, stopLevel, node);


						}
					}
					
					//browseType(NodeId.parseNodeId(childNodeId), mySession, level+1, stopLevel, node);
				}
				catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			}
		}
	}
	
	public Set<String> getNodeSet() {
		return nodeSet;
	}
	public void setNodeSet(Set<String> nodeSet) {
		this.nodeSet = nodeSet;
	}
	
	
	
	
	
	private String getDataType(SessionChannel mySession, String nodeId) throws ServiceFaultException, ServiceResultException {
		try {
			
			
			
		// Read DataType Test
		NodeId nodeVar=NodeId.parseNodeId(nodeId);
		ReadResponse res5 = mySession.Read(null, null, TimestampsToReturn.Neither,
		    new ReadValueId(nodeVar, Attributes.DataType, null, null));
		
		
		
		
		if (res5.getResults()[0].getStatusCode()==StatusCode.GOOD) {
			
			String value=res5.getResults()[0].getValue().toString();
			if (!value.contains("ns=")) {
				value="ns=0;"+value;
			}
			//logger.info("test="+ value);
			String ret=dataTypes.get(value);
			//logger.info("ret="+ret);
			if (ret==null)
				ret="";
			return ret;
		}
		}catch (Exception e) {
			// TODO: handle exception
			return "";
		}
		return "";

	}
	
	
	private String getAttribute(SessionChannel mySession, String nodeId, UnsignedInteger attribute) throws ServiceFaultException, ServiceResultException {
		// Read DataType Test
		NodeId nodeVar=NodeId.parseNodeId(nodeId);
		ReadResponse res5 = mySession.Read(null, null, TimestampsToReturn.Neither,
		    new ReadValueId(nodeVar, attribute, null, null));
		
		
		
		
		if (res5.getResults()[0].getStatusCode()==StatusCode.GOOD) {
			
			String value=res5.getResults()[0].getValue().toString();
			
			return value;
		}
		return "";

	}
	
	
	private DataValue[] getDataValue(SessionChannel mySession, String nodeId) throws ServiceFaultException, ServiceResultException {
		// Read DataType Test
		NodeId nodeVar=NodeId.parseNodeId(nodeId);
		ReadResponse res5 = mySession.Read(null, null, TimestampsToReturn.Neither,
		    new ReadValueId(nodeVar, Attributes.Value, null, null));
		
		
		
		
		if (res5.getResults()[0].getStatusCode()==StatusCode.GOOD) {
			
			
			
			return res5.getResults();
		}
		return null;

	}
	
	
}
