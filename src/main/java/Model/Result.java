package Model;

public class Result {

    private String result;
    private int id;

    public Result() {
    }

    public Result(String result, int id) {
        this.result = result;
        this.id = id;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Result{" +
                "result='" + result + '\'' +
                ", id=" + id +
                '}';
    }
}
