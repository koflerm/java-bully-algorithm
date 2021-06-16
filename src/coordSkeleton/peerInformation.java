package coordSkeleton;

import java.net.Socket;

public class peerInformation {
    private int peerid;
    private Socket connection;

    public peerInformation(int peerid, Socket connection) {
        this.peerid = peerid;
        this.connection = connection;
    }

    public Socket getConnection() {
        return connection;
    }

    public int getPeerid() {
        return peerid;
    }
}
