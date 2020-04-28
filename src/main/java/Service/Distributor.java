package Service;

import Model.Job;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.AmazonClientException;
import com.amazonaws.services.rekognition.model.AmazonRekognitionException;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.util.IOUtils;

public class Distributor {

    private Queue<Job> googleQueue = new LinkedList<>();
    private Queue<Job> amazonQueue = new LinkedList<>();

    public Distributor() {
        Thread googleThread = new Thread(new GoogleQueueHandler(googleQueue));
        Thread amazonThread = new Thread(new AmazonQueueHandler(amazonQueue));
        googleThread.start();
        amazonThread.start();
    }

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
            //See source: https://docs.aws.amazon.com/rekognition/latest/dg/images-bytes.html

            String photo = "input.jpg";

            ByteBuffer imageBytes;
            try (InputStream inputStream = new FileInputStream(new File(photo))) {
                imageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));


            AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.defaultClient();

            DetectLabelsRequest request = new DetectLabelsRequest()
                    .withImage(new Image()
                            .withBytes(imageBytes))
                    .withMaxLabels(10)
                    .withMinConfidence(77F);

            DetectLabelsResult result = rekognitionClient.detectLabels(request);
            List<Label> labels = result.getLabels();

            System.out.println("Detected labels for " + photo);
            for (Label label: labels) {
                System.out.println(label.getName() + ": " + label.getConfidence().toString());
            }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        while(true){
            processJob();
        }
    }
}
