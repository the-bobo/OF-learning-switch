package net.floodlightcontroller.Assignment3;

import java.io.IOException;
import java.util.*;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFType;
import org.openflow.protocol.Wildcards;
import org.openflow.protocol.Wildcards.Flag;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.util.HexString;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitch.PortChangeType;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.ImmutablePort;
import net.floodlightcontroller.core.internal.CmdLineSettings;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.FloodlightModuleLoader;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightModuleContext;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.hub.Hub;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryListener;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.linkdiscovery.LinkInfo;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.Assignment3.Djikstras;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Assignment3 implements ILinkDiscoveryListener, IOFMessageListener,
IFloodlightModule, IOFSwitchListener {

	protected ILinkDiscoveryService linkDiscoverer;
	protected IFloodlightProviderService floodlightProvider;
	protected Map<Link, LinkInfo> links;
	protected int N; //will be used as number of switches in linkDiscoveryUpdate
	protected static Logger log = LoggerFactory.getLogger(Assignment3.class);
	protected static String[] aryLines;
	protected static List<Vertex> hostVertices = new ArrayList<Vertex>(); 
	protected static List<FullSwitchToHost> switchToHostWithPortInfo = new ArrayList<FullSwitchToHost>();
	protected static List<Vertex> switchToHost = new ArrayList<Vertex>();

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {

		Map<String, String> configParams = context.getConfigParams(this);
		//String TopologyInfo = configParams.get("Topology Info");
		String NumSwitches = configParams.get("Num");
		N = Integer.parseInt(NumSwitches); //NEEDS TO BE N = args[0] FOR COMMAND LINE TO WORK

		// From Slides
		this.floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);

		// add self as one of switch event listeners
		this.floodlightProvider.addOFSwitchListener(this);

		// add self as one of link event listeners
		this.linkDiscoverer = context.getServiceImpl(ILinkDiscoveryService.class);
		this.linkDiscoverer.addListener(this);

		// =========== FILE PARSING ================

		//GET INPUT FILE TO SCRUB HOST-->SWITCH CONNECTIONS FOR DJIKSTRA
		//String file_name = args[1];
		//String file_name = configParams.get("Topology Info"); //REPLACE WITH PATH TO FILE FROM COMMAND LINE ARG
		String file_name = "./topologyinfo.txt";
		System.out.println("====== HERE IS WHAT I GET FOR file_name " + file_name);
		try{
			ReadFile file = new ReadFile(file_name);
			aryLines = file.OpenFile();
		}

		catch (IOException f){
			System.out.println(f.getMessage());
			System.out.println("\n ERROR: First command line arg must be number of switches as a single integer");
			System.out.println("\n ERROR: Second command line arg must be path to Input File with Host-->Switch topology \n");
		}

		int iterator_counter;
		//String[][] elephantList = new String[][] { };

		for (iterator_counter = 0; iterator_counter < aryLines.length; iterator_counter++){
			System.out.println(aryLines[iterator_counter]);
		}



		// PARSE THE FILE INPUT BY SPLITTING ON COMMAS, AND USING THAT TO CREATE VERTEX OBJECTS
		String[] dummyAryLines = aryLines;
		String[] dummyAryLines2 = aryLines;
		String[] dummyAryLines3 = aryLines;

		for (String item : dummyAryLines){
			//each item in the array is a String row: "10.0.0.1, 00:00:00...:01, 1"
			//This stores our Vertices for Djikstras, but does not associate port info w/ switches
			//This code assumes each host connects to only one switch
			Scanner parsing = new Scanner(item);
			String hostIP = parsing.next();
			hostIP = hostIP.substring(0,hostIP.length()-1); // gets rid of trailing comma
			Vertex newHost = new Vertex("Host", hostIP, "-1");
			String switchDPID = parsing.next();
			switchDPID = switchDPID.substring(0, switchDPID.length()-1);
			Vertex newSwitch = new Vertex("Switch", "-1", switchDPID);

			List<Edge> edgeList = new ArrayList<Edge>();
			edgeList.add(new Edge(newSwitch, 1));
			newHost.adjacencies = edgeList;

			hostVertices.add(newHost);
			parsing.close();
		}
		System.out.println("Here is what I get for hostVertices " + hostVertices);

		for (String item2: dummyAryLines2){
			//This will tell us which Switches connect to which Hosts,
			//along with the portInfo
			Scanner parsing2 = new Scanner(item2);
			String hostIP2 = parsing2.next();
			hostIP2 = hostIP2.substring(0,hostIP2.length()-1);
			Vertex newTerminalHost = new Vertex("Host", hostIP2, "-1");
			String switchDPID2 = parsing2.next();
			switchDPID2 = switchDPID2.substring(0, switchDPID2.length()-1);
			FullSwitchToHost newFullSwitchToHost = new FullSwitchToHost(switchDPID2, Short.parseShort(parsing2.next()), newTerminalHost);
			switchToHostWithPortInfo.add(newFullSwitchToHost);
			parsing2.close();
		}

		for (String item3: dummyAryLines3){
			//this will tell us which Switches connect to which Hosts,
			//without portInfo, so that we can run Djikstras on a full graph
			Scanner parsing3 = new Scanner(item3);
			String hostIP2 = parsing3.next();
			hostIP2 = hostIP2.substring(0,hostIP2.length()-1);
			Vertex newTerminalHost = new Vertex("Host", hostIP2, "-1");
			String switchDPID2 = parsing3.next();
			switchDPID2 = switchDPID2.substring(0, switchDPID2.length()-1);
			Vertex newSwitchToHost = new Vertex("Switch", "-1", switchDPID2);
			Short dummyDummy = Short.parseShort(parsing3.next()); // not sure what'll happen if i don't dispose of it 
			//add newTerminalHost as an Edge in Vertex newSwitchToHost
			List<Edge> edgeList2 = new ArrayList<Edge>();
			edgeList2.add(new Edge(newTerminalHost, 1));
			newSwitchToHost.adjacencies = edgeList2;

			switchToHost.add(newSwitchToHost);
			parsing3.close();
		}

		/*System.out.println("Inside init()");
		System.out.println("Inside init()");
		System.out.println("Inside init()");*/


	}

	@Override
	public void linkDiscoveryUpdate(LDUpdate update) {
		// Make a Link object for insertion into our links Map object
		// Nope doesn't matter -- this is not where the link updates register
		//Link newLink = new Link(update.getSrc(), update.getSrcPort(), update.getDst(), update.getDstPort());

		// Make a LinkInfo object for insertion into our links Map object
		// firstSeenTime, lastLldpReceivedTime, lastBddpReceivedTime
		// Long firstSeenTime, Long lastLldpReceivedTime, Long lastBddpReceivedTime
		/* None of this matters
		LinkInfo newLinkInfo = new LinkInfo();

		System.out.println("This is what linkDiscoverer returns in linkDiscovery(LDUpdate) : " + this.linkDiscoverer.getLinks());

		System.out.println("Inside linkDiscoveryUpdate)");
		for (Map.Entry<Link, LinkInfo> entry: links.entrySet()) {
			System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
		} */
		/*System.out.println("Inside linkDiscoveryUpdate)");
		System.out.println("Inside linkDiscoveryUpdate)");
		System.out.println("Inside linkDiscoveryUpdate)");*/

	}

	@Override
	public void linkDiscoveryUpdate(List<LDUpdate> updateList) {
		// THIS IS THE ONLY PLACE LINK UPDATES SEEM TO HAPPEN
		//int metaFlipBit = 0; //makes sure we run through this only once
		//if (this.linkDiscoverer.getLinks().size() == N && metaFlipBit == 0){
		if (this.linkDiscoverer.getLinks().size() == N){
			links = this.linkDiscoverer.getLinks();
			//metaFlipBit++;
			//System.out.println("Links is: " + links);

			// ITERATE THRU hostVertices ADJACENCY LISTS AND AUGMENT EACH SWITCH OBJECT'S ADJACENCY
			// LIST WITH THE SWITCHES IT CONNECTS TO, BUT NO PORT INFO

			/*try {
			    Thread.sleep(1000);                 //1000 milliseconds is one second.
			} catch(InterruptedException ex) {
			    Thread.currentThread().interrupt();
			}*/

			int ijj; 
			for (ijj = 0; ijj < aryLines.length; ijj++){

				if( hostVertices.get(ijj) != null ){

					int host_adj_list_cntr;
					for (host_adj_list_cntr = 0; host_adj_list_cntr < hostVertices.get(ijj).adjacencies.size(); host_adj_list_cntr++){
						String hostVerts_switch; 
						hostVerts_switch = hostVertices.get(ijj).adjacencies.get(host_adj_list_cntr).target.swID;

						int cntrx;
						for (cntrx = 0; cntrx < switchToHost.size(); cntrx++){
							if ( hostVerts_switch.equals(switchToHost.get(cntrx).swID) ){ // switches match
								// check to see if we already have this entry in the adjacency list of the switch
								int flipBit2 = 0;
								int sw2host_adj_list_cntr;

								for (sw2host_adj_list_cntr = 0; sw2host_adj_list_cntr < switchToHost.get(cntrx).adjacencies.size(); sw2host_adj_list_cntr++){
									int sw_adj_list_cntr_inside_host;

									for (sw_adj_list_cntr_inside_host = 0; sw_adj_list_cntr_inside_host < hostVertices.get(ijj).adjacencies.get(host_adj_list_cntr).target.adjacencies.size(); sw_adj_list_cntr_inside_host++){

										// This check (for duplicate Switch-->Switch entries) is bad, but I don't know why 
										/*if ( hostVertices.get(ijj).adjacencies.get(host_adj_list_cntr).target.adjacencies.get(sw_adj_list_cntr_inside_host).target.swID.equals(switchToHost.get(cntrx).adjacencies.get(sw2host_adj_list_cntr).target.swID) 
												&& hostVertices.get(ijj).adjacencies.get(host_adj_list_cntr).target.adjacencies.get(sw_adj_list_cntr_inside_host).target.type.equals(switchToHost.get(cntrx).adjacencies.get(sw2host_adj_list_cntr).target.type)){
											flipBit2 = 1;
										}*/
										if ( hostVertices.get(ijj).adjacencies.get(host_adj_list_cntr).target.adjacencies.get(sw_adj_list_cntr_inside_host).target.Host_IP_Addr.equals(switchToHost.get(cntrx).adjacencies.get(sw2host_adj_list_cntr).target.Host_IP_Addr)
												&& hostVertices.get(ijj).adjacencies.get(host_adj_list_cntr).target.adjacencies.get(sw_adj_list_cntr_inside_host).target.type.equals(switchToHost.get(cntrx).adjacencies.get(sw2host_adj_list_cntr).target.type)){
											flipBit2 = 1;
										}
									}
								}

								if (flipBit2 == 0){
									// add it
									int second_cntr;
									for (second_cntr = 0; second_cntr < switchToHost.get(cntrx).adjacencies.size(); second_cntr++){
										hostVertices.get(ijj).adjacencies.get(host_adj_list_cntr).target.adjacencies.add(switchToHost.get(cntrx).adjacencies.get(second_cntr));
									}
								}
							}
						}

						// if swID == switchToHost.get(cntr).swID [[cntr iterates over switchToHost]] 
						// then we know we're talking about the same Switch
						// so add the Hosts this Switch connects to to its adjacency list
						// hostVertices.get(ijj).adjacencies.get(counterthing).target.adjacencies.add(switchToHost.get(cntr).adjacencies.get(second_cntr); 
						// second_cntr iterates over switchToHost.get(cntr).adjacencies.size();


						for (Map.Entry<Link, LinkInfo> entry: links.entrySet()) {
							if ( HexString.toHexString(entry.getKey().getSrc()).equals(hostVerts_switch) ){
								// add the entry.getKey().getDst() to the adjacency list of the switch
								// IF AND ONLY IF it doesn't already contain it
								int flipBit = 0;
								int iterator;
								// check to see if we already have this entry in the adjacency list of the switch
								for (iterator = 0; iterator < hostVertices.get(ijj).adjacencies.get(host_adj_list_cntr).target.adjacencies.size(); iterator++){
									if ( hostVertices.get(ijj).adjacencies.get(host_adj_list_cntr).target.adjacencies.get(iterator).target.swID.equals(HexString.toHexString(entry.getKey().getDst())) ){
										flipBit = 1;
									}
								}
								if (flipBit == 0){
									hostVertices.get(ijj).adjacencies.get(host_adj_list_cntr).target.adjacencies.add(new Edge(new Vertex( "Switch", "-1", HexString.toHexString(entry.getKey().getDst()) ), 1 ));
								}
							}
							//System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());

						}
						
						// NEED TO CONSTRUCT VERTEX OBJECTS FOR SWITCHES WITH NO HOST CONNECTIONS
						// Iterate over links object
							// If entry.getKey().getSrc() is NOT in any Host's Adjacency List
								// Make a Vertex for it
									// Scan through entire links object again, and add Edges
						
						for (Map.Entry<Link, LinkInfo> entry: links.entrySet()) {
							int sthing;
							for (sthing = 0; sthing < aryLines.length; sthing++){
								if( hostVertices.get(sthing) != null ){
									int hostAdjCntr;
									int doNotAdd = 0;
									for ( hostAdjCntr = 0; hostAdjCntr < hostVertices.get(sthing).adjacencies.size(); hostAdjCntr++){
										if ( HexString.toHexString(entry.getKey().getSrc()).equals(hostVertices.get(sthing).adjacencies.get(hostAdjCntr).target.swID) ){
											doNotAdd = 1;
										}
										if ( doNotAdd == 0){
											Vertex newMetaSwitch = new Vertex("Switch", "-1", HexString.toHexString(entry.getKey().getSrc()));
											List<Edge> metaAdjacencies = new ArrayList<Edge>();
											for (Map.Entry<Link, LinkInfo> entry2: links.entrySet()){
												if ( newMetaSwitch.swID.equals(HexString.toHexString(entry2.getKey().getSrc()))){
													metaAdjacencies.add(new Edge( new Vertex("Switch", "-1", HexString.toHexString(entry2.getKey().getDst()) ), 1 ));
												}
											}
											newMetaSwitch.adjacencies = metaAdjacencies;
										}
									}
								}
							}
						}
						// Checking to see if we avoid adding duplicates
						//System.out.println("Adjacency list for: " + hostVerts_switch);
						//System.out.println(hostVertices.get(ijj).adjacencies.get(counterthing).target.adjacencies);
					}
					//Now we need to do this again, but add Switch --> Host connections from switchToHost

				}

			}

			// Pass to Djikstra's for calculating shortest path
			// Djikstra's will need to run for every host in the network
			// ======== RUNNING DJIKSTRAS =========
			/*int djikCntr;
			for (djikCntr = 0; djikCntr < hostVertices.size(); djikCntr++){
				System.out.println("The adjacency list for hostVertices[" + djikCntr + "] is: " + hostVertices.get(djikCntr).adjacencies);
				System.out.println("The adjacency list for the switch connected to hostVertices[" + djikCntr + "] is: " + hostVertices.get(djikCntr).adjacencies.get(0).target.adjacencies);
				Djikstras.computePaths(hostVertices.get(djikCntr));
				int djikCntr2;
				System.out.println("hostVertices.size() is: " + hostVertices.size());
				for (djikCntr2 = 0; djikCntr2 < hostVertices.size(); djikCntr2++){
					if( djikCntr2 != djikCntr ){
						//System.out.println("djikCntr is: " + djikCntr + " djikCntr2 is: " + djikCntr2);
						System.out.println(Djikstras.getShortestPathTo(hostVertices.get(djikCntr2)));
					}
				}
			}*/

			// ============ TESTING DJIKSTRAS
			int sourceVertex = 0;
			int nextVertex = 2;
			
			System.out.println("====== Going to do computePaths on source vertex hostVertices[" + sourceVertex + "]");
			System.out.println("The v.minDistance of the vertex hostVertices[" + sourceVertex + "] is: " + hostVertices.get(sourceVertex).minDistance);
			System.out.println("The v.minDistance of the switch of vertex hostVertices[" + sourceVertex + "] is: " + hostVertices.get(sourceVertex).adjacencies.get(0).target.minDistance);
			Djikstras.computePaths(hostVertices.get(sourceVertex));
			
			/*
			System.out.println("====== Going to do getShortestPathTo hostVertices[" + nextVertex + "]");
			List<Vertex> path = new ArrayList<Vertex>();
			path = Djikstras.getShortestPathTo(hostVertices.get(nextVertex));
			System.out.println("Here is the result: " + path);
			*/
		}



		//System.out.println("After waiting for " + N + " many links to come online, this is what linkDiscoverer returns in LIST<LDUpdate> : " + this.linkDiscoverer.getLinks());

		// PRETTY PRINTER
		/*
		System.out.println("Inside linkDiscoveryUpdate LIST)");
		for (Map.Entry<Link, LinkInfo> entry: links.entrySet()) {
			System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
		}

		System.out.println(this.linkDiscoverer.getLinks());
		 */
		/*System.out.println("Inside linkDiscoveryUpdate(List<LDUpdate> updateList)");
		System.out.println("Inside linkDiscoveryUpdate(List<LDUpdate> updateList)");
		System.out.println("Inside linkDiscoveryUpdate(List<LDUpdate> updateList)");*/

	}
	/*
	public void bossModGenerator(--WhateverDjikstrasGivesMe--) {
		//Needs to iterate over the following functionality for each entry in --WhateverDjikstrasGivesMe--
		//Go through and clean up all the //needs replacing

		for (something : somethingElse){ //iterate through a whole path object for a given vertex
			//from Djikstra's - install flowMods to switches along
			//that path. 


			//Generate a single flowMod message
			OFFlowMod flowMod = (OFFlowMod) floodlightProvider
					.getOFMessageFactory()
					.getMessage(OFType.FLOW_MOD);
			flowMod.setCommand(OFFlowMod.OFPFC_ADD);

			//Generate match for the flowMod
			OFMatch match = new OFMatch();
			//should be setDataLayerDestination or match.setNetworkDestination(int ip address)?
			match.setDataLayerDestination(??WHAT??.getDestinationMACAddress()); //needs replacing 
			match.setInputPort(pi.getInPort()); //needs replacing - should be port of
			//the Switch that it accepts these packets on 
			//(i.e. packets FROM vertex we're iterating over headed TO each target in adjacencies list) 
			match.setWildcards(Wildcards.FULL.matchOn(Flag.IN_PORT, Flag.DL_DST)); //change Flag.DL_DST to NW_DST

			//Applying match to the flowMod
			OFActionOutput action = new OFActionOutput()
			.setPort(lrntable.get(---PORT OF CONTROLLER TO SEND TO SWITCH---));
			//is the port of the Controller that connects to the Switch we want to send
			//the flowMod message to
			List<OFAction> actions = new ArrayList<OFAction>(1);
			actions.add(action);
			flowMod.setActions(actions);

			// misc reformatting to avoid an IOException
			flowMod.setLength((short)(OFFlowMod.MINIMUM_LENGTH + OFActionOutput.MINIMUM_LENGTH)); //assuming you only have one action, which is output
			flowMod.setBufferId(OFPacketOut.BUFFER_ID_NONE); 

			// send flowMod to the Switch
			// send flowMod message to switch
			try {
				sw.write(flowMod, null); //need to define "sw" as the current switch
			} catch (IOException e) {
				log.error("Failure sending flowMod message", e);
			}

		}
	}
	 */
	public static void main(String[] args) throws FloodlightModuleException {

		// Setup logger
		System.setProperty("org.restlet.engine.loggerFacadeClass", 
				"org.restlet.ext.slf4j.Slf4jLoggerFacade");

		CmdLineSettings settings = new CmdLineSettings();
		CmdLineParser parser = new CmdLineParser(settings);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			parser.printUsage(System.out);
			System.exit(1);
		}

		// Load modules
		FloodlightModuleLoader fml = new FloodlightModuleLoader();
		IFloodlightModuleContext moduleContext = fml.loadModulesFromConfig(settings.getModuleFile());
		// Run REST server
		IRestApiService restApi = moduleContext.getServiceImpl(IRestApiService.class);
		restApi.run();
		// Run the main floodlight module
		IFloodlightProviderService controller =
				moduleContext.getServiceImpl(IFloodlightProviderService.class);

		// This call blocks, it has to be the last line in the main
		controller.run();
	}




	@Override
	public String getName() {
		// TODO Auto-generated method stub
		/*System.out.println("Inside getName()");
		System.out.println("Inside getName()");
		System.out.println("Inside getName()");*/

		return null;
	}


	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub

		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		/*System.out.println("Inside isCallbackOrderingPostreq");
		System.out.println("Inside isCallbackOrderingPostreq");
		System.out.println("Inside isCallbackOrderingPostreq");*/

		return false;
	}

	@Override
	public void switchAdded(long switchId) {
		// TODO Auto-generated method stub
		/*System.out.println("Inside switchAdded()");
		System.out.println("Inside switchAdded()");
		System.out.println("Inside switchAdded()");*/

	}

	@Override
	public void switchRemoved(long switchId) {
		// TODO Auto-generated method stub
		/*System.out.println("Inside switchRemoved()");
		System.out.println("Inside switchRemoved()");
		System.out.println("Inside switchRemoved()");*/

	}

	@Override
	public void switchActivated(long switchId) {
		// TODO Auto-generated method stub
		/*System.out.println("Inside switchActivated()");
		System.out.println("Inside switchActivated()");
		System.out.println("Inside switchActivated()");*/

	}

	@Override
	public void switchPortChanged(long switchId, ImmutablePort port,
			PortChangeType type) {
		// TODO Auto-generated method stub
		/*System.out.println("Inside switchPortChanged()");
		System.out.println("Inside switchPortChanged()");
		System.out.println("Inside switchPortChanged()");*/

	}

	@Override
	public void switchChanged(long switchId) {
		// TODO Auto-generated method stub
		/*System.out.println("Inside switchChanged()");
		System.out.println("Inside switchChanged()");
		System.out.println("Inside switchChanged()");*/

	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		/*System.out.println("Inside getModuleServices()");
		System.out.println("Inside getModuleServices()");
		System.out.println("Inside getModuleServices()");*/

		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		/*System.out.println("Inside getServiceImpls()");
		System.out.println("Inside getServiceImpls()");
		System.out.println("Inside getServiceImpls()");*/

		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		// TODO Auto-generated method stub
		/*
		System.out.println("Inside getModuleDependencies()");
		System.out.println("Inside getModuleDependencies()");
		System.out.println("Inside getModuleDependencies()");*/

		return null;
	}

	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		// TODO Auto-generated method stub

		/*System.out.println("Inside startUp");
		System.out.println("Inside startUp");
		System.out.println("Inside startUp");*/


	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(
			IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		// TODO Auto-generated method stub
		/*System.out.println("Inside Ilistener.Command receive");
		System.out.println("Inside Ilistener.Command receive");
		System.out.println("Inside Ilistener.Command receive");*/


		return null;
	}


}
