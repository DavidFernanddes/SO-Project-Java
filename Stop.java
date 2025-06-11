import java.util.concurrent.Semaphore;

public class Stop {
    private final int index;
    private final int sectionSemaphore;
    private final int capacity;
    private final Semaphore stopSemaphore;
    private final Semaphore capacitySemaphore;
    private final int sectionSemaphoreIndex;

    public Stop(int index, int sectionSemaphore, int capacity, Semaphore capacitySemaphore, int sectionSemaphoreIndex) {
        this.index = index;
        this.sectionSemaphore = sectionSemaphore;
        this.capacity = capacity;
        this.stopSemaphore = new Semaphore(capacity);
        this.capacitySemaphore = capacitySemaphore;
        this.sectionSemaphoreIndex = sectionSemaphoreIndex;
    }

    public int getIndex() {
        return index;
    }

    public int getSemaphoreIndex() {
        return sectionSemaphore;
    }

    public int getStopCapacity() {
        return capacity;
    }

    public Semaphore getStopSemaphore() {
        return stopSemaphore;
    }

    public Semaphore getCapacitySemaphore() {
        return capacitySemaphore;
    }

    public int getSectionSemaphoreIndex() {
        return sectionSemaphoreIndex;
    }
}
