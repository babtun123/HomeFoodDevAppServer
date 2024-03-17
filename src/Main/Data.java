package Main;
import java.io.Serializable;
import java.util.ArrayList;

public class Data implements Serializable {
    public String message;
    public ArrayList<Boolean> connectedClients;
    public ArrayList<String> clientNames = new ArrayList<>();
    public boolean sendToAll = true;
    public ArrayList<String> sendTo = new ArrayList<>();
    public String screenName;
    public int clientNumber;

    public Data(String m, ArrayList<Boolean> conn) {

        this.message = m;
        this.connectedClients = conn;
    }
}

