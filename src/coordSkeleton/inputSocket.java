package coordSkeleton;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class inputSocket implements Runnable{

    private Socket targetSocket;
    private peer hostClass;
    private boolean run;

    public inputSocket(peer hostClass, Socket targetSocket)
    {
        this.targetSocket = targetSocket;
        this.hostClass = hostClass;
        this.run = true;
    }

    public void run() {
        while(run)
        {
            byte [] data = new byte[200];
            try {
                if (targetSocket.isClosed()) {
                    run = false;
                } else {
                    DataInputStream is = new DataInputStream(targetSocket.getInputStream());
                    int bread = is.read(data);
                    if (bread > 0) {
                        message msg = new message(data);
                        this.hostClass.handleMessage(new peerInformation(msg.getPeerId(), targetSocket), msg);
                    }
                }
            } catch (IOException e) {
                run = false;
            }
        }
    }

    public void close()
    {
        run = false;
    }
}
