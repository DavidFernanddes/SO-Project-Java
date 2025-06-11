import java.util.concurrent.Semaphore;

public class Train extends Thread {

    private final int id;
    private final int trackIndex;
    private int positionIndex;
    private final int frequency;
    private char state; // M = moving, S = stopped, W = waiting
    private int tickCount;
    private int currentSection;

    public Train(int id, int trackIndex, int positionIndex, int frequency) {
        this.id = id;
        this.trackIndex = trackIndex;
        this.positionIndex = positionIndex;
        this.frequency = frequency;
        this.state = 'M';
        this.tickCount = 0;
        this.currentSection = 0;
    }

    public int getTrainId() {
        return id;
    }

    public int getTrackIndex() {
        return trackIndex;
    }

    public int getPositionIndex() {
        return positionIndex;
    }

    public char getTrainState() {
        return state;
    }

    public int getCurrentSection() {
        return currentSection;
    }

    @Override
    public void run() {
        Track track = Main.tracks.get(trackIndex);

        while (true) {
            try {
                Thread.sleep(100); // Small delay for simulation effect
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            tickCount++;
            if (tickCount % frequency != 0) continue;

            int nextPos = (positionIndex + 1) % track.getPositions().size();

            // Check if current position is before a stop
            int nextStopIndex = track.getNextStopIndex(positionIndex);
            if (nextStopIndex != -1 && nextStopIndex == nextPos) {
                Stop stop = track.getStopByPosition(nextStopIndex);
                state = 'W';
                try {
                    stop.getCapacitySemaphore().acquire(); // wait for stop capacity
                    Main.sectionSemaphores[stop.getSectionSemaphoreIndex()].acquire(); // wait for section
                    positionIndex = nextStopIndex;
                    state = 'S';
                    if (currentSection < track.getStops().size())
                        Main.sectionSemaphores[track.getStops().get(currentSection).getSectionSemaphoreIndex()].release();
                    currentSection = track.getStops().indexOf(stop);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // If currently on a stop, move to next segment if possible
            else if (track.isStopPosition(positionIndex)) {
                Stop currentStop = track.getStopByPosition(positionIndex);
                state = 'W';
                try {
                    Main.sectionSemaphores[currentStop.getSectionSemaphoreIndex()].acquire(); // wait for section
                    positionIndex = nextPos;
                    state = 'M';
                    currentStop.getCapacitySemaphore().release(); // leave stop
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // Just move normally
            else {
                positionIndex = nextPos;
                state = 'M';
            }
        }
    }
}