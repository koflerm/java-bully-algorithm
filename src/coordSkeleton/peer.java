package coordSkeleton;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class peer implements Runnable{


	private int id;
	private int port;
	private listenSocket listener; // der Serversocket zum Verbinden der Anderen Peers/Clients/Slaves
	private boolean run = false;
	//private ArrayList<Socket> connectedPeers;
	//private ArrayList<Socket> connectionToPeers;
	private ArrayList<peerInformation> connectionToPeers;
	private ArrayList<Socket> connectedPeers;
	private ArrayList<message> electionMessages;
	private ArrayList<Thread> listenerThreads;

	private boolean isCoordinator;
	private boolean coordiatorElected;
	private boolean initialCoordinatorElected;
	private boolean electionInProgress;
	private boolean killJobStarted;
	private boolean receivedAnswer;
	private long electionTimeout;
	private peerInformation currentCoordinator;
	private message lastHeartbeat;
	private int [] peers;
	private long coordinatorTTL; // in Sekunden
	Thread listenerThread;
	public peer(long coordinatorTTL, int id, int port, int [] peers)
	{
		this.id = id;
		this.port = port;
		run = true;
		this.killJobStarted = false;
		this.coordinatorTTL = coordinatorTTL;
		this.listener = new listenSocket(this, port);
		this.isCoordinator = false;
		this.coordiatorElected = false;
		this.electionInProgress = false;
		this.initialCoordinatorElected = false;
		this.receivedAnswer = false;
		this.lastHeartbeat = null;
		//connectedPeers = new ArrayList<Socket>();
		connectionToPeers = new ArrayList<peerInformation>();
		connectedPeers = new ArrayList<Socket>();
		electionMessages = new ArrayList<message>();
		listenerThreads = new ArrayList<Thread>();
		this.peers = peers;
		System.out.println("Starting listener of peer: " + id);
		listenerThread = new Thread(listener);
		listenerThread.start();
		
	}

	public boolean isActive()
	{
		return run;
	}
	
	/*
	 * Baue das Netzwerk - in unserem Fall ein Mesh
	 */
	public void buildMesh()
	{
		for(int i = 0; i < peers.length;i++)
		{
			if(peers[i] != port)
			{
				try {
					this.debugMessage("connecting to peer with port: " + peers[i]);
					Socket clS = new Socket("localhost", peers[i]);
					byte [] data = new byte[message.getMessageSize()];
					(new DataInputStream(clS.getInputStream())).read(data);
					message helloMsg = new message(data);
					if(clS.isConnected() && helloMsg.getType() == message.HELLO) 
					{
						debugMessage("received hello!");
						//connectionToPeers.add(clS);
						connectionToPeers.add(new peerInformation(helloMsg.getPeerId(), clS));
					}
					else
					{
						debugMessage("could not connect to peer with port: "+ peers[i]);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void run() {
		try
		{
			Thread.sleep(1000);
		}
		catch(InterruptedException ex)
		{
			Thread.currentThread().interrupt();
		}

		// Falls wird der Koordinator sind, simuliere Knotenausfall

		this.startListenerThreadsToPeers();

		while(run)
		{
			// 1. Prüfe periodisch ob der derzeitige Koordinator online ist - wenn wir nicht selbst der Koordinator sind
			// 2. Wenn dieser nicht Online ist initiiere den Bully-Algorithmus und bestimme aus den bestehenden Peers einen neuen Koordinator

			if(isCoordinator && initialCoordinatorElected)
			{
				if(killJobStarted == false)
				{
					debugMessage("Starting kill timer...");
					Timer timer = new Timer();
					timer.schedule(new TimerTask() {
						public void run() {
							stop();
						}
					}, coordinatorTTL*1000);
					killJobStarted = true;
				}
				debugMessage("I am the leader so time for some coffee");
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else
			{
				if (!receivedAnswer && electionTimeout != 0 && electionTimeout < System.currentTimeMillis()) {
					debugMessage("I won the election! Sending VICTORY messages");
					this.sendVictoryMessages();
				} else {
					// if we are not the coordinator send something - its the peer with the highest id/index
					try {
						if (coordiatorElected) {
							if (lastHeartbeat != null && lastHeartbeat.getExpires() > System.currentTimeMillis()) {
								debugMessage("CRAP! We lost the leader!");
								coordiatorElected = false;
							} else {
								DataOutputStream os = new DataOutputStream(currentCoordinator.getConnection().getOutputStream());
								message msg = new message(message.HEARTBEAT, id, System.currentTimeMillis() + 2000);
								os.write(msg.toByteArray());
								os.flush();
							}
							Thread.sleep(1000);
						} else {
							// Start new election
							this.startElection();
							Thread.sleep(1000);
						}
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					} catch (IOException e) {
						debugMessage("CRAP! We lost the leader!");
						for (int i = 0; i < connectionToPeers.size(); i++)
							if (connectionToPeers.get(i).getPeerid() == currentCoordinator.getPeerid())
								connectionToPeers.remove(connectionToPeers.get(i));
						coordiatorElected = false;
						currentCoordinator = null;
					}
				}
			}
		}
	}

	public void startElection() {
		if (initialCoordinatorElected && !receivedAnswer) {
			debugMessage("Sending election messages");
			if (this.electionTimeout == 0) {
				this.electionTimeout = System.currentTimeMillis() + 5000;
			}
			for(int i = 0; i < connectionToPeers.size(); i++) {
				if (connectionToPeers.get(i).getPeerid() > id) {
					try {
						DataOutputStream os = new DataOutputStream(connectionToPeers.get(i).getConnection().getOutputStream());
						message msg = new message(message.ELECTION, id, System.currentTimeMillis() + 2000);
						os.write(msg.toByteArray());
						os.flush();
					} catch (IOException e) {

					}
				}
			}
		} else {
			int higherPeers = getHigherPeers();

			if (getHigherPeers() == 0) {
				debugMessage("I am the initial leader! Sending VICTORY messages");
				this.initialCoordinatorElected = true;
				this.sendVictoryMessages();
			}
		}
	}

	public void sendVictoryMessages() {
		this.isCoordinator = true;
		this.electionInProgress = false;
		this.coordiatorElected = true;
		for (int i = 0; i < connectionToPeers.size(); i++) {
			try {
				DataOutputStream os = new DataOutputStream(connectionToPeers.get(i).getConnection().getOutputStream());
				message msg = new message(message.VICTORY, id);
				byte[] byteMsg = msg.toByteArray();
				os.write(byteMsg);
				os.flush();

			} catch (IOException e) {
				debugMessage("Failed to send VICTORY message to peer " + connectionToPeers.get(i).getPeerid());
				connectionToPeers.remove(connectionToPeers.get(i));
			}
		}
	}

	public int getHigherPeers() {
		int higherPeers = 0;
		for(int i = 0; i < connectionToPeers.size(); i++) {
			if (connectionToPeers.get(i).getPeerid() > id) {
				higherPeers++;
			}
		}

		return higherPeers;
	}

	public void startListenerThreadsToPeers() {
		for(int i = 0; i < connectionToPeers.size(); i++)
		{
			Thread th = new Thread(new inputSocket(this, connectedPeers.get(i)));
			th.start();
			listenerThreads.add(th);
		}
	}

	public void handleMessage(peerInformation peer, message msg)
	{
		switch(msg.getType())
		{
		case message.HEARTBEAT:
			DataOutputStream os;
			peer = getPeerInformationById(peer.getPeerid());
			try {
				os = new DataOutputStream(peer.getConnection().getOutputStream());
				os.write((new message(message.ALIVE, id)).toByteArray());
				os.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
		case message.ELECTION:
			debugMessage("Received Election Message from peer " + peer.getPeerid());
			peer = getPeerInformationById(peer.getPeerid());
			DataOutputStream outp;
			try {
				outp = new DataOutputStream(peer.getConnection().getOutputStream());
				outp.write((new message(message.ANSWER, id)).toByteArray());
				outp.flush();

				if (!electionInProgress) {
					this.electionInProgress = true;
					this.startElection();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
		case message.VICTORY:
			debugMessage("Praise the lord a new leader! It is peer " + peer.getPeerid());
			this.currentCoordinator = getPeerInformationById(peer.getPeerid());
			this.electionInProgress = false;
			this.coordiatorElected = true;
			this.initialCoordinatorElected = true;
			this.receivedAnswer = false;
			break;
		case message.ALIVE:
			this.lastHeartbeat = null;
			break;
		case message.ANSWER:
			debugMessage("Received an answer. Sending no more election messages");
			this.receivedAnswer = true;
			this.electionTimeout = 0;
			break;
		default:
			debugMessage("unknown message type!");
		}

	}

	private peerInformation getPeerInformationById(int peerId) {
		for(int i = 0; i < connectionToPeers.size(); i++) {
			if (connectionToPeers.get(i).getPeerid() == peerId) {
				return connectionToPeers.get(i);
			}
		}
		return null;
	}

	public int getId()
	{
		return id;
	}

	public boolean alreadyConnected(int remotePort)
	{

		return false;
	}

	public synchronized void addPeer(Socket clientSocketPeer) {
		message helloMsg = new message(message.HELLO, id);
		try {
			DataOutputStream os = new DataOutputStream(clientSocketPeer.getOutputStream());
			byte[] byteHelloMsg = helloMsg.toByteArray();
			os.write(byteHelloMsg);
			os.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.connectedPeers.add(clientSocketPeer);
	}

	public void debugMessage(String text)
	{
		System.out.println((isCoordinator ? "Coordinator (peer"+ id +" ): " : "Peer " + id + ": ") + text);
	}

	public void stop()
	{
		run = false;
		debugMessage("dying.");
		try {

			listener.close();
			for(int i = 0; i < listenerThreads.size(); i++) listenerThreads.get(i).interrupt();
			for(int i = 0; i < connectionToPeers.size(); i++) connectionToPeers.get(i).getConnection().close();
			for(int i = 0; i < connectedPeers.size(); i++) connectedPeers.get(i).close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
