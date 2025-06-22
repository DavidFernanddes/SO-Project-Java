import java.util.concurrent.Semaphore;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Consumer extends Thread {
    private static final Logger logger = Logger.getLogger(Consumer.class.getName());

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
        this.requestTime = Math.max(100, requestTime); // Mínimo 100ms
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
        logger.info("Consumer started at stop " + stopIndex);

        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(requestTime);

                empty.acquire();
                mutex.acquire();

                try {
                    currentRequests++;
                    logger.fine("Consumer: Pedido criado. Total: " + currentRequests + "/" + capacity);
                } finally {
                    mutex.release();
                    full.release();
                }

            } catch (InterruptedException e) {
                logger.info("Consumer interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error in consumer", e);
                break;
            }
        }

        logger.info("Consumer stopped");
    }

    /**
     * Método para comboios obterem pedidos
     */
    public void takeRequest() throws InterruptedException {
        full.acquire();
        mutex.acquire();

        try {
            if (currentRequests > 0) {
                currentRequests--;
                logger.fine("Consumer: Pedido retirado. Total: " + currentRequests + "/" + capacity);
            }
        } finally {
            mutex.release();
            empty.release();
        }
    }

    /**
     * Método para comboios entregarem produtos (consumir pedido e produto)
     */
    public void deliverProduct() throws InterruptedException {
        mutex.acquire();
        try {
            logger.fine("Consumer: Produto entregue!");
        } finally {
            mutex.release();
        }
    }

    /**
     * Verificar se há pedidos disponíveis (não bloqueante)
     */
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
            logger.log(Level.WARNING, "Error checking requests", e);
            return false;
        }
    }

    /**
     * Para o consumo de forma segura
     */
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

    @Override
    public String toString() {
        return String.format("Consumer[stop=%d, requests=%d/%d, time=%dms]",
                stopIndex, getCurrentRequests(), capacity, requestTime);
    }
}