package Handler;

import Model.Result;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class ResponseHandler {

    private SessionManager sessionManager;

    public ResponseHandler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void sendResult(Result result) {
        int clientId = result.getId();
        try {
            // Get socket from session manager
            Socket socket = sessionManager.getClient(clientId);

            // Instantiate PrintWriter to send response to client
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // Instantiate JSONObject
            JSONObject jsonObject = new JSONObject();
            // Populate JSONObject
            jsonObject.put("result", result.getResult());
            jsonObject.put("id", clientId);

            System.out.println(jsonObject);

            // Send JSONObject to client
            out.println(jsonObject.toJSONString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
