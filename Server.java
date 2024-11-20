import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Server {

    public static void main(String[] args) {
        try {
            if (args.length < 3) {
                System.err.println("Usage: java Server <serverId> <nextServer> <hasToken>");
                return;
            }
            String registryHost = System.getenv("CENTRAL_REGISTRY_HOST");
            String serverId = "server" + args[0];
            String nextServer = "server" + args[1];

            // Create the HandleRequests object with serverId 
            HandleRequests obj = new HandleRequests(serverId, nextServer);

            // Connect to centralized registry on server1
            Registry registry;
            if ("server1".equals(serverId)) {
                // Set the hostname explicitly for RMI communication
                System.setProperty("java.rmi.server.hostname", "server1");
                // Create registry on Server1
                registry = LocateRegistry.createRegistry(1099);
                registry.bind("HandleRequests-" + serverId, obj);
            } else {
                // Connect to registry hosted by Server1
                registry = LocateRegistry.getRegistry(registryHost, 1099);
            }

            // Export the object to the RMI runtime
            HandleRequestsInterface stub = (HandleRequestsInterface) UnicastRemoteObject.exportObject(obj, 0);
        
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
        }
    }
}
