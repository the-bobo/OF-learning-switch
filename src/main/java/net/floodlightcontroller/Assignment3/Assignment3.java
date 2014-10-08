package net.floodlightcontroller.Assignment3;

import java.io.IOException;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {

		Map<String, String> configParams = context.getConfigParams(this);
		String TopologyInfo = configParams.get("Topology Info");
		String NumSwitches = configParams.get("Num");
		System.out.println("Here is what NumSwitches looks like: " + NumSwitches);
		N = Integer.parseInt(NumSwitches); //NEEDS TO BE N = args[0] FOR COMMAND LINE TO WORK

		// From Slides
		this.floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);

		// add self as one of switch event listeners
		this.floodlightProvider.addOFSwitchListener(this);

		// add self as one of link event listeners
		this.linkDiscoverer = context.getServiceImpl(ILinkDiscoveryService.class);
		this.linkDiscoverer.addListener(this);

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

		if (this.linkDiscoverer.getLinks().size() == N){
			links = this.linkDiscoverer.getLinks();
		}


		// Pass to Djikstra's for calculating shortest path
		// Djikstra's will need to run for every host in the network

		//System.out.println("After waiting for " + N + " many links to come online, this is what linkDiscoverer returns in LIST<LDUpdate> : " + this.linkDiscoverer.getLinks());

		// PRETTY PRINTER
		/*
		System.out.println("Inside linkDiscoveryUpdate LIST)");
		for (Map.Entry<Link, LinkInfo> entry: links.entrySet()) {
			System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
		}*/

		/*System.out.println("Inside linkDiscoveryUpdate(List<LDUpdate> updateList)");
		System.out.println("Inside linkDiscoveryUpdate(List<LDUpdate> updateList)");
		System.out.println("Inside linkDiscoveryUpdate(List<LDUpdate> updateList)");*/

	}

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


		//GET INPUT FILE TO SCRUB HOST-->SWITCH CONNECTIONS FOR DJIKSTRA
		String file_name = args[1];
		file_name = "./tilepuzinput.txt"; //REPLACE WITH PATH TO FILE FROM COMMAND LINE ARG
		try{
		ReadFile file = new ReadFile(file_name);
		aryLines = file.OpenFile();
		}
		catch (IOException f){
			System.out.println(f.getMessage());
			System.out.println("\n ERROR: First command line arg must be number of switches as a single integer");
			System.out.println("\n ERROR: Second command line arg must be path to Input File with Host-->Switch topology \n");
		}
		
		int iterator_counter = 0;
		for (iterator_counter = 0; iterator_counter < aryLines.length; iterator_counter++){
			System.out.println(aryLines[iterator_counter]);
		}

		// PARSE THE FILE INPUT BY SPLITTING ON COMMAS, AND USING THAT TO CREATE VERTEX OBJECTS

		
		for (String item : aryLines){
			//each item in the array is a String row: "10.0.0.1, 00:00:00...:01, 1"
			//This stores our Vertices for Djikstras, but does not associate port info w/ switches
			Scanner parsing = new Scanner(item);
			Vertex newHost = new Vertex("Host", parsing.nextLong(), (long)-1);
			Vertex newSwitch = new Vertex("Switch", (long)-1, parsing.nextLong());
			newHost.adjacencies = new Edge[]{
					new Edge(newSwitch, 1)
			};
			hostVertices.add(newHost);
		}
		
		for (String item: aryLines){
			//This will tell us which Switches connect to which Hosts,
			//along with the portInfo
			Scanner parsing2 = new Scanner(item);
			Vertex newTerminalHost = new Vertex("Host", parsing2.nextLong(), (long)-1); 
			FullSwitchToHost newFullSwitchToHost = new FullSwitchToHost(parsing2.nextLong(), parsing2.nextShort(), newTerminalHost);
			switchToHostWithPortInfo.add(newFullSwitchToHost);
		}
		
		// ITERATE THRU hostVertices ADJACENCY LISTS AND AUGMENT EACH SWITCH OBJECT'S ADJACENCY
		// LIST WITH THE SWITCHES IT CONNECTS TO, BUT NO PORT INFO
		
		// THEN, DO IT AGAIN, BUT STORE PORT INFO

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
