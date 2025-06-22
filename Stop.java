import java.util.concurrent.Semaphore;

public class Stop {
    private int stopIndex;
    private int semaphoreIndex;
    private int capacity;
    private Semaphore capacitySemaphore;

    public Stop() {
        this(0, 0, 1);
    }

    public Stop(int stopIndex, int semaphoreIndex) {
        this(stopIndex, semaphoreIndex, 1);
    }

    public Stop(int stopIndex, int semaphoreIndex, int capacity) {
        this.stopIndex = stopIndex;
        this.semaphoreIndex = semaphoreIndex;
        this.capacity = Math.max(1, capacity);
        this.capacitySemaphore = new Semaphore(this.capacity);
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
        this.capacity = Math.max(1, capacity);
        this.capacitySemaphore = new Semaphore(this.capacity);
    }

    public Semaphore getCapacitySemaphore() {
        return capacitySemaphore;
    }
}