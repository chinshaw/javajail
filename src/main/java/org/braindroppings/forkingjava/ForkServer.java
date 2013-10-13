/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.braindroppings.forkingjava;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

class ForkServer extends UnicastRemoteObject implements IForkServer,
		IForkService {

	/**
	 * ForkServer logger
	 */
	private static final Logger logger = Logger.getLogger(ForkServer.class
			.getName());

	/**
	 * Number of seconds to wait before contacting client to see if it is still
	 * avlive.
	 */
	private static final int PING_INTERVAL = 10;

	/**
	 * This is needed because we want to return from our
	 * 
	 * @author chinshaw
	 * 
	 */
	class ShutdownThread extends Thread {

		public void run() {
			try {
				Thread.sleep(PING_INTERVAL * 2000);
			} catch (InterruptedException e) {
			}
			exit(0);
		}
	}

	class PingThread extends Thread {

		IForkClient pingClient;

		public PingThread(IForkClient client) {
			this.pingClient = client;
		}

		public void run() {

			while (true) {
				try {
					logger.finest("Doing ping");
					pingClient.ping();
					Thread.sleep(PING_INTERVAL * 1000);
				} catch (RemoteException e) {
					logger.warning("Calling close on forked server");
					exit(0);
				} catch (InterruptedException e) {
					logger.warning("Hmm, caught an interrupted exception");
				}
			}
		}
	}

	/**
	 * Required for UnicastRemoteObject
	 */
	private static final long serialVersionUID = -3476634356209948347L;

	private static String forkId;

	protected ForkServer() throws RemoteException {
		this(UUID.randomUUID().toString());
	}

	protected ForkServer(String forkId) throws RemoteException {
		super();
		ForkServer.forkId = forkId;
	}

	public void start() throws AccessException, RemoteException,
			AlreadyBoundException {
		String serverId = getIdentifier();

		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		Registry registry = LocateRegistry
				.getRegistry(Constants.RMI_CONNECTION_PORT);

		registry.bind(serverId, this);

		IForkClient client = null;
		try {
			client = (IForkClient) LocateRegistry.getRegistry(
					Constants.RMI_CONNECTION_PORT).lookup(
					forkId + IForkClient.TYPE_IDENTIFIER);
			logger.info("Contacting client");
			client.onServerStarted(serverId);

			new PingThread(client).start();

		} catch (NotBoundException e) {
			logger.info("Problem binding to client and we are going to die now");
			exit(-1);
		}
	}

	/**
	 * Returns the server identifier, this is a combination of the client's fork
	 * id and the {@link IForkServer#TYPE_IDENTIFIER}
	 * 
	 * @return Sting representation of the server identifier.
	 */
	public static String getIdentifier() {
		return forkId + TYPE_IDENTIFIER;
	}

	/**
	 * Starts a forked server process, simply calls new ForkServer and starts
	 * it. The argument for an id is required and it should be the last
	 * argument.
	 * 
	 * @param args
	 *            command line arguments, ignored
	 * @throws Exception
	 *             if the server could not be started
	 */
	public static void main(String[] args) throws Exception {
		String generatedId = args[0];

		if (generatedId == null) {
			throw new ForkException(
					"The last argument to the fork server must be a valid uuid identifier");
		}

		try {
			UUID.fromString(generatedId);
		} catch (IllegalArgumentException e) {
			throw new ForkException(
					"Forked server must have a valid uuid to start so that it can bind to client, the id sent was -> "
							+ generatedId, e);
		}
		try {
			new ForkServer(generatedId).start();
		} catch (Error e) {
			System.out.println("GOT AN ERROR " + e);
			throw e;
		}

	}

	/**
	 * If you get here we will simply return true. The server will close on most
	 * errors so if this returns then you can be pretty sure it is ready to go.
	 */
	public boolean ping() {
		return true;
	}

	/**
	 * This needs some work, and I don't know if it is a great idea since we can
	 * use callable
	 */
	public <T extends Serializable> T execute(Class<T> returnType,
			IForkedJob job) throws RemoteException {
		System.out.println("About to start");
		job.run();
		return null;
	}

	/**
	 * This is a stub because it is required by the {@link IForkService}
	 * interface it is required but the client should call it's execute method
	 * with a default timeout, I will not allow you to execute indefinitely, go
	 * make your own server.
	 * 
	 * @see #execute(Callable, int)
	 * @param callable
	 *            Don't even bother we're just throwing an exception
	 */
	public <T extends Serializable> T execute(RemoteOperation<T> callable)
			throws ForkException {
		throw new RuntimeException(
				"You are a mystical wizard with jmp powers!!");
	}

	/**
	 * This will execute a {@link Callable} job in this process vs the server.
	 * It will attempt to execute for as long as the timer is set for. If the
	 * timout occurrs we will try to kill the job but the client will be in
	 * charge of killing this process or calling close.
	 * 
	 * @parm {@link Callable} operation that will execute as a job on the
	 *       server.
	 * @param timeout
	 *            in seconds to wait for the job to complete.
	 * @return T this is a serializable object that can be returned from the
	 *         server.
	 * @throws ForkTimeoutException
	 */
	public <T extends Serializable> T execute(RemoteOperation<T> callable,
			int timeout) throws ForkException, ForkTimeoutException {
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		Future<T> future = executorService.submit(callable);

		T result = null;
		try {
			result = future.get(timeout, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			throw new ForkTimeoutException(
					"Your job timed out, it took longer than " + timeout
							+ " seconds to complete", e);
		} catch (ExecutionException e) {
			throw new ForkException("Unable to execute task ", e.getCause());
		} catch (InterruptedException e) {
			logger.warning("The job was interrupted but what do we do");
		} catch (Error e) {
			System.out.println("ERROR OCCURRED " + e);
			throw new ForkException("Unable to complete execution of task ",
					e.getCause());
		}

		return result;
	}

	/**
	 * Allows us to add a classpath to the class path for this service, this
	 * should be in the form of a url
	 */
	public void addClassPath(String classPath) throws IOException,
			RemoteException {
		ClassPathHacker.addFile(classPath);
	}

	/**
	 * Close the server essentially calls close.
	 * 
	 * @throws NotBoundException
	 * 
	 * @see IForkService#close()
	 */
	public void close() {

		Registry registry;
		try {
			registry = LocateRegistry
					.getRegistry(Constants.RMI_CONNECTION_PORT);
			registry.unbind(getIdentifier());
		} catch (Exception e) {
			// Doesn't matter we're about to die.
		}

		new ShutdownThread().start();
	}

	public long totalMemory() {
		return Runtime.getRuntime().totalMemory();
	}

	public long maxMemory() {
		return Runtime.getRuntime().maxMemory();
	}

	public long freeMemory() {
		return Runtime.getRuntime().freeMemory();
	}

	/**
	 * Close the forked server.
	 * 
	 * @param exitCode
	 *            The code to exit with.
	 * @throws NotBoundException
	 * @throws RemoteException
	 * @throws AccessException
	 */
	private void exit(int exitCode) {
		System.exit(exitCode);
	}

	public boolean alive() throws RemoteException {
		return true;
	}

}