package Service;

import Model.Job;

import java.awt.image.BufferedImage;
import java.util.concurrent.BlockingDeque;

public class Distributor {

    private BlockingDeque<Job> googleQueue;
    private BlockingDeque<Job> amazonQueue;

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
