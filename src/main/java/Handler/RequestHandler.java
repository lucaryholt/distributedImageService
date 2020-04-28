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
import java.net.SocketException;
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
    private int id = 0;

    public ReceiveClient(int port, SessionManager sessionManager, Distributor distributor){
        this.sessionManager = sessionManager;
        this.distributor = distributor;
        try{
            serverSocket = new ServerSocket(port);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private void receive(){
        try {
            //Serveren tager imod en klient
            Socket socket = serverSocket.accept();
            //Får ID til klienten
            int id = getId();
            //Starter ny client thread med socket og fundne ID
            newClientThread(socket, id);
            //Giver sessionmanageren socket og id
            sessionManager.addClient(socket, id);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void newClientThread(Socket socket, int id){
        Thread thread = new Thread(new Client(socket, distributor, id));
        thread.start();
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

class Client implements Runnable{

    private BufferedReader bufferedReader;
    private Distributor distributor;
    private JSONParser jsonParser;
    private int id;
    private boolean run = true;

    public Client(Socket socket, Distributor distributor, int id) {
        this.distributor = distributor;
        this.id = id;
        this.jsonParser = new JSONParser();
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("client " + id + " connected...");
    }

    private void receiveRequest(){
        try {
            try {
                //Læs JSONObject fra klienten
                String received = bufferedReader.readLine();
                JSONObject jsonObject = (JSONObject) jsonParser.parse(received);

                //Hiver hvilken service der skal bruges (String) og billede (BufferedImage) ud
                String base64bytes = (String) jsonObject.get("image");
                byte[] bytes = Base64.getDecoder().decode(base64bytes);

                String service = (String) jsonObject.get("service");
                InputStream in = new ByteArrayInputStream(bytes);
                BufferedImage imageFromBytes = ImageIO.read(in);

                System.out.println("received image from " + id + "...");

                //Så bliver det sendt over til distributoren
                distributor.addJob(id, service, imageFromBytes);
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (SocketException e){
                System.out.println(id + " disconnected...");
                run = false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while(run){
            receiveRequest();
        }
    }
}