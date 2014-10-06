package net.floodlightcontroller.Assignment3;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFType;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Assignment3 implements ILinkDiscoveryListener, IOFMessageListener,
		IFloodlightModule, IOFSwitchListener {
	
	protected ILinkDiscoveryService linkDiscoverer;
	protected IFloodlightProviderService floodlightProvider;
	protected Map<Link, LinkInfo> links;
	protected static Logger log = LoggerFactory.getLogger(Assignment3.class);

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		
		Map<String, String> configParams = context.getConfigParams(this);
		String TopologyInfo = configParams.get("Topology Info");
		String NumSwitches = configParams.get("N");
		
		// From Slides
		this.floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);

		// add self as one of switch event listeners
		this.floodlightProvider.addOFSwitchListener(this);
		
		// add self as one of link event listeners
		this.linkDiscoverer = context.getServiceImpl(ILinkDiscoveryService.class);
		this.linkDiscoverer.addListener(this);
		
		// Get Map of current topology
		links = this.linkDiscoverer.getLinks();
		System.out.println("This is what linkDiscoverer returns in init() : " + this.linkDiscoverer.getLinks());

		// Run OSPF on the 'links' Map
		// Probably should do this as a separate method that happens every time
		// the links object changes
		
		/*System.out.println("Inside init()");
		System.out.println("Inside init()");
		System.out.println("Inside init()");*/
		

	}
	
	@Override
	public void linkDiscoveryUpdate(LDUpdate update) {
		// Make a Link object for insertion into our links Map object
		Link newLink = new Link(update.getSrc(), update.getSrcPort(), update.getDst(), update.getDstPort());

		// Make a LinkInfo object for insertion into our links Map object
		// firstSeenTime, lastLldpReceivedTime, lastBddpReceivedTime
		// Long firstSeenTime, Long lastLldpReceivedTime, Long lastBddpReceivedTime
		LinkInfo newLinkInfo = new LinkInfo();

		System.out.println("This is what linkDiscoverer returns in linkDiscovery(LDUpdate) : " + this.linkDiscoverer.getLinks());
		
		System.out.println("Inside linkDiscoveryUpdate)");
		for (Map.Entry<Link, LinkInfo> entry: links.entrySet()) {
			System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
		}
		/*System.out.println("Inside linkDiscoveryUpdate)");
		System.out.println("Inside linkDiscoveryUpdate)");
		System.out.println("Inside linkDiscoveryUpdate)");*/

	}

	@Override
	public void linkDiscoveryUpdate(List<LDUpdate> updateList) {
		// TODO Auto-generated method stub
		
		links = this.linkDiscoverer.getLinks();
		System.out.println("This is what linkDiscoverer returns in LIST<LDUpdate> : " + this.linkDiscoverer.getLinks());
		
		System.out.println("Inside linkDiscoveryUpdate LIST)");
		for (Map.Entry<Link, LinkInfo> entry: links.entrySet()) {
			System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
		}
		
		/*System.out.println("Inside linkDiscoveryUpdate(List<LDUpdate> updateList)");
		System.out.println("Inside linkDiscoveryUpdate(List<LDUpdate> updateList)");
		System.out.println("Inside linkDiscoveryUpdate(List<LDUpdate> updateList)");*/

	}

	
    public static void main(String[] args) throws FloodlightModuleException {
    	
    	// Setup logger
        System.setProperty("org.restlet.engine.loggerFacadeClass", 
                "org.restlet.ext.slf4j.Slf4jLoggerFacade");
        
        // DEBUG STATEMENTS THAT WORK
        // Both of these work
        //System.out.println("Entering public static void main qqqq");
        //log.error("Enteirng public static void main lllll");
        
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
