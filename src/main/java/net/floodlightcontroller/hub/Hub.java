/**
*    Copyright 2011, Big Switch Networks, Inc. 
*    Originally created by David Erickson, Stanford University
* 
*    Licensed under the Apache License, Version 2.0 (the "License"); you may
*    not use this file except in compliance with the License. You may obtain
*    a copy of the License at
*
*         http://www.apache.org/licenses/LICENSE-2.0
*
*    Unless required by applicable law or agreed to in writing, software
*    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
*    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
*    License for the specific language governing permissions and limitations
*    under the License.
**/

package net.floodlightcontroller.hub;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Hashtable; 
import java.util.List;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.Ethernet;

import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.action.OFActionType;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.Wildcards;
import org.openflow.protocol.Wildcards.Flag;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.util.U16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author David Erickson (daviderickson@cs.stanford.edu) - 04/04/10
 */
public class Hub implements IFloodlightModule, IOFMessageListener {
    protected static Logger log = LoggerFactory.getLogger(Hub.class);
    
    protected IFloodlightProviderService floodlightProvider;

    protected Hashtable<Long, Short> lrntable = new Hashtable<Long, Short>();
    protected Hashtable<Long, Hashtable> meta_table = new Hashtable<Long, Hashtable>();
    
    List<OFAction> actions = null;
    
    /**
     * @param floodlightProvider the floodlightProvider to set
     */
    public void setFloodlightProvider(IFloodlightProviderService floodlightProvider) {
        this.floodlightProvider = floodlightProvider;
    }

    @Override
    public String getName() {
        return Hub.class.getPackage().getName();
    }

    public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {        
    	
    	OFPacketIn pi = (OFPacketIn) msg;
        OFPacketOut po = (OFPacketOut) floodlightProvider.getOFMessageFactory()
                .getMessage(OFType.PACKET_OUT);
        po.setBufferId(pi.getBufferId())
            .setInPort(pi.getInPort());
        
        // enter the meta_table to find the right switch ID
        if(!meta_table.containsKey(sw.getId())) {
        	Hashtable<Long, Short> newtable = new Hashtable<Long, Short>();
        	meta_table.put(sw.getId(), newtable);
        }
        
        lrntable = meta_table.get(sw.getId());
        
        // learning switch
        Ethernet eth =
                IFloodlightProviderService.bcStore.get(cntx,
                                            IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
        
        Long sourceMACHash = Ethernet.toLong(eth.getSourceMACAddress());
        Long destMACHash = Ethernet.toLong(eth.getDestinationMACAddress());
        
        // why are we .toLong the source and dest MAC addrs? careful here
        
        if(!lrntable.containsKey(sourceMACHash)) {
        	lrntable.put(sourceMACHash, pi.getInPort());
        }
        
        if(lrntable.containsKey(destMACHash)){
        	//use the port value associated with the key to make the packet-out
        	OFActionOutput action = new OFActionOutput()
        		.setPort(lrntable.get(destMACHash));
        	po.setActions(Collections.singletonList((OFAction)action));
            po.setActionsLength((short) OFActionOutput.MINIMUM_LENGTH);

            // set data if is is included in the packetin
            if (pi.getBufferId() == OFPacketOut.BUFFER_ID_NONE) {
                byte[] packetData = pi.getPacketData();
                po.setLength(U16.t(OFPacketOut.MINIMUM_LENGTH
                        + po.getActionsLength() + packetData.length));
                po.setPacketData(packetData);
            } else {
                po.setLength(U16.t(OFPacketOut.MINIMUM_LENGTH
                        + po.getActionsLength()));
            }
            // sends pkt-out to the switch
            try {
                sw.write(po, cntx);
            } catch (IOException e) {
                log.error("Failure writing PacketOut", e);
            }
            
            // create flowMod message for this destMAC
            OFFlowMod flowMod = (OFFlowMod) floodlightProvider
            		.getOFMessageFactory()
            		.getMessage(OFType.FLOW_MOD);
            flowMod.setCommand(OFFlowMod.OFPFC_ADD);
            
            // now scrub match from the packet received
            OFMatch match = new OFMatch();
            match.setDataLayerDestination(eth.getDestinationMACAddress());
            match.setInputPort(pi.getInPort());
            match.setWildcards(Wildcards.FULL.matchOn(Flag.IN_PORT, Flag.DL_DST));
            
            // now put match and pkt-out actions together in this flowMod
            flowMod.setMatch(match);
            List<OFAction> actions = new ArrayList<OFAction>(1);
            actions.add(action);
            flowMod.setActions(actions);
            
            // misc reformatting to avoid an IOException
            flowMod.setLength((short)(OFFlowMod.MINIMUM_LENGTH + OFActionOutput.MINIMUM_LENGTH)); //assuming you only have one action, which is output
            flowMod.setBufferId(OFPacketOut.BUFFER_ID_NONE); 
            
            // send flowMod message to switch
            try {
            	sw.write(flowMod, null);
            } catch (IOException e) {
            	log.error("Failure sending flowMod message", e);
            }
            
        }

        else {
        	//make packetout and set port to flood, send pkt-out to switch
            // set actions
            OFActionOutput action = new OFActionOutput()
                .setPort(OFPort.OFPP_FLOOD.getValue());
            po.setActions(Collections.singletonList((OFAction)action));
            po.setActionsLength((short) OFActionOutput.MINIMUM_LENGTH);

            // set data if is is included in the packetin
            if (pi.getBufferId() == OFPacketOut.BUFFER_ID_NONE) {
                byte[] packetData = pi.getPacketData();
                po.setLength(U16.t(OFPacketOut.MINIMUM_LENGTH
                        + po.getActionsLength() + packetData.length));
                po.setPacketData(packetData);
            } else {
                po.setLength(U16.t(OFPacketOut.MINIMUM_LENGTH
                        + po.getActionsLength()));
            }
            try {
                sw.write(po, cntx);
            } catch (IOException e) {
                log.error("Failure writing PacketOut", e);
            }

        }
        
        return Command.CONTINUE;
    }

    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
        return false;
    }

    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
        return false;
    }

    // IFloodlightModule
    
    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        // We don't provide any services, return null
        return null;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService>
            getServiceImpls() {
        // We don't provide any services, return null
        return null;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>>
            getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> l = 
                new ArrayList<Class<? extends IFloodlightService>>();
        l.add(IFloodlightProviderService.class);
        return l;
    }

    @Override
    public void init(FloodlightModuleContext context)
            throws FloodlightModuleException {
        floodlightProvider =
                context.getServiceImpl(IFloodlightProviderService.class);
    }

    @Override
    public void startUp(FloodlightModuleContext context) {
        floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
    }
}
