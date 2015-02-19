package cs455.scaling.utility;

import java.nio.channels.SocketChannel;

/**
 * A OpsChangeRequest class
 *
 * @author ahrtmn, 09 03 2014
 */
public class OpsChangeRequest
{
    public static final int CHANGEOPS = 2;

    public SocketChannel socket;
    public int type;
    public int ops;

    public OpsChangeRequest(SocketChannel socket, int type, int ops)
    {
        this.socket = socket;
        this.type = type;
        this.ops = ops;
    }
}
