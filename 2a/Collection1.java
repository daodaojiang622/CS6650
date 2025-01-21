import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class Collection1 {

    public static void main(String[] args) {
        int numberOfElements = 100000; // Number of elements to add

        // Test with Vector
        Vector<Integer> vector = new Vector<>();
        long vectorStartTime = System.currentTimeMillis();
        for (int i = 0; i < numberOfElements; i++) {
            vector.add(i);
        }
        long vectorEndTime = System.currentTimeMillis();
        System.out.println("Time taken to add elements to Vector: " + (vectorEndTime - vectorStartTime) + " ms");

        // Test with ArrayList
        List<Integer> arrayList = new ArrayList<>();
        long arrayListStartTime = System.currentTimeMillis();
        for (int i = 0; i < numberOfElements; i++) {
            arrayList.add(i);
        }
        long arrayListEndTime = System.currentTimeMillis();
        System.out.println("Time taken to add 100k elements to ArrayList: " + (arrayListEndTime - arrayListStartTime) + " ms");
    }
}
