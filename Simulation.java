import java.io.*;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class Simulation {
    public static final int MAX_TRACKS = 5;
    public static final int MAX_TRAINS = 10;
    public static final int BOARD_SIZE = 10;
    public static final int MAX_SEMAPHORES = 20; // Aumentado para 20 conforme especificação

    private Track[] tracks;
    private Train[] trains;
    private char[][] board;
    private Semaphore[] semaphores;
    private Semaphore mutex;
    private int numTrains;

    public Simulation() {
        this.tracks = new Track[MAX_TRACKS];
        this.trains = new Train[MAX_TRAINS];
        this.board = new char[BOARD_SIZE][BOARD_SIZE];
        this.semaphores = new Semaphore[MAX_SEMAPHORES];
        this.mutex = new Semaphore(1);
        this.numTrains = 0;

        // Initialize arrays
        for (int i = 0; i < MAX_TRACKS; i++) {
            tracks[i] = new Track();
        }
        for (int i = 0; i < MAX_TRAINS; i++) {
            trains[i] = new Train();
        }
        for (int i = 0; i < MAX_SEMAPHORES; i++) {
            semaphores[i] = new Semaphore(1);
        }
    }

    public int readFile(String fileName, int fileType) throws IOException {
        Scanner scanner = new Scanner(new File(fileName));

        int numTracks = scanner.nextInt();
        numTrains = scanner.nextInt();

        System.out.println("Reading " + numTracks + " tracks and " + numTrains + " trains");

        for (int t = 0; t < numTracks; t++) {
            tracks[t].setNum(scanner.nextInt());
            tracks[t].setSize(scanner.nextInt());
            tracks[t].setNumStops(scanner.nextInt());

            System.out.println("Track " + tracks[t].getNum() + " size: " + tracks[t].getSize() + " stops: " + tracks[t].getNumStops());

            // Read track positions
            for (int p = 0; p < tracks[t].getSize(); p++) {
                int x = scanner.nextInt();
                int y = scanner.nextInt();
                tracks[t].setPosition(p, new Position(x, y));
                System.out.println("  Position " + p + ": (" + x + "," + y + ")");
            }

            // Read stops
            for (int s = 0; s < tracks[t].getNumStops(); s++) {
                int stopIndex = scanner.nextInt();
                int semaphoreIndex = scanner.nextInt();
                int capacity = scanner.nextInt();
                tracks[t].setStop(s, new Stop(stopIndex, semaphoreIndex, capacity));
                System.out.println("  Stop " + s + ": index=" + stopIndex + " sem=" + semaphoreIndex + " cap=" + capacity);
            }
        }

        // Read trains
        for (int c = 0; c < numTrains; c++) {
            int trainNum = scanner.nextInt();
            int trackIndex = scanner.nextInt();
            int position = scanner.nextInt();
            int speed = scanner.nextInt();

            trains[c].setNum(trainNum);
            trains[c].setTrack(trackIndex);
            trains[c].setPosition(position);
            trains[c].setSpeed(speed);
            trains[c].setCounter(0);
            trains[c].setTrainState('M');

            System.out.println("Train " + trains[c].getNum() + ": track=" + trains[c].getTrack() +
                    " pos=" + trains[c].getPosition() + " speed=" + trains[c].getSpeed());
        }

        scanner.close();
        return numTrains;
    }

    public synchronized void fillBoard() {
        // Clear board
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = ' ';
            }
        }

        // Add track positions - apenas processar tracks que existem
        for (int t = 0; t < MAX_TRACKS; t++) {
            Track track = tracks[t];
            if (track == null || track.getSize() <= 0) continue;

            for (int p = 0; p < track.getSize(); p++) {
                Position pos = track.getPosition(p);
                if (pos == null) continue;

                int x = pos.getX();
                int y = pos.getY();

                if (x >= 0 && x < BOARD_SIZE && y >= 0 && y < BOARD_SIZE) {
                    boolean isStop = false;

                    // Check if this position is a stop
                    for (int s = 0; s < track.getNumStops(); s++) {
                        Stop stop = track.getStop(s);
                        if (stop != null && stop.getStopIndex() == p) {
                            board[y][x] = 'S';
                            isStop = true;
                            break;
                        }
                    }

                    if (!isStop) {
                        board[y][x] = 'X';
                    }
                }
            }
        }

        // Add trains (sobrepõe as posições das tracks)
        for (int c = 0; c < numTrains; c++) {
            Train train = trains[c];
            if (train == null) continue;

            int trackIdx = train.getTrack();
            int posIdx = train.getPosition();

            if (trackIdx < 0 || trackIdx >= MAX_TRACKS) continue;

            Track track = tracks[trackIdx];
            if (track == null || posIdx < 0 || posIdx >= track.getSize()) continue;

            Position pos = track.getPosition(posIdx);
            if (pos == null) continue;

            int x = pos.getX();
            int y = pos.getY();

            if (x >= 0 && x < BOARD_SIZE && y >= 0 && y < BOARD_SIZE) {
                board[y][x] = (char)('0' + (train.getNum() % 10));
            }
        }
    }

    public synchronized void printState() {
        System.out.println("Tracks");
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                System.out.print("   " + board[i][j]);
            }
            System.out.println();
        }

        System.out.println();
        System.out.println("Trains:");
        for (int c = 0; c < numTrains; c++) {
            Train train = trains[c];
            int trackIdx = train.getTrack();
            int posIdx = train.getPosition();

            if (trackIdx >= 0 && trackIdx < MAX_TRACKS &&
                    posIdx >= 0 && posIdx < tracks[trackIdx].getSize()) {
                Position pos = tracks[trackIdx].getPosition(posIdx);

                if (pos != null) {
                    System.out.printf("T%d (%d,%d) - %d - %c%n",
                            train.getNum(), pos.getX(), pos.getY(),
                            train.getSection(), train.getTrainState());
                }
            }
        }
        System.out.println();
        System.out.println("----------------------------------------");
        System.out.println();
    }

    public void startSimulation(int waitTime, int fileType) {
        // Initialize trains with wait time
        for (int i = 0; i < numTrains; i++) {
            trains[i].initialize(tracks, semaphores, mutex, waitTime, fileType);
            trains[i].start();
        }

        // Main visualization loop
        while (true) {
            try {
                mutex.acquire();
                fillBoard();
                printState();
                mutex.release();
                Thread.sleep(waitTime); // Usar waitTime diretamente, não dividir por 1000
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
            System.out.println("Usage: java Simulation <input_file> <wait_time_ms> [file_type]");
            System.exit(1);
        }

        String fileName = args[0];
        int waitTime = Integer.parseInt(args[1]); // Manter em milissegundos, não converter
        int fileType = (args.length == 3) ? Integer.parseInt(args[2]) : 1; // Default 1, não 0

        Simulation simulation = new Simulation();

        try {
            int numTrains = simulation.readFile(fileName, fileType);
            if (numTrains == 0) {
                System.err.println("No trains loaded from file");
                System.exit(1);
            }

            simulation.startSimulation(waitTime, fileType);

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            System.exit(1);
        }
    }
}