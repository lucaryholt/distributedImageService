package Service;

import Handler.ResponseHandler;
import Model.Job;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import Model.Result;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.AmazonClientException;
import com.amazonaws.services.rekognition.model.AmazonRekognitionException;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.util.IOUtils;

import javax.imageio.ImageIO;

//Luca
public class Distributor {

    private Queue<Job> googleQueue = new LinkedList<>();
    private Queue<Job> amazonQueue = new LinkedList<>();

    private ResponseHandler responseHandler;

    public Distributor(ResponseHandler responseHandler) {
        this.responseHandler = responseHandler;

        //Starter de to threads til håndtering af de to køer
        Thread googleThread = new Thread(new GoogleQueueHandler(googleQueue, responseHandler));
        Thread amazonThread = new Thread(new AmazonQueueHandler(amazonQueue, responseHandler));
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
    private ResponseHandler responseHandler;

    public GoogleQueueHandler(Queue<Job> queue, ResponseHandler responseHandler) {
        this.queue = queue;
        this.responseHandler = responseHandler;
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
    private ResponseHandler responseHandler;

    public AmazonQueueHandler(Queue<Job> queue, ResponseHandler responseHandler) {
        this.queue = queue;
        this.responseHandler = responseHandler;
    }

    private void processJob(){
        if(queue.peek() != null){
            //See source: https://docs.aws.amazon.com/rekognition/latest/dg/images-bytes.html

            Job job = queue.remove();

            try{
                AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.defaultClient();

                //Send and get response from Amazon
                DetectLabelsResult result = rekognitionClient.detectLabels(makeRequestFromJob(job));

                //Instantiate new Result object, then pass it to ResponseHandler
                responseHandler.setResult(makeResultFromRequest(result, job));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private DetectLabelsRequest makeRequestFromJob(Job job){
        return new DetectLabelsRequest()
                .withImage(convertImage(job.getImage()))
                .withMaxLabels(10)
                .withMinConfidence(77F);
    }

    private Result makeResultFromRequest(DetectLabelsResult detectLabelsResult, Job job){
        List<Label> labels = detectLabelsResult.getLabels();

        //We instantiate and fill in results from Amazon Labels
        List<String> results = new ArrayList<>();
        for (Label label: labels) {
            results.add(label.getName() + ": " + label.getConfidence().toString());
        }

        return new Result(results, job.getId());
    }

    //Converts BufferedImage to Amazon Image Object
    private Image convertImage(BufferedImage image){
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            ByteBuffer byteBuffer = ByteBuffer.wrap(baos.toByteArray());
            return new Image().withBytes(byteBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void run() {
        while(true){
            processJob();
        }
    }
}
