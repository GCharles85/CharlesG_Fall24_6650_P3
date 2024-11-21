import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.Naming;
import java.util.logging.*;

public class Server {
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
// todo clean up
    public static void main(String[] args) {
        try {
            if (args.length < 3) {
                System.err.println("Usage: java Server <serverId> <nextServer> <hasToken>");
                return;
            }
            LOGGER.setLevel(Level.SEVERE);
            String registryHost = System.getenv("CENTRAL_REGISTRY_HOST");
            String serverId = "server" + args[0];
            String nextServer = "server" + args[1];

            // Create the HandleRequests object with serverId 
            HandleRequests obj = new HandleRequests(serverId, nextServer);

            // Connect to centralized registry on server1
            Registry registry;
            
            
                 System.setProperty("java.rmi.server.hostname", serverId);
                // Create registry on Server1
                registry = LocateRegistry.createRegistry(1099);
                HandleRequestsInterface stub  = (HandleRequestsInterface) UnicastRemoteObject.exportObject(obj, 0);
                registry.bind("HandleRequests-" + serverId, stub);
                
            // } else {
            //     // Connect to registry hosted by Server1
            //     registry = LocateRegistry.getRegistry(registryHost, 1099);
            // }

            // Locate Server2's remote object in the RMI registry
            
            HandleRequestsInterface currentServer = (HandleRequestsInterface) Naming.lookup("rmi://"+serverId+":1099/HandleRequests-" + serverId);
            if ("server1".equals(serverId)) {
                //give server1 the token
                currentServer.setInitToken();
            }


            while(true){
                currentServer.passToken();
                Thread.sleep(1000);
            }
        
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
        }
    }
}
