import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class NetworkClient {

    public static void main(String[] args) {
        try {
            // Connect to the RMI registry
            Registry registry = LocateRegistry.getRegistry("server", 1099);

            // Lookup the remote object
            HandleRequestsInterface stub = (HandleRequestsInterface) registry.lookup("HandleRequests");

            // Perform multiple PUT, GET, and DELETE operations
            System.out.println(stub.processRequest("PUT(key1, value1)"));
            System.out.println(stub.processRequest("PUT(key2, value2)"));
            System.out.println(stub.processRequest("PUT(key3, value3)"));
            System.out.println(stub.processRequest("PUT(key4, value4)"));
            System.out.println(stub.processRequest("PUT(key5, value5)"));
            System.out.println(stub.processRequest("GET(key1)"));
            System.out.println(stub.processRequest("GET(key2)"));
            System.out.println(stub.processRequest("GET(key3)"));
            System.out.println(stub.processRequest("GET(key4)"));
            System.out.println(stub.processRequest("GET(key5)"));       
            System.out.println(stub.processRequest("DELETE(key1)"));
            System.out.println(stub.processRequest("DELETE(key2)"));
            System.out.println(stub.processRequest("DELETE(key3)"));
            System.out.println(stub.processRequest("DELETE(key4)"));
            System.out.println(stub.processRequest("DELETE(key5)"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}