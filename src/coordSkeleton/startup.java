package coordSkeleton;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

public class startup {

	public static void main(String [] args)
	{
		int peers = 5; // Integer.valueOf(args[0]);			
		int port = 8600;
		ArrayList<peer> myPeers = new ArrayList<peer>();
		ArrayList<Thread> threads = new ArrayList<Thread>();
		int [] ports = new int[peers];
		for(int i = 0; i < peers;i++) ports[i] = port + i;
		for(int i  = 0; i < peers; i++)
		{
			myPeers.add(new peer(5, i, ports[i], ports));
			threads.add(new Thread(myPeers.get(i)));
		}
		
		for(int i = 0; i < peers;i++) myPeers.get(i).buildMesh();
		
		// starten der threads von den peers, diese sollten nun alle bereit sein
		for(int i = 0; i < peers;i++) threads.get(i).start();
		
		
		while(true){
			int active =0;
			for(int i = 0; i < peers; i++) 
				{
					if(!myPeers.get(i).isActive()) active++;
				}
			if (active == peers) return;
		}
	}

	
}
