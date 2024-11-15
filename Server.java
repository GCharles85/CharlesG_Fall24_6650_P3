import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Server {

    public static void main(String[] args) {
        try {
            // Server initialization
            if (args.length < 2) {
                System.err.println("Usage: java Server <serverId> <nextServer>");
                return;
            }

            String serverId = args[0];
            String nextServer = args[1];

            // Create the HandleRequests object with serverId and nextServer for token ring
            HandleRequests obj = new HandleRequests(serverId, nextServer);

            // Export the object to the RMI runtime
            HandleRequestsInterface stub = (HandleRequestsInterface) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.bind("HandleRequests", stub);

            System.out.println("Server " + serverId + " ready and bound to registry.");

            // Token ring simulation: pass token initially if serverId is 1
            if ("1".equals(serverId)) {
                obj.receiveToken();
                Thread.sleep(5000); // Simulate some critical section work
                obj.passToken();
            }

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
