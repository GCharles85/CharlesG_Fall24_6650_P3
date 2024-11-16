import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Server {

    public static void main(String[] args) {
        try {
            if (args.length < 3) {
                System.err.println("Usage: java Server <serverId:int> <nextServer:string> <hasToken:bool>");
                return;
            }
            String registryHost = System.getenv("CENTRAL_REGISTRY_HOST");
            String serverId = args[0];
            String nextServer = args[1];

            // Create the HandleRequests object with serverId 
            HandleRequests obj = new HandleRequests(serverId, nextServer);

            // Connect to centralized registry on server1
            Registry registry;
            if ("Server1".equals(serverId)) {
                // Create registry on Server1
                registry = LocateRegistry.createRegistry(1099);
            } else {
                // Connect to registry hosted by Server1
                registry = LocateRegistry.getRegistry(registryHost, 1099);
            }

            // Export the object to the RMI runtime
            HandleRequestsInterface stub = (HandleRequestsInterface) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the service with a unique name
            registry.bind("HandleRequests-" + serverId, obj);
         
            // Server runs indefinitely to handle requests
            while (true) {
                // This can include additional logic for monitoring or processing
                Thread.sleep(1000);
            }

        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
