import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Track {
    private final int number;
    private final List<Position> positions;
    private final List<Stop> stops;

    public Track(Scanner scanner) {
        this.number = scanner.nextInt();
        int positionCount = scanner.nextInt();
        int stopCount = scanner.nextInt();

        positions = new ArrayList<>(positionCount);
        stops = new ArrayList<>(stopCount);

        for (int i = 0; i < positionCount; i++) {
            int x = scanner.nextInt();
            int y = scanner.nextInt();
            positions.add(new Position(x, y));
        }

        for (int i = 0; i < stopCount; i++) {
            int posIndex = scanner.nextInt();
            int semaphore = scanner.nextInt();
            int capacity = scanner.nextInt();
            stops.add(new Stop(posIndex, semaphore, capacity, Main.sectionSemaphores[semaphore], semaphore));
        }
    }

    public int getNumber() {
        return number;
    }

    public List<Position> getPositions() {
        return new ArrayList<>(positions);
    }

    public List<Stop> getStops() {
        return new ArrayList<>(stops);
    }

    public boolean isStopPosition(int index) {
        return stops.stream().anyMatch(stop -> stop.getIndex() == index);
    }

    public Stop getStopByPosition(int index) {
        return stops.stream()
                .filter(stop -> stop.getIndex() == index)
                .findFirst()
                .orElse(null);
    }

    public int getNextStopIndex(int currentIndex) {
        int nextIndex = (currentIndex + 1) % positions.size();
        return stops.stream().anyMatch(stop -> stop.getIndex() == nextIndex) ? nextIndex : -1;
    }
}
