package net.floodlightcontroller.Assignment3;

// FROM https://github.com/adamvail/floodlight-modules/blob/master/edu/wisc/cs/bootcamp/sdn/routing/Dijkstra.java

import java.util.PriorityQueue;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import org.openflow.util.*;

// THIS NEEDS TO PASS TO OUR FLOWMOD GENERATOR
// Djikstra's will need to run for every host in the network


class Vertex implements Comparable<Vertex> 
{
	public final String type; // "Host" or "Switch"
	public final String Host_IP_Addr; //if Host, this object has this, else -1 
	public final long swID; //if Switch, this object has this, else -1
	//public final short swPort; //if Switch, this object has this, else -1 
	public Edge[] adjacencies; //contains all IP's (hosts) and DPID's (switches) it connects to
							   //for DPID's it connects to, should contain their inPort
	public double minDistance = Double.POSITIVE_INFINITY;
	public Vertex previous; 
	public Vertex(String argName, String hostAddr, long switchID) { 
		type = argName; 
		Host_IP_Addr = hostAddr; 
		swID = switchID; 
		//swPort = switchPort;
		}
	public String toString() { return "" + type + " " + "Host IP ADDR: " + Host_IP_Addr + " Switch DPID: " + swID + "\n"; }
	public int compareTo(Vertex other)
	{
		return Double.compare(minDistance, other.minDistance);
	}
}


/*class Vertex implements Comparable<Vertex>
{
	public final String name; //should be IP or DPID
	public Edge[] adjacencies; //contains all IP's (hosts) and DPID's (switches) it connects to
	public double minDistance = Double.POSITIVE_INFINITY;
	public Vertex previous; //is linked to another Vertex as its previous (?)
	public Vertex(String argName) { name = argName; }
	public String toString() { return "" + name; }
	public int compareTo(Vertex other)
	{
		return Double.compare(minDistance, other.minDistance);
	}
}*/

class Edge
{
	public final Vertex target; //indicates the second member of the ordered vertex pair (vi,vj)
	public final double weight; //edge weight - should just be 1 for all edges for us
	public Edge(Vertex argTarget, double argWeight) //constructor for an Edge
	{ target = argTarget; weight = argWeight; }
}
public class Djikstras
{
	public static void computePaths(Vertex source) //takes the Vertex source to do shortest path from
	{
		source.minDistance = 0.;
		PriorityQueue<Vertex> vertexQueue = new PriorityQueue<Vertex>();
		vertexQueue.add(source);
		while (!vertexQueue.isEmpty()) {
			Vertex u = vertexQueue.poll();
			// Visit each edge exiting u
			for (Edge e : u.adjacencies)
			{
				Vertex v = e.target;
				double weight = e.weight;
				double distanceThroughU = u.minDistance + weight;
				if (distanceThroughU < v.minDistance) {
					vertexQueue.remove(v);
					v.minDistance = distanceThroughU ;
					v.previous = u;
					vertexQueue.add(v);
				}
			}
		}
	}
	public static List<Vertex> getShortestPathTo(Vertex target) //gives shortest path from source
																//to target
	//should iterate through for all hosts to all other hosts
	//should return a list of Switch DPID's terminating in a host IP
	{
		List<Vertex> path = new ArrayList<Vertex>(); //path is a list of Vertices
		for (Vertex vertex = target; vertex != null; vertex = vertex.previous)
			path.add(vertex);
		Collections.reverse(path);
		return path;
	}
	/*
	public static void example()
	{
		
		Vertex v0 = new Vertex("Harrisburg");
		Vertex v1 = new Vertex("Baltimore");
		Vertex v2 = new Vertex("Washington");
		Vertex v3 = new Vertex("Philadelphia");
		Vertex v4 = new Vertex("Binghamton");
		Vertex v5 = new Vertex("Allentown");
		Vertex v6 = new Vertex("New York");
		v0.adjacencies = new Edge[]{ new Edge(v1, 79.83),
				new Edge(v5, 81.15) };
		v1.adjacencies = new Edge[]{ new Edge(v0, 79.75),
				new Edge(v2, 39.42),
				new Edge(v3, 103.00) };
		v2.adjacencies = new Edge[]{ new Edge(v1, 38.65) };
		v3.adjacencies = new Edge[]{ new Edge(v1, 102.53),
				new Edge(v5, 61.44),
				new Edge(v6, 96.79) };
		v4.adjacencies = new Edge[]{ new Edge(v5, 133.04) };
		v5.adjacencies = new Edge[]{ new Edge(v0, 81.77),
				new Edge(v3, 62.05),
				new Edge(v4, 134.47),
				new Edge(v6, 91.63) };
		v6.adjacencies = new Edge[]{ new Edge(v3, 97.24),
				new Edge(v5, 87.94) };
		Vertex[] vertices = { v0, v1, v2, v3, v4, v5, v6 };
		computePaths(v0);
		for (Vertex v : vertices)
		{
			System.out.println("Distance to " + v + ": " + v.minDistance);
			List<Vertex> path = getShortestPathTo(v);
			System.out.println("Path: " + path);
		}
		 
	}
	*/
}
