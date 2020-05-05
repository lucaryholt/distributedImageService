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
import java.util.Base64;

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
    private Distributor distributor;
    private JSONParser jsonParser;
    private int id = 0;

    public ReceiveClient(int port, SessionManager sessionManager, Distributor distributor){
        this.sessionManager = sessionManager;
        this.distributor = distributor;
        jsonParser = new JSONParser();
        try{
            serverSocket = new ServerSocket(port);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private void receive(){
        try {
            //The server accepts a klient
            Socket socket = serverSocket.accept();
            //Får ID til klienten
            int id = getId();
            //Starts a new client thread with socket and found ID
            receiveJob(socket, id);
            //Passes socket and id to sessionmanager
            sessionManager.addClient(socket, id);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveJob(Socket socket, int id){
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            //Read JSONObject from client
            String received = bufferedReader.readLine();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(received);

            //Reads which service og pulls image from JSONObject
            String base64bytes = (String) jsonObject.get("image");
            byte[] bytes = Base64.getDecoder().decode(base64bytes);

            InputStream in = new ByteArrayInputStream(bytes);
            BufferedImage imageFromBytes = ImageIO.read(in);

            System.out.println("received image from " + id + "...");

            //This info is then sent to the distributor
            distributor.addJob(id, imageFromBytes);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
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