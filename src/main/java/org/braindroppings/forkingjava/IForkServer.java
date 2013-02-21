package org.braindroppings.forkingjava;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IForkServer extends Remote {

    /**
     * Identifier for the server daemon
     */
    public static final String TYPE_IDENTIFIER = "_SERVER";

    public boolean ping() throws RemoteException;

}
