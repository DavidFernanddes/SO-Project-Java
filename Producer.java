import java.util.concurrent.Semaphore;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Producer extends Thread {
    private static final Logger logger = Logger.getLogger(Producer.class.getName());

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
        this.productionTime = Math.max(100, productionTime); // Mínimo 100ms
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
        logger.info("Producer started at stop " + stopIndex);

        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(productionTime);

                empty.acquire();
                mutex.acquire();

                try {
                    currentProducts++;
                    logger.fine("Producer: Produto produzido. Total: " + currentProducts + "/" + capacity);
                } finally {
                    mutex.release();
                    full.release();
                }

            } catch (InterruptedException e) {
                logger.info("Producer interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error in producer", e);
                break;
            }
        }

        logger.info("Producer stopped");
    }

    /**
     * Método para comboios retirarem produtos
     */
    public void takeProduct() throws InterruptedException {
        full.acquire();
        mutex.acquire();

        try {
            if (currentProducts > 0) {
                currentProducts--;
                logger.fine("Producer: Produto retirado. Total: " + currentProducts + "/" + capacity);
            }
        } finally {
            mutex.release();
            empty.release();
        }
    }

    /**
     * Verificar se há produtos disponíveis (não bloqueante)
     */
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
            logger.log(Level.WARNING, "Error checking products", e);
            return false;
        }
    }

    /**
     * Para a produção de forma segura
     */
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

    @Override
    public String toString() {
        return String.format("Producer[stop=%d, products=%d/%d, time=%dms]",
                stopIndex, getCurrentProducts(), capacity, productionTime);
    }
}