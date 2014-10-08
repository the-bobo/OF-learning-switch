package net.floodlightcontroller.Assignment3;

import java.util.*;

public class FullSwitch {

	public final long switchDPID;
	public final List<FullSwitchNeighbor> neighbors = new ArrayList<FullSwitchNeighbor>();
	
	public FullSwitch(long argDPID, FullSwitchNeighbor argNeighbors){
		switchDPID = argDPID;
		neighbors.add(argNeighbors);
	}

}

class FullSwitchNeighbor{ //is a list of neighboring switches for a given 'home' switch
	public final long target_swDPID;
	public final short original_swOutPort; //is the outPort of the home switch that connects to this neighbor
	
	public FullSwitchNeighbor(long arg_targDPID, short arg_orig_swOutPort){
		target_swDPID = arg_targDPID;
		original_swOutPort = arg_orig_swOutPort;
	}
}

class FullSwitchToHost{ //for a switch connecting to a host
	public final long switchDPID_t; //same thing as above switchDPID, just uniquified name in case i need to in java...
	public final short switchOutPort;
	public final Vertex the_host_this_switch_knows;
	
	public FullSwitchToHost(long argDPID_t, short arg_switchOutPort, Vertex argHost){
		switchDPID_t = argDPID_t;
		switchOutPort = arg_switchOutPort;
		the_host_this_switch_knows = argHost;
	}
}