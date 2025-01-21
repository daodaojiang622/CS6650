import java.util.*;
import java.util.concurrent.*;

public class Collection2 {

    public static void main(String[] args) throws InterruptedException {
        int numberOfElements = 100000; // Number of elements to add
        int numberOfThreads = 100; // Number of threads for multithreaded test

        // Test with HashTable (Single-threaded)
        Hashtable<Integer, Integer> hashTable = new Hashtable<>();
        long hashTableStartTime = System.currentTimeMillis();
        for (int i = 0; i < numberOfElements; i++) {
            hashTable.put(i, i);
        }
        long hashTableEndTime = System.currentTimeMillis();
        System.out.println("Time taken to add elements to HashTable (Single-threaded): " + (hashTableEndTime - hashTableStartTime) + " ms");

        // Test with HashMap (Single-threaded)
        HashMap<Integer, Integer> hashMap = new HashMap<>();
        long hashMapStartTime = System.currentTimeMillis();
        for (int i = 0; i < numberOfElements; i++) {
            hashMap.put(i, i);
        }
        long hashMapEndTime = System.currentTimeMillis();
        System.out.println("Time taken to add elements to HashMap (Single-threaded): " + (hashMapEndTime - hashMapStartTime) + " ms");

        // Test with ConcurrentHashMap (Single-threaded)
        ConcurrentHashMap<Integer, Integer> concurrentHashMap = new ConcurrentHashMap<>();
        long concurrentHashMapStartTime = System.currentTimeMillis();
        for (int i = 0; i < numberOfElements; i++) {
            concurrentHashMap.put(i, i);
        }
        long concurrentHashMapEndTime = System.currentTimeMillis();
        System.out.println("Time taken to add elements to ConcurrentHashMap (Single-threaded): " + (concurrentHashMapEndTime - concurrentHashMapStartTime) + " ms");

        // Multithreaded test
        System.out.println("\nMultithreaded test with " + numberOfThreads + " threads:");

        // HashTable (Multithreaded)
        hashTable.clear();
        long hashTableMultiStartTime = System.currentTimeMillis();
        runMultithreadedTest(hashTable, numberOfElements, numberOfThreads);
        long hashTableMultiEndTime = System.currentTimeMillis();
        System.out.println("Time taken to add elements to HashTable (Multithreaded): " + (hashTableMultiEndTime - hashTableMultiStartTime) + " ms");

        // Synchronized HashMap (Multithreaded)
        Map<Integer, Integer> synchronizedHashMap = Collections.synchronizedMap(new HashMap<>());
        long synchronizedHashMapMultiStartTime = System.currentTimeMillis();
        runMultithreadedTest(synchronizedHashMap, numberOfElements, numberOfThreads);
        long synchronizedHashMapMultiEndTime = System.currentTimeMillis();
        System.out.println("Time taken to add elements to Synchronized HashMap (Multithreaded): " + (synchronizedHashMapMultiEndTime - synchronizedHashMapMultiStartTime) + " ms");

        // ConcurrentHashMap (Multithreaded)
        concurrentHashMap.clear();
        long concurrentHashMapMultiStartTime = System.currentTimeMillis();
        runMultithreadedTest(concurrentHashMap, numberOfElements, numberOfThreads);
        long concurrentHashMapMultiEndTime = System.currentTimeMillis();
        System.out.println("Time taken to add elements to ConcurrentHashMap (Multithreaded): " + (concurrentHashMapMultiEndTime - concurrentHashMapMultiStartTime) + " ms");
    }

    private static void runMultithreadedTest(Map<Integer, Integer> map, int numberOfElements, int numberOfThreads) throws InterruptedException {
        Thread[] threads = new Thread[numberOfThreads];
        int elementsPerThread = numberOfElements / numberOfThreads;

        for (int i = 0; i < numberOfThreads; i++) {
            final int start = i * elementsPerThread;
            final int end = (i + 1) * elementsPerThread;

            threads[i] = new Thread(() -> {
                for (int j = start; j < end; j++) {
                    map.put(j, j);
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }
}
