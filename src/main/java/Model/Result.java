package Model;

import java.util.List;

public class Result {

    private List<String> result;
    private int id;

    public Result() {
    }

    public Result(List<String> result, int id) {
        this.result = result;
        this.id = id;
    }

    public List<String> getResult() {
        return result;
    }

    public void setResult(List<String> result) {
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
