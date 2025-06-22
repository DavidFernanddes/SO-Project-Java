import java.util.concurrent.Semaphore;

public class Train extends Thread {
    private int num;
    private int track;
    private int position;
    private int speed;
    private int counter;
    private char state;
    private int section;
    private int lastStop;
    private int nextStop;

    // References to shared data
    private Track[] tracks;
    private Semaphore[] semaphores;
    private Semaphore mutex;
    private int waitTime;
    private int fileType;

    public Train() {
        this.num = 0;
        this.track = 0;
        this.position = 0;
        this.speed = 1;
        this.counter = 0;
        this.state = 'M';
        this.section = 0;
        this.lastStop = -1;
        this.nextStop = -1;
    }

    public Train(int num, int track, int position, int speed) {
        this();
        this.num = num;
        this.track = track;
        this.position = position;
        this.speed = speed;
    }

    public void initialize(Track[] tracks, Semaphore[] semaphores, Semaphore mutex, int waitTime, int fileType) {
        this.tracks = tracks;
        this.semaphores = semaphores;
        this.mutex = mutex;
        this.waitTime = waitTime;
        this.fileType = fileType;

        if (fileType == 2) {
            findTrainStops();
        }
    }

    @Override
    public void run() {
        if (fileType == 2) {
            trainProcessWithStops();
        } else {
            trainProcessSimple();
        }
    }

    private void trainProcessSimple() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                mutex.acquire();
                counter++;
                if (counter % speed == 0) {
                    position++;
                    if (position >= tracks[track].getSize()) {
                        position = 0;
                    }
                }
                mutex.release();
                Thread.sleep(waitTime / 1000); // Convert microseconds to milliseconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void trainProcessWithStops() {
        Track currentTrack = tracks[track];

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep((waitTime * speed) / 1000);

                mutex.acquire();
                counter++;

                if (counter % speed == 0) {
                    moveTrainStep(currentTrack);
                }

                mutex.release();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void moveTrainStep(Track currentTrack) throws InterruptedException {
        // Check if we're currently at a stop
        if (isAtStop(currentTrack)) {
            // We're at a stop, try to leave
            state = 'W'; // Waiting to leave stop

            int nextSemIdx = currentTrack.getStop(nextStop).getSemaphoreIndex();

            // Release mutex before trying to acquire section semaphore
            mutex.release();

            // Try to acquire next section (this will block if section is occupied)
            semaphores[nextSemIdx].acquire();

            // Re-acquire mutex to update state
            mutex.acquire();

            // Successfully acquired next section, move out of stop
            state = 'M';
            position++;
            if (position >= currentTrack.getSize()) {
                position = 0;
            }

            // Release stop capacity semaphore
            currentTrack.getStop(section).getCapacitySemaphore().release();

            // Update section tracking
            section = nextStop;
            nextStop = (nextStop + 1) % currentTrack.getNumStops();

        } else {
            // Check if next position is a stop
            int nextPos = (position + 1) % currentTrack.getSize();
            boolean nextIsStop = false;
            int stopIndex = -1;

            for (int s = 0; s < currentTrack.getNumStops(); s++) {
                if (currentTrack.getStop(s).getStopIndex() == nextPos) {
                    nextIsStop = true;
                    stopIndex = s;
                    break;
                }
            }

            if (nextIsStop) {
                // Next position is a stop, try to enter
                state = 'W'; // Waiting to enter stop

                Stop stop = currentTrack.getStop(stopIndex);

                // Release mutex before trying to acquire stop capacity
                mutex.release();

                // Try to acquire stop capacity (this will block if stop is full)
                stop.getCapacitySemaphore().acquire();

                // Re-acquire mutex to update state
                mutex.acquire();

                // Successfully acquired stop capacity, move to stop
                state = 'S';
                position = nextPos;

                // Release previous section semaphore
                if (section >= 0 && section < currentTrack.getNumStops()) {
                    int prevSemIdx = currentTrack.getStop(section).getSemaphoreIndex();
                    semaphores[prevSemIdx].release();
                }

                // Update section tracking
                section = stopIndex;

            } else {
                // Normal movement within section
                state = 'M';
                position++;
                if (position >= currentTrack.getSize()) {
                    position = 0;
                }
            }
        }
    }

    private boolean isAtStop(Track currentTrack) {
        for (int s = 0; s < currentTrack.getNumStops(); s++) {
            if (currentTrack.getStop(s).getStopIndex() == position) {
                return true;
            }
        }
        return false;
    }

    private void findTrainStops() {
        Track currentTrack = tracks[track];
        int pos = position;
        int lastStopIdx = -1;
        int nextStopIdx = -1;

        // Find current or last passed stop
        for (int s = 0; s < currentTrack.getNumStops(); s++) {
            if (currentTrack.getStop(s).getStopIndex() <= pos) {
                if (lastStopIdx == -1 ||
                        currentTrack.getStop(s).getStopIndex() > currentTrack.getStop(lastStopIdx).getStopIndex()) {
                    lastStopIdx = s;
                }
            }
        }

        if (lastStopIdx == -1) {
            lastStopIdx = currentTrack.getNumStops() - 1;
        }

        // Find next stop
        for (int s = 0; s < currentTrack.getNumStops(); s++) {
            if (currentTrack.getStop(s).getStopIndex() > pos) {
                if (nextStopIdx == -1 ||
                        currentTrack.getStop(s).getStopIndex() < currentTrack.getStop(nextStopIdx).getStopIndex()) {
                    nextStopIdx = s;
                }
            }
        }

        if (nextStopIdx == -1) {
            nextStopIdx = 0;
        }

        lastStop = lastStopIdx;
        nextStop = nextStopIdx;
        section = lastStopIdx;

        // Check if currently at a stop
        boolean atStop = false;
        for (int s = 0; s < currentTrack.getNumStops(); s++) {
            if (currentTrack.getStop(s).getStopIndex() == pos) {
                state = 'S';
                section = s;
                atStop = true;
                break;
            }
        }

        // If not at a stop, acquire semaphore for current section
        if (!atStop && section >= 0 && section < currentTrack.getNumStops()) {
            try {
                int semIdx = currentTrack.getStop(section).getSemaphoreIndex();
                semaphores[semIdx].acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Getters and setters
    public synchronized int getNum() { return num; }
    public synchronized void setNum(int num) { this.num = num; }

    public synchronized int getTrack() { return track; }
    public synchronized void setTrack(int track) { this.track = track; }

    public synchronized int getPosition() { return position; }
    public synchronized void setPosition(int position) { this.position = position; }

    public synchronized int getSpeed() { return speed; }
    public synchronized void setSpeed(int speed) { this.speed = speed; }

    public synchronized int getCounter() { return counter; }
    public synchronized void setCounter(int counter) { this.counter = counter; }

    public synchronized char getTrainState() { return state; }
    public synchronized void setTrainState(char state) { this.state = state; }

    public synchronized int getSection() { return section; }
    public synchronized void setSection(int section) { this.section = section; }

    public synchronized int getLastStop() { return lastStop; }
    public synchronized void setLastStop(int lastStop) { this.lastStop = lastStop; }

    public synchronized int getNextStop() { return nextStop; }
    public synchronized void setNextStop(int nextStop) { this.nextStop = nextStop; }
}