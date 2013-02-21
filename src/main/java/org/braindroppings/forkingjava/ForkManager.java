package org.braindroppings.forkingjava;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;

public class ForkManager {

    private static ForkManager instance;

    private static volatile Registry rmiRegistry = null;

    private ForkManager() {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        
        try {
            rmiRegistry = LocateRegistry.createRegistry(Constants.RMI_CONNECTION_PORT);
        } catch (RemoteException e) {
            e.printStackTrace();
            throw new RuntimeException("Terminal unable to create rmi registry ", e);
        }
    }

    public static ForkManager getInstance() {
        if (instance == null) {
            instance = new ForkManager();
        }
        return instance;
    }

    public IForkService getFork() throws IOException, ForkException, BootstrapException {
        return new ForkClient();
    }

    public IForkService getFork(String java) throws RemoteException, ForkException, BootstrapException {
        return new ForkClient(java);
    }

    public IForkService getFork(String java, Map<String, String> environment) throws IOException, ForkException, BootstrapException {
        return new ForkClient(java);
    }

    public IForkService getFork(BootstrapBuilder bootstrap, Map<String, String> environment) throws RemoteException, ForkException, BootstrapException {
        return new ForkClient(bootstrap, environment);
    }

    public Registry getRmiRegistry() {
        return rmiRegistry;
    }
}