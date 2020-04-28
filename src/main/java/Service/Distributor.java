package Service;

import Model.Job;

import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.Queue;

//Luca
public class Distributor {

    private Queue<Job> googleQueue = new LinkedList<>();
    private Queue<Job> amazonQueue = new LinkedList<>();

    public Distributor() {
        //Starter de to threads til håndtering af de to køer
        Thread googleThread = new Thread(new GoogleQueueHandler(googleQueue));
        Thread amazonThread = new Thread(new AmazonQueueHandler(amazonQueue));
        googleThread.start();
        amazonThread.start();
    }

    //Tager imod jobs fra RequestHandleren og deler dem ud til de rigtige køer
    public synchronized void addJob(int id, String service, BufferedImage image){
        Job job = new Job(id, image);
        switch (service){
            case "google"   :   googleQueue.add(job);
                                break;
            case "amazon"   :   amazonQueue.add(job);
                                break;
        }
    }

}

class GoogleQueueHandler implements Runnable{

    private Queue<Job> queue;

    public GoogleQueueHandler(Queue<Job> queue) {
        this.queue = queue;
    }

    //TODO Den her skal hive forreste Job og håndtere det og til sidst sende det til responseHandleren som result
    private void processJob(){
        if(queue.peek() != null){
            //Do something with job
        }
    }

    @Override
    public void run() {
        while(true){
            processJob();
        }
    }
}

class AmazonQueueHandler implements Runnable{

    private Queue<Job> queue;

    public AmazonQueueHandler(Queue<Job> queue) {
        this.queue = queue;
    }

    private void processJob(){
        if(queue.peek() != null){
            //Do something with job

        }
    }

    @Override
    public void run() {
        while(true){
            processJob();
        }
    }
}
