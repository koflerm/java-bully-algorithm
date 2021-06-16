package coordSkeleton;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class message {
	
	public static final int ELECTION = 0x01;
	public static final int VICTORY = 0x03;
	public static final int ANSWER = 0x02;
	public static final int HEARTBEAT = 0xFF;
	public static final int ALIVE = 0x0F;
	public static final int HELLO = 0x09;
	
	private int type;	// der typ der nachricht
	private int peerId; // die id des Peers
	private long expires;
	
	public message(int type, int peerId)
	{
		this.type = type;
		this.peerId = peerId;
	}

	public message(int type, int peerId, long expires) {
		this.type = type;
		this.peerId = peerId;
		this.expires = expires;
	}
	
	public message(byte [] data)
	{
		this.type = ByteBuffer.wrap(Arrays.copyOfRange(data, 0, 4)).getInt();
		this.peerId = ByteBuffer.wrap(Arrays.copyOfRange(data, 4, 8)).getInt();
		if (this.type == message.ELECTION || this.type == message.HEARTBEAT) {
			this.expires = ByteBuffer.wrap(Arrays.copyOfRange(data, 8, 16)).getLong();
		}
	}
		
	public byte [] toByteArray()
	{
		ByteBuffer b =  ByteBuffer.allocate(getMessageSize());
		b.putInt(this.type);
		b.putInt(this.peerId);
		if (this.type == message.ELECTION || this.type == message.HEARTBEAT) {
			b.putLong(this.expires);
		}
		return b.array();
	}
	
	public static final int getMessageSize()
	{
		return 2*Integer.BYTES + Long.BYTES;
	}
	
	public int getType()
	{
		return type;
	}
	
	public int getPeerId()
	{
		return peerId;
	}

	public long getExpires() {
		return expires;
	}
}
