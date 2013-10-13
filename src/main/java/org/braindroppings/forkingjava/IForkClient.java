package org.braindroppings.forkingjava;

import java.io.Serializable;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IForkClient extends Remote, Serializable {

	/**
	 * Client Identifier used to generate a unique client rmi id.
	 */
	public static final String TYPE_IDENTIFIER = "_client";

	/**
	 * This method is used to alert the client that the server has started, once
	 * the server has started it's ping thread it will call this method on the
	 * client to initiate communication.
	 * 
	 * @param rmiServerId
	 * @throws AccessException
	 * @throws RemoteException
	 * @throws NotBoundException
	 */
	public void onServerStarted(String rmiServerId) throws AccessException,
			RemoteException, NotBoundException;

	/**
	 * Method that the server can use to check to make sure it's peer client is
	 * still alive. If not then the spawned server process should take the
	 * appropriate measures to close.
	 * 
	 * @return
	 * @throws RemoteException
	 */
	public boolean ping() throws RemoteException;

}
