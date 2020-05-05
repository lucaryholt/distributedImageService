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
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Label;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;

import javax.imageio.ImageIO;

//Luca
public class Distributor {

    private Queue<Job> googleQueue = new LinkedList<>();
    private Queue<Job> amazonQueue = new LinkedList<>();

    private ResponseHandler responseHandler;

    public Distributor(ResponseHandler responseHandler) {
        this.responseHandler = responseHandler;

        //Starts the two threads that handles the two queues
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
                                System.out.println("added job to google queue from " + id + "...");
                                break;
            case "amazon"   :   amazonQueue.add(job);
                                System.out.println("added job to amazon queue from " + id + "...");
                                break;
        }
    }

}

class GoogleQueueHandler implements Runnable{

    private Queue<Job> queue;
    private ResponseHandler responseHandler;
    private ImageAnnotatorClient vision;

    //TODO Can't authorize at the moment. Have tried setting environment variables on Windows and Mac
    //Google just can't read them for some reason... But logic should work.
    public GoogleQueueHandler(Queue<Job> queue, ResponseHandler responseHandler) {
        this.queue = queue;
        this.responseHandler = responseHandler;
        try {
            vision = ImageAnnotatorClient.create();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processJob(){
        if(queue.peek() != null){
            Job job = queue.remove();

            System.out.println("sending image to google from " + job.getId() + "...");

            //Build image annotation request
            List<AnnotateImageRequest> requests = new ArrayList<>();
            Feature feat = Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).build();
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(convertImage(job.getImage())).build();
            requests.add(request);

            System.out.println("received data from google to " + job.getId() + "...");

            //Get response
            BatchAnnotateImagesResponse response = vision.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            for(AnnotateImageResponse res : responses){
                for(EntityAnnotation annotation : res.getLabelAnnotationsList()){
                    System.out.println(annotation.getDescription() + " ... " + annotation.getScore());
                }
            }
        }
    }

    //Converts BufferedImage to Google Vision Image
    private com.google.cloud.vision.v1.Image convertImage(BufferedImage image){
        try {
            //Convert bufferedImage to ByteString
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            baos.flush();
            byte[] byteArray = baos.toByteArray();
            baos.close();
            ByteString imgBytes = ByteString.copyFrom(byteArray);

            //Convert byte array to Google Vision Image
            return com.google.cloud.vision.v1.Image.newBuilder().setContent(imgBytes).build();
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

class AmazonQueueHandler implements Runnable{

    private Queue<Job> queue;
    private ResponseHandler responseHandler;

    public AmazonQueueHandler(Queue<Job> queue, ResponseHandler responseHandler) {
        this.queue = queue;
        this.responseHandler = responseHandler;
    }

    private void processJob(){
        //System.out.println("checking.");
        if(queue.size() != 0){
            //See source: https://docs.aws.amazon.com/rekognition/latest/dg/images-bytes.html

            Job job = queue.remove();

            System.out.println("sending image to amazon from " + job.getId() + "...");

            try{
                AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.defaultClient();

                //Send and get response from Amazon
                DetectLabelsResult result = rekognitionClient.detectLabels(makeRequestFromJob(job));

                System.out.println("received data from amazon to " + job.getId() + "...");

                //Instantiate new Result object, then pass it to ResponseHandler
                responseHandler.sendResult(makeResultFromRequest(result, job));
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
            try {
                Thread.sleep(50); //Without this it does not work... I have no idea why...
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
