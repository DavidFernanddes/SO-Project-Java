import java.util.concurrent.Semaphore;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Train extends Thread {
    private static final Logger logger = Logger.getLogger(Train.class.getName());

    private static final char STATE_MOVING = 'M';
    private static final char STATE_WAITING = 'W';
    private static final char STATE_AT_STOP = 'S';
    private static final char STATE_WAITING_CONSUMER = 'C';
    private static final char STATE_WAITING_PRODUCER = 'P';

    private final int num;
    private final int track;
    private volatile int position;
    private final int speed;
    private volatile int counter;
    private volatile char state;
    private volatile int section;

    private volatile boolean hasRequest;
    private volatile boolean hasProduct;

    private Track[] tracks;
    private Semaphore[] semaphores;
    private Semaphore mutex;
    private int waitTime;
    private int fileType;

    private Producer producer;
    private Consumer consumer;

    public Train() {
        this(0, 0, 0, 1);
    }

    public Train(int num, int track, int position, int speed) {
        this.num = num;
        this.track = track;
        this.position = position;
        this.speed = Math.max(1, speed);
        this.counter = 0;
        this.state = STATE_MOVING;
        this.section = 0;
        this.hasRequest = false;
        this.hasProduct = false;

        this.setName("Train-" + num);
        this.setDaemon(true);
    }

    public void initialize(Track[] tracks, Semaphore[] semaphores, Semaphore mutex,
                           int waitTime, int fileType) {
        this.tracks = tracks;
        this.semaphores = semaphores;
        this.mutex = mutex;
        this.waitTime = Math.max(1, waitTime);
        this.fileType = fileType;

        initializePosition();
    }

    public void setProducerConsumer(Producer producer, Consumer consumer) {
        this.producer = producer;
        this.consumer = consumer;
    }

    @Override
    public void run() {
        logger.info("Train " + num + " started");

        try {
            if (fileType == 2) {
                runProducerConsumerMode();
            } else {
                runBasicMode();
            }
        } catch (InterruptedException e) {
            logger.info("Train " + num + " interrupted");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in train " + num, e);
        } finally {
            cleanup();
        }
    }

    private void runProducerConsumerMode() throws InterruptedException {
        Track currentTrack = tracks[track];

        while (!Thread.currentThread().isInterrupted()) {
            Thread.sleep(waitTime);

            mutex.acquire();
            try {
                counter++;
                if (counter % speed == 0) {
                    processProducerConsumerLogic(currentTrack);
                }
            } finally {
                mutex.release();
            }
        }
    }

    private void processProducerConsumerLogic(Track currentTrack) throws InterruptedException {
        if (!hasRequest) {
            handleNoRequest(currentTrack);
        } else if (hasRequest && !hasProduct) {
            handleNeedProduct(currentTrack);
        } else if (hasRequest && hasProduct) {
            handleDelivery(currentTrack);
        } else {
            moveTrainStep(currentTrack);
        }
    }

    private void handleNoRequest(Track currentTrack) throws InterruptedException {
        if (isAtConsumerStop(currentTrack)) {
            state = STATE_WAITING_CONSUMER;
            mutex.release();

            try {
                if (consumer != null && consumer.hasRequests()) {
                    consumer.takeRequest();
                    mutex.acquire();
                    hasRequest = true;
                    state = STATE_AT_STOP;
                } else {
                    Thread.sleep(100);
                    mutex.acquire();
                    state = STATE_WAITING_CONSUMER;
                }
            } catch (InterruptedException e) {
                mutex.acquire();
                throw e;
            }
        } else {
            moveTrainStep(currentTrack);
        }
    }

    private void handleNeedProduct(Track currentTrack) throws InterruptedException {
        if (isAtProducerStop(currentTrack)) {
            state = STATE_WAITING_PRODUCER;
            mutex.release();

            try {
                if (producer != null && producer.hasProducts()) {
                    producer.takeProduct();
                    mutex.acquire();
                    hasProduct = true;
                    state = STATE_AT_STOP;
                } else {
                    mutex.acquire();
                    state = STATE_MOVING;
                }
            } catch (InterruptedException e) {
                mutex.acquire();
                throw e;
            }
        } else {
            moveTrainStep(currentTrack);
        }
    }

    private void handleDelivery(Track currentTrack) throws InterruptedException {
        if (isAtConsumerStop(currentTrack)) {
            mutex.release();

            try {
                if (consumer != null) {
                    consumer.deliverProduct();
                }
                mutex.acquire();
                hasRequest = false;
                hasProduct = false;
                state = STATE_AT_STOP;
            } catch (InterruptedException e) {
                mutex.acquire();
                throw e;
            }
        } else {
            moveTrainStep(currentTrack);
        }
    }

    private void runBasicMode() throws InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            Thread.sleep(waitTime);

            mutex.acquire();
            try {
                counter++;
                if (counter % speed == 0) {
                    Track currentTrack = tracks[track];
                    moveTrainStep(currentTrack);
                }
            } finally {
                mutex.release();
            }
        }
    }

    private void moveTrainStep(Track currentTrack) throws InterruptedException {
        int nextPos = (position + 1) % currentTrack.getSize();

        boolean currentlyAtStop = currentTrack.isStopPosition(position);
        boolean nextIsStop = currentTrack.isStopPosition(nextPos);

        if (currentlyAtStop) {
            handleExitFromStop(currentTrack, nextPos);
        } else if (nextIsStop) {
            handleEnterStop(currentTrack, nextPos);
        } else {
            position = nextPos;
            state = STATE_MOVING;
        }
    }

    private void handleExitFromStop(Track currentTrack, int nextPos) throws InterruptedException {
        state = STATE_WAITING;

        int currentStopIndex = currentTrack.findStopIndexAtPosition(position);
        if (currentStopIndex == -1) return;

        int nextSectionIndex = (currentStopIndex + 1) % currentTrack.getNumStops();
        Stop nextSection = currentTrack.getStop(nextSectionIndex);

        mutex.release();

        try {
            if (semaphores[nextSection.getSemaphoreIndex()].tryAcquire()) {
                mutex.acquire();

                position = nextPos;
                state = STATE_MOVING;
                section = nextSectionIndex;

                currentTrack.getStop(currentStopIndex).getCapacitySemaphore().release();
            } else {
                mutex.acquire();
                state = STATE_WAITING;
            }
        } catch (InterruptedException e) {
            mutex.acquire();
            throw e;
        }
    }

    private void handleEnterStop(Track currentTrack, int nextPos) throws InterruptedException {
        state = STATE_WAITING;
        int nextStopIndex = currentTrack.findStopIndexAtPosition(nextPos);
        if (nextStopIndex == -1) return;

        Stop nextStop = currentTrack.getStop(nextStopIndex);

        mutex.release();

        try {
            if (nextStop.getCapacitySemaphore().tryAcquire()) {
                mutex.acquire();

                position = nextPos;
                state = STATE_AT_STOP;

                if (section >= 0 && section < currentTrack.getNumStops()) {
                    Stop currentSection = currentTrack.getStop(section);
                    semaphores[currentSection.getSemaphoreIndex()].release();
                }

                section = nextStopIndex;
            } else {
                mutex.acquire();
                state = STATE_WAITING;
            }
        } catch (InterruptedException e) {
            mutex.acquire();
            throw e;
        }
    }

    private boolean isAtConsumerStop(Track currentTrack) {
        if (consumer == null) return false;

        int consumerStopIndex = consumer.getStopIndex();
        Stop consumerStop = currentTrack.getStop(consumerStopIndex);

        return consumerStop != null && consumerStop.getStopIndex() == position;
    }

    private boolean isAtProducerStop(Track currentTrack) {
        if (producer == null) return false;

        int producerStopIndex = producer.getStopIndex();
        Stop producerStop = currentTrack.getStop(producerStopIndex);

        return producerStop != null && producerStop.getStopIndex() == position;
    }

    private void initializePosition() {
        Track currentTrack = tracks[track];

        int currentSection = 0;
        for (int s = 0; s < currentTrack.getNumStops(); s++) {
            Stop stop = currentTrack.getStop(s);
            if (stop != null && stop.getStopIndex() <= position) {
                currentSection = s;
            }
        }

        this.section = currentSection;

        boolean atStop = currentTrack.isStopPosition(position);
        if (atStop) {
            state = STATE_AT_STOP;
        } else {
            if (currentSection < currentTrack.getNumStops()) {
                try {
                    Stop currentStop = currentTrack.getStop(currentSection);
                    if (currentStop != null) {
                        semaphores[currentStop.getSemaphoreIndex()].acquire();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void cleanup() {
        try {
            Track currentTrack = tracks[track];

            if (state == STATE_AT_STOP && section >= 0 && section < currentTrack.getNumStops()) {
                currentTrack.getStop(section).getCapacitySemaphore().release();
            }

            if (state == STATE_MOVING && section >= 0 && section < currentTrack.getNumStops()) {
                Stop currentSection = currentTrack.getStop(section);
                if (currentSection != null) {
                    semaphores[currentSection.getSemaphoreIndex()].release();
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error during cleanup for train " + num, e);
        }

        logger.info("Train " + num + " stopped");
    }

    public synchronized int getNum() { return num; }
    public synchronized int getTrack() { return track; }
    public synchronized int getPosition() { return position; }
    public synchronized int getSpeed() { return speed; }
    public synchronized int getCounter() { return counter; }
    public synchronized char getTrainState() { return state; }
    public synchronized int getSection() { return section; }
    public synchronized boolean hasRequest() { return hasRequest; }
    public synchronized boolean hasProduct() { return hasProduct; }

    public synchronized void setPosition(int position) { this.position = position; }
    public synchronized void setCounter(int counter) { this.counter = counter; }
    public synchronized void setTrainState(char state) { this.state = state; }
    public synchronized void setSection(int section) { this.section = section; }
    public synchronized void setHasRequest(boolean hasRequest) { this.hasRequest = hasRequest; }
    public synchronized void setHasProduct(boolean hasProduct) { this.hasProduct = hasProduct; }

    @Override
    public String toString() {
        return String.format("Train %d: pos=%d, section=%d, state=%c, hasRequest=%b, hasProduct=%b",
                num, position, section, state, hasRequest, hasProduct);
    }
}