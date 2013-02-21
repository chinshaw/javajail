package org.braindroppings.forkingjava;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.Callable;

public interface IForkService extends Remote {

    public static int DEFAULT_MEMORY_MAX = 128;
    
    
    public abstract void addClassPath(String classPath) throws IOException, RemoteException;

    public abstract <T extends Serializable> T execute(Class<T> returnType, IForkedJob job) throws RemoteException;

    public <T extends Serializable> T execute(RemoteOperation<T> callable) throws RemoteException, ForkException, ForkTimeoutException;

    public abstract <T extends Serializable> T execute(RemoteOperation<T> callable, int timeout) throws RemoteException, ForkException, ForkTimeoutException;

    public long totalMemory() throws RemoteException;

    public long maxMemory() throws RemoteException;

    public long freeMemory() throws RemoteException;

    public abstract boolean alive() throws RemoteException;
    
    public abstract void close() throws RemoteException;
}
