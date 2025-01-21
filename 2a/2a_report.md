## Assignment 2a Report

### 1. Results from the multithreaded counter program
```declarative
Running with 1 threads...
Final counter value: 10
Time taken: 0 ms

Running with 100 threads...
Final counter value: 1000
Time taken: 4 ms

Running with 1000 threads...
Final counter value: 10000
Time taken: 27 ms

Running with 10000 threads...
Final counter value: 100000
Time taken: 278 ms
```

### 2. Results from the collection 1 program
```Time taken to add elements to Vector: 3 ms
Time taken to add elements to Vector: 3 ms
Time taken to add 100k elements to ArrayList: 1 ms
```

### 3. Results from the collection 2 program
```declarative
Time taken to add elements to HashTable (Single-threaded): 7 ms
Time taken to add elements to HashMap (Single-threaded): 4 ms
Time taken to add elements to ConcurrentHashMap (Single-threaded): 22 ms

Multithreaded test with 100 threads:
Time taken to add elements to HashTable (Multithreaded): 10 ms
Time taken to add elements to Synchronized HashMap (Multithreaded): 14 ms
Time taken to add elements to ConcurrentHashMap (Multithreaded): 15 ms
```

### 4. Results from the multithreaded file writer program
This program implements three approaches to writing to a file using multiple threads:
1. direct write in each thread
2. bath write after thread completion
3. main thread write

The program runs with 1000, 2000, 5000, and 10,000 threads. The results are as follows:

* 1000 threads generated
```
Approach 1 (Direct Write): Time taken = 157 ms
Approach 2 (Batch Write): Time taken = 122 ms
Approach 3 (Main Thread Write): Time taken = 196 ms
```

* 2000 threads generated
```
Approach 1 (Direct Write): Time taken = 291 ms
Approach 2 (Batch Write): Time taken = 155 ms
Approach 3 (Main Thread Write): Time taken = 348 ms
```

* 5000 threads generated
```
Approach 1 (Direct Write): Time taken = 561 ms
Approach 2 (Batch Write): Time taken = 306 ms
Approach 3 (Main Thread Write): Time taken = 828 ms
```

* 10,000 threads generated
```
Approach 1 (Direct Write): Time taken = 1165 ms
Approach 2 (Batch Write): Time taken = 468 ms
Approach 3 (Main Thread Write): Time taken = 1395 ms
```

*Main thread*: 76.88% increase in time taken from 1000 to 2000 threads, 136.93% increase from 2000 to 5000 threads, and 73.50% increase from 5000 to 10000 threads. 

*Direct write*: 63.74% increase in time taken from 1000 to 2000 threads, 106.07% increase from 2000 to 5000 threads, and 80.59% increase from 5000 to 10000 threads.

*Batch write*: 53.77% increase in time taken from 1000 to 2000 threads, 66.26% increase from 2000 to 5000 threads, and 1.55% increase from 5000 to 10000 threads.

Insights and takeaways:
1. Performance aspect:
  * Main thread write took the longest time to write to the file. It may be that this approach accumulates data in memory and writes all at once, resulting in a significant overhead.
  * Direct write was faster than main thread write, may due to the frequent input output operations
  * Batch write was the fastest, as it writes data in batches after all threads have completed their tasks. This approach reduces the overhead of frequent I/O operations, and synchronization overhead.
2. Scalability:
  * Main thread suffers the most as the number of threads increases due to the large amount of data stored in memory and synchronization overhead during collection.
  * Direct write also suffers as the number of threads increases, but not as much as main thread write.
  * Batch write is the most scalable approach, as it writes data in batches after all threads have completed their tasks. 

Alternative implementations:
1. Only one thread writes to the file while the threads are generating the strings.
2. Write data in a ascending timestamp order

Results from the alternative implementation:
```
Producer Threads: Time taken = 1169 ms
Consumer Thread: Time taken = 2591 ms
Total Time (Producer + Consumer): Time taken = 2591 ms
```
Pros and Cons of the alternative implementation:
* Pros:
  * The PriorityBlockingQueue ensures ascending timestamp order.
  * The generation and writing processes are independent.
  * Only one writer ensures thread safety.
* Cons:
  * One single writer may become a bottleneck when the number of threads increases.
  * The use of PriorityBlockingQueue requires additional memory and adds complexity, which may introduce overhead.