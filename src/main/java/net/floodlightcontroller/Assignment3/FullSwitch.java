package net.floodlightcontroller.Assignment3;

public class FullSwitch {

	public final long switchDPID;
	public final FullSwitchNeighbor[] neighbors;
	
	public FullSwitch(long argDPID, FullSwitchNeighbor[] argNeighbors){
		switchDPID = argDPID;
		neighbors = argNeighbors;
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
