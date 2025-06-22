import java.util.concurrent.Semaphore;

public class Consumer extends Thread {
    private final int stopIndex;
    private final int capacity;
    private final int requestTime;
    private volatile int currentRequests;

    private final Semaphore empty;
    private final Semaphore full;
    private final Semaphore mutex;

    private volatile boolean running;

    public Consumer(int stopIndex, int capacity, int requestTime) {
        this.stopIndex = stopIndex;
        this.capacity = Math.max(1, capacity);
        this.requestTime = Math.max(100, requestTime);
        this.currentRequests = 0;

        this.empty = new Semaphore(this.capacity);
        this.full = new Semaphore(0);
        this.mutex = new Semaphore(1);

        this.running = true;
        this.setName("Consumer-" + stopIndex);
        this.setDaemon(true);
    }

    @Override
    public void run() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(requestTime);

                empty.acquire();
                mutex.acquire();

                try {
                    currentRequests++;
                } finally {
                    mutex.release();
                    full.release();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void takeRequest() throws InterruptedException {
        full.acquire();
        mutex.acquire();

        try {
            if (currentRequests > 0) {
                currentRequests--;
            }
        } finally {
            mutex.release();
            empty.release();
        }
    }

    public void deliverProduct() throws InterruptedException {
        mutex.acquire();
        try {
            // Produto entregue
        } finally {
            mutex.release();
        }
    }

    public boolean hasRequests() {
        try {
            if (mutex.tryAcquire()) {
                try {
                    return currentRequests > 0;
                } finally {
                    mutex.release();
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public void stopConsumer() {
        running = false;
        this.interrupt();
    }

    public int getStopIndex() {
        return stopIndex;
    }

    public int getCapacity() {
        return capacity;
    }

    public synchronized int getCurrentRequests() {
        return currentRequests;
    }

    public int getRequestTime() {
        return requestTime;
    }
}