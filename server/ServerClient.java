package cs455.scaling.server;

import cs455.scaling.utility.OpsChangeRequest;

/**
 * A ServerClient interface
 *
 * @author ahrtmn, 09 03 2014
 */
public interface ServerClient
{

    /**
     * Method to add an ops change request to the calling selector thread.
     *
     * @param opsChangeRequest <code>OpsChangeRequest</code> with the desired selector change request.
     */
    public void addOpsChangeRequest(OpsChangeRequest opsChangeRequest);
}
