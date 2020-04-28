package Model;

import java.awt.image.BufferedImage;

public class Job {

    private int id;
    private BufferedImage image;

    public Job() {
    }

    public Job(int id, BufferedImage image) {
        this.id = id;
        this.image = image;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }
}
