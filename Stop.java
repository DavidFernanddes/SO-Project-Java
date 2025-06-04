import java.util.concurrent.Semaphore;

public class Stop {
    private int stopIndex;
    private int semaphoreIndex;
    private int stopCapacity;
    private Semaphore stopSemaphore;

    public Stop(int stopIndex, int semaphoreIndex, int stopCapacity) {
        this.stopIndex = stopIndex;
        this.semaphoreIndex = semaphoreIndex;
        this.stopCapacity = stopCapacity;
        this.stopSemaphore = new Semaphore(stopCapacity, true); // justo (FIFO)
    }

    public int getStopIndex() {
        return stopIndex;
    }

    public int getSemaphoreIndex() {
        return semaphoreIndex;
    }

    public int getStopCapacity() {
        return stopCapacity;
    }

    public Semaphore getStopSemaphore() {
        return stopSemaphore;
    }

    public void enter() {
        try {
            stopSemaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void leave() {
        stopSemaphore.release();
    }
}
