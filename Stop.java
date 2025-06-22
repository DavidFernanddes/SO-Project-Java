import java.util.concurrent.Semaphore;

public class Stop {
    private int stopIndex;
    private int semaphoreIndex;
    private int capacity;
    private Semaphore capacitySemaphore;

    public Stop() {
        this.stopIndex = 0;
        this.semaphoreIndex = 0;
        this.capacity = 1;
        this.capacitySemaphore = new Semaphore(1);
    }

    public Stop(int stopIndex, int semaphoreIndex) {
        this.stopIndex = stopIndex;
        this.semaphoreIndex = semaphoreIndex;
        this.capacity = 1;
        this.capacitySemaphore = new Semaphore(1);
    }

    public Stop(int stopIndex, int semaphoreIndex, int capacity) {
        this.stopIndex = stopIndex;
        this.semaphoreIndex = semaphoreIndex;
        this.capacity = capacity;
        this.capacitySemaphore = new Semaphore(capacity);
    }

    public int getStopIndex() {
        return stopIndex;
    }

    public void setStopIndex(int stopIndex) {
        this.stopIndex = stopIndex;
    }

    public int getSemaphoreIndex() {
        return semaphoreIndex;
    }

    public void setSemaphoreIndex(int semaphoreIndex) {
        this.semaphoreIndex = semaphoreIndex;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
        this.capacitySemaphore = new Semaphore(capacity);
    }

    public Semaphore getCapacitySemaphore() {
        return capacitySemaphore;
    }

    public void setCapacitySemaphore(Semaphore capacitySemaphore) {
        this.capacitySemaphore = capacitySemaphore;
    }
}