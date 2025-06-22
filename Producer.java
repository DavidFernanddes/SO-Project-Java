import java.util.concurrent.Semaphore;

public class Producer extends Thread {
    private final int stopIndex;
    private final int capacity;
    private final int productionTime;
    private volatile int currentProducts;

    private final Semaphore empty;
    private final Semaphore full;
    private final Semaphore mutex;

    private volatile boolean running;

    public Producer(int stopIndex, int capacity, int productionTime) {
        this.stopIndex = stopIndex;
        this.capacity = Math.max(1, capacity);
        this.productionTime = Math.max(100, productionTime);
        this.currentProducts = 0;

        this.empty = new Semaphore(this.capacity);
        this.full = new Semaphore(0);
        this.mutex = new Semaphore(1);

        this.running = true;
        this.setName("Producer-" + stopIndex);
        this.setDaemon(true);
    }

    @Override
    public void run() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(productionTime);

                empty.acquire();
                mutex.acquire();

                try {
                    currentProducts++;
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

    public void takeProduct() throws InterruptedException {
        full.acquire();
        mutex.acquire();

        try {
            if (currentProducts > 0) {
                currentProducts--;
            }
        } finally {
            mutex.release();
            empty.release();
        }
    }

    public boolean hasProducts() {
        try {
            if (mutex.tryAcquire()) {
                try {
                    return currentProducts > 0;
                } finally {
                    mutex.release();
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public void stopProducer() {
        running = false;
        this.interrupt();
    }

    public int getStopIndex() {
        return stopIndex;
    }

    public int getCapacity() {
        return capacity;
    }

    public synchronized int getCurrentProducts() {
        return currentProducts;
    }

    public int getProductionTime() {
        return productionTime;
    }
}