import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.Semaphore;

public class Main {

    public static final int MAP_SIZE = 10;
    public static final int MAX_SECTIONS = 20;

    public static char[][] map = new char[MAP_SIZE][MAP_SIZE];
    public static List<Track> tracks = new ArrayList<>();
    public static List<Train> trains = new ArrayList<>();
    public static Semaphore[] sectionSemaphores = new Semaphore[MAX_SECTIONS];

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java Main <filename> <refresh_ms> <phase>");
            return;
        }

        String filename = args[0];
        int refreshTime = Integer.parseInt(args[1]);
        int phase = Integer.parseInt(args[2]);

        if (phase != 1) {
            System.out.println("Only Phase 1 is supported for now.");
            return;
        }

        // Init section semaphores
        for (int i = 0; i < MAX_SECTIONS; i++) {
            sectionSemaphores[i] = new Semaphore(1);
        }

        try {
            Scanner sc = new Scanner(new File(filename));
            int numTracks = sc.nextInt();
            int numTrains = sc.nextInt();

            for (int i = 0; i < numTracks; i++) {
                Track t = new Track(sc);
                tracks.add(t);
            }

            for (int i = 0; i < numTrains; i++) {
                int id = sc.nextInt();
                int trackIndex = sc.nextInt();
                int posIndex = sc.nextInt();
                int speed = sc.nextInt();
                Train train = new Train(id, trackIndex, posIndex, speed);
                trains.add(train);
            }

            for (Train t : trains) {
                t.start();
            }

            while (true) {
                updateMap();
                printMap();
                printTrains();
                Thread.sleep(refreshTime);
            }

        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + filename);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized void updateMap() {
        for (int i = 0; i < MAP_SIZE; i++) {
            Arrays.fill(map[i], ' ');
        }

        for (Track track : tracks) {
            for (Position p : track.getPositions()) {
                map[p.getY()][p.getX()] = 'X';
            }
            for (Stop stop : track.getStops()) {
                Position p = track.getPositions().get(stop.getIndex());
                map[p.getY()][p.getX()] = 'S';
            }
        }

        for (Train t : trains) {
            Position p = tracks.get(t.getTrackIndex()).getPositions().get(t.getPositionIndex());
            map[p.getY()][p.getX()] = (char) ('0' + t.getId());
        }
    }

    public static synchronized void printMap() {
        System.out.println("Tracks:\n");
        for (int i = 0; i < MAP_SIZE; i++) {
            for (int j = 0; j < MAP_SIZE; j++) {
                System.out.print(" " + map[i][j]);
            }
            System.out.println();
        }
        System.out.println();
    }

    public static synchronized void printTrains() {
        System.out.println("Trains:");
        for (Train t : trains) {
            System.out.printf("T%d (%d,%d) - %d - %s\n",
                    t.getId(),
                    tracks.get(t.getTrackIndex()).getPositions().get(t.getPositionIndex()).getX(),
                    tracks.get(t.getTrackIndex()).getPositions().get(t.getPositionIndex()).getY(),
                    t.getCurrentSection(),
                    t.getState()
            );
        }
        System.out.println();
    }
}