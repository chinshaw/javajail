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
import java.net.URI;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

class ForkClient extends UnicastRemoteObject implements IForkClient,
		IForkService {

	/**
	 * Required for UnicastRemoteOjbect
	 */
	private static final long serialVersionUID = -591616561609370607L;

	/**
	 * Logger instance for this class alone
	 */
	private static final Logger logger = Logger.getLogger(ForkClient.class
			.getName());

	/**
	 * The started process connection for the server
	 */
	private transient Process process;

	/**
	 * The IForkService instance rmi connection to the server.
	 */
	private transient IForkService forkedServer;

	/**
	 * This is out bootstrap builder.
	 */
	private static transient BootstrapBuilder bootstrap;

	/**
	 * The environment to use.
	 */
	private transient Map<String, String> environment;

	/**
	 * Boolean for when the server is started, this may go away and use a thread
	 * waitfor.
	 */
	private volatile boolean started = false;

	/**
	 * This is the generated generated id for the client server interaction.
	 */
	private final String rmiId = UUID.randomUUID().toString();

	/**
	 * Default constructor and will expect that java is in the default path for
	 * your os. This will also use the current system environment from the
	 * current process.
	 * 
	 * @throws RemoteException
	 * @throws ForkException
	 * @throws BootstrapException
	 */
	protected ForkClient() throws RemoteException, ForkException,
			BootstrapException {
		this("java", System.getenv(), DEFAULT_MEMORY_MAX);
	}

	/**
	 * @see ForkManager#getFork()
	 * @param java
	 * @throws RemoteException
	 * @throws ForkException
	 * @throws BootstrapException
	 */
	protected ForkClient(String java) throws RemoteException, ForkException,
			BootstrapException {
		this(java, System.getenv(), DEFAULT_MEMORY_MAX);

	}

	public ForkClient(String java, Map<String, String> environment,
			int maxMemory) throws RemoteException, ForkException,
			BootstrapException {
		this(new BootstrapBuilder().setJavaExecutable(java)
				.setMaxHeapSize(maxMemory)
				.setExtraClassPath(System.getProperty("java.class.path")),
				environment);
	}

	public ForkClient(String java, Map<String, String> environment,
			int maxMemory, String extraClassPath) throws RemoteException,
			ForkException, BootstrapException {
		this(new BootstrapBuilder().setJavaExecutable(java)
				.setMaxHeapSize(maxMemory).setExtraClassPath(extraClassPath),
				environment);
	}

	public ForkClient(BootstrapBuilder bootstrap) throws RemoteException,
			ForkException, BootstrapException {
		this(bootstrap, System.getenv());
	}

	public ForkClient(BootstrapBuilder bootstrap,
			Map<String, String> environment) throws RemoteException,
			ForkException, BootstrapException {
		ForkClient.bootstrap = bootstrap;
		this.environment = environment;

		try {
			// Must bind to rmi with our id before we start the server.
			ForkManager.getInstance().getRmiRegistry()
					.bind(getRmiIdentifier(), this);
			startServer();
		} catch (IOException e) {
			throw new ForkException(e);
		} catch (AlreadyBoundException e) {
			throw new ForkException(e);
		}
	}

	private void startServer() throws ForkException, BootstrapException {

		ProcessBuilder builder = new ProcessBuilder();

		List<String> command = bootstrap.addExtraArgument(rmiId).build();

		if (environment != null) {
			builder.environment().putAll(environment);
		}

		logger.info("starting server with command -> " + command);
		builder.command(command);

		logger.info("Starting process");
		try {
			this.process = builder.start();

			// Process output and error streams
			new StreamPiper(process.getErrorStream(), "Server: ").start();
			new StreamPiper(process.getInputStream(), "Server: ").start();

			waitForStartBeacon();
		} catch (IOException e) {
			throw new ForkException("Unable to fork java process ", e);
		}
	}

	/**
	 * One time method that is used to wait until the server is alive. The while
	 * loop may be unncessary. TODO come up with a better way to block the
	 * current thread while waiting for the onServerStarted event.
	 * 
	 * @throws IOException
	 */
	private void waitForStartBeacon() {
		logger.fine("Waiting for server to finish starting can call serverinitialized");
		while (true) {
			if (started == true) {
				break;
			}
		}
	}

	public String getRmiIdentifier() {
		return rmiId + IForkClient.TYPE_IDENTIFIER;
	}

	/**
	 * @see IForkClient#onServerStarted(String)
	 */
	public void onServerStarted(String rmiServerId) throws AccessException,
			RemoteException, NotBoundException {
		forkedServer = (IForkService) ForkManager.getInstance()
				.getRmiRegistry().lookup(rmiServerId);
		started = true;
	}

	public synchronized boolean ping() {
		return true;
	}

	public <T extends Serializable> T execute(Class<T> returnType,
			IForkedJob job) throws RemoteException {
		return forkedServer.execute(returnType, job);
	}

	public <T extends Serializable> T execute(RemoteOperation<T> callable)
			throws RemoteException, ForkException, ForkTimeoutException {
		return execute(callable, 600);
	}

	public <T extends Serializable> T execute(
			RemoteOperation<T> remoteOperation, int timeout)
			throws RemoteException, ForkException, ForkTimeoutException {
		// RemoteOperation<T> stub = (RemoteOperation<T>)
		// UnicastRemoteObject.exportObject(remoteOperation);

		return forkedServer.execute(remoteOperation, timeout);
	}

	/**
	 * This will affect the server's runtime classpath, the command goes to the
	 * remote process and modifies that classpath. If the classpath is t
	 */
	public void addClassPath(URI path) throws RemoteException, IOException {
		forkedServer.addClassPath(path.toString());
	}

	public void close() throws RemoteException {
		try {
			ForkManager.getInstance().getRmiRegistry()
					.unbind(getRmiIdentifier());
		} catch (NotBoundException e) {
		}
		forkedServer.close();
	}

	public long totalMemory() throws RemoteException {
		return forkedServer.totalMemory();
	}

	public long maxMemory() throws RemoteException {
		return forkedServer.maxMemory();
	}

	public long freeMemory() throws RemoteException {
		return forkedServer.freeMemory();
	}

	public void addClassPath(String classPath) throws IOException,
			RemoteException {
		// TODO Auto-generated method stub
	}

	public boolean alive() throws RemoteException {
		return forkedServer.alive();
	}

}