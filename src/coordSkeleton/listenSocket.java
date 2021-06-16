package coordSkeleton;

import java.io.IOException;
import java.net.*;

class addSocket implements Runnable  {
	private Socket sock;
	private peer p;
	public addSocket(peer p, Socket sock)
	{
		this.sock = sock;
		this.p = p;
	}
	public void run()
	{
		p.addPeer(sock);
	}
}

public class listenSocket implements Runnable{
	
	private int listenPort;
	private ServerSocket socket; 
	private peer hostClass;
	private boolean run = false;
	
	public listenSocket(peer hostClass, int port)
	{
		try {
		
			this.socket = new ServerSocket(port);
			this.hostClass = hostClass;
			run = true;
	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void run() {
	
		while(run)
		{
			try {
				this.hostClass.debugMessage("listener waiting for incoming connections.");
				Socket sock = socket.accept();
				this.hostClass.debugMessage("connection from " + sock.getPort());
				new Thread(new addSocket(this.hostClass, sock)).start();
			} catch (IOException e) {
				run = false;
			}
		}
		
	}
	
	public void close() throws IOException
	{
		socket.close();
		run = false;
	}
}
