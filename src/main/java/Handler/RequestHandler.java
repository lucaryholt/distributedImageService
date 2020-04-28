package Handler;

import Service.Distributor;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

//Luca
public class RequestHandler {

    public RequestHandler(int port, SessionManager sessionManager, Distributor distributor) {
        Thread thread = new Thread(new ReceiveClient(port, sessionManager, distributor));
        thread.start();
    }

}


class ReceiveClient implements Runnable{

    private ServerSocket serverSocket;
    private SessionManager sessionManager;
    private JSONParser jsonParser;
    private Distributor distributor;
    private int id = 0;

    public ReceiveClient(int port, SessionManager sessionManager, Distributor distributor){
        this.sessionManager = sessionManager;
        this.distributor = distributor;
        this.jsonParser = new JSONParser();
        try{
            serverSocket = new ServerSocket(port);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private void receive(){
        try {
            Socket socket = serverSocket.accept();
            int id = getId();
            receiveRequest(socket, id);
            sessionManager.addClient(socket, id);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveRequest(Socket socket, int id){
        try {
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                JSONObject jsonObject = (JSONObject) jsonParser.parse(bufferedReader.readLine());

                String service = (String) jsonObject.get("service");
                InputStream in = new ByteArrayInputStream((byte[]) jsonObject.get("image"));
                BufferedImage imageFromBytes = ImageIO.read(in);

                distributor.addJob(id, service, imageFromBytes);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getId(){
        id++;
        return id;
    }

    @Override
    public void run() {
        while(true){
            receive();
        }
    }
}