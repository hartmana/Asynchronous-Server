package cs455.scaling.server;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.SocketChannel;
import java.util.Deque;
import java.util.LinkedList;

/**
 * A ClientInfo class
 *
 * @author ahrtmn, 01 03 2014
 */
public class ClientInfo
{
    /**
     * Channel associated with this client
     */
    private final Channel channel;

    /**
     * Deque to be used for pending information to be written to the client
     */
    private final Deque<byte[]> pendingWriteList;

    /**
     * SocketAddress of the remote client
     */
    private final SocketAddress remoteAddress;



    /**
     * Constructor that takes a channel and gathers all client connection info
     *
     * @param channel <code>Channel</code> of the incoming connection for client info to be processed from.
     */
    public ClientInfo(Channel channel) throws IOException
    {
        this.channel = channel;
        this.remoteAddress = ((SocketChannel) channel).getRemoteAddress();
        pendingWriteList = new LinkedList<byte[]>();
    }

    /**
     * Method to return the entire pending write list to be written to the client.
     *
     * @return <code>Deque<byte[]></code> containing all of the pending writes to the client.
     */
    public synchronized byte[] getPendingWrite()
    {
        return pendingWriteList.poll();
    }

    /**
     * Method to add a byte array to the pending write list for this client.
     *
     * @param pendingWrite <code>byte[]</code> of data to be added to this clients pending write list.
     */
    public synchronized void addPendingWrite(byte[] pendingWrite)
    {
        pendingWriteList.add(pendingWrite);
    }

    /**
     * Method to determine if there is more in this clients pending write list
     *
     * @return <code>boolean</code> denoting whether there is more available to be written.
     */
    public synchronized boolean hasWrites()
    {
        return pendingWriteList.size() > 0;
    }

    /**
     * Method to disconnect from the current client
     *
     * @throws IOException
     */
    public void disconnect() throws IOException
    {
        channel.close();
    }

    /**
     * Accessor for the remote client address
     * @return  <code>String</code> of the current clients remote address.
     */
    public String getRemoteAddress()
    {
        return remoteAddress.toString();
    }
}
