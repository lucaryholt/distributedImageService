package Service;

import Handler.ResponseHandler;
import Model.Job;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

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

    private Random random = new Random();

    private ResponseHandler responseHandler;

    public Distributor(ResponseHandler responseHandler) {
        this.responseHandler = responseHandler;

        //Starts the two threads that handles the two queues
        Thread googleThread = new Thread(new GoogleQueueHandler(googleQueue, responseHandler, this));
        Thread amazonThread = new Thread(new AmazonQueueHandler(amazonQueue, responseHandler, this));
        googleThread.start();
        amazonThread.start();
    }

    //Tager imod jobs fra RequestHandleren og deler dem ud til de rigtige køer
    public synchronized void addJob(int id, BufferedImage image){
        Job job = new Job(id, image);
        if(random.nextInt(10) < 3){
            googleQueue.add(job);
            System.out.println("added job to google queue from " + id + "...");
        }else{
            amazonQueue.add(job);
            System.out.println("added job to amazon queue from " + id + "...");
        }
        synchronized (this){
            notifyAll();
        }
    }

}

class GoogleQueueHandler implements Runnable{

    private Queue<Job> queue;
    private ResponseHandler responseHandler;
    private Distributor distributor;
    private ImageAnnotatorClient vision;

    //TODO Can't authorize at the moment. Have tried setting environment variables on Windows and Mac
    //Google just can't read them for some reason... But logic should work.
    public GoogleQueueHandler(Queue<Job> queue, ResponseHandler responseHandler, Distributor distributor) {
        this.queue = queue;
        this.responseHandler = responseHandler;
        this.distributor = distributor;
        try {
            vision = ImageAnnotatorClient.create();
        } catch (IOException e) {
            System.out.println("Google authentication failed...");
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

            List<String> results = new ArrayList<>();
            results.add("Results from Google Vision:");

            //Get response
            if (vision != null) {
                BatchAnnotateImagesResponse response = vision.batchAnnotateImages(requests);
                List<AnnotateImageResponse> responses = response.getResponsesList();

                for(AnnotateImageResponse res : responses){
                    for(EntityAnnotation annotation : res.getLabelAnnotationsList()){
                        System.out.println(annotation.getDescription() + " ... " + annotation.getScore());
                    }
                }
            }else{
                //As this is not working ATM we just send a "fake" result back
                results.add("This is an image");
            }

            //The result is handed to the ResponseHandler
            responseHandler.sendResult(new Result(results, job.getId()));
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
            try{
                System.out.println("Google ready");
                synchronized (distributor){
                    distributor.wait();
                }
                System.out.println("Google working");
                if(!queue.isEmpty()){
                    processJob();
                }
            } catch (Exception e){
                System.out.println("Google queue: " + e.toString());
                e.printStackTrace();
            }
        }
    }
}

class AmazonQueueHandler implements Runnable{

    private Queue<Job> queue;
    private ResponseHandler responseHandler;
    private Distributor distributor;

    public AmazonQueueHandler(Queue<Job> queue, ResponseHandler responseHandler, Distributor distributor) {
        this.queue = queue;
        this.responseHandler = responseHandler;
        this.distributor = distributor;
    }

    private void processJob(){
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
        results.add("Results from Amazon Rekognition:");
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
            try{
                System.out.println("Amazon ready");
                synchronized (distributor){
                    distributor.wait();
                }
                System.out.println("Amazon working");
                if(!queue.isEmpty()){
                    processJob();
                }
            } catch (Exception e){
                System.out.println("Amazon queue: " + e.toString());
            }
        }
    }
}
