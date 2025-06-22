import java.io.*;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class Simulation {
    public static final int MAX_TRACKS = 5;
    public static final int MAX_TRAINS = 10;
    public static final int BOARD_SIZE = 10;
    public static final int MAX_SEMAPHORES = 20;

    private Track[] tracks;
    private Train[] trains;
    private char[][] board;
    private Semaphore[] semaphores;
    private Semaphore mutex;
    private int numTrains;
    private int numTracks;

    private Producer[] producers;
    private Consumer[] consumers;

    public Simulation() {
        this.tracks = new Track[MAX_TRACKS];
        this.trains = new Train[MAX_TRAINS];
        this.board = new char[BOARD_SIZE][BOARD_SIZE];
        this.semaphores = new Semaphore[MAX_SEMAPHORES];
        this.mutex = new Semaphore(1);
        this.numTrains = 0;
        this.numTracks = 0;

        this.producers = new Producer[MAX_TRACKS];
        this.consumers = new Consumer[MAX_TRACKS];

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
        try (Scanner scanner = new Scanner(new File(fileName))) {
            numTracks = scanner.nextInt();
            numTrains = scanner.nextInt();

            if (numTracks <= 0 || numTracks > MAX_TRACKS) {
                throw new IllegalArgumentException("Invalid number of tracks: " + numTracks);
            }
            if (numTrains <= 0 || numTrains > MAX_TRAINS) {
                throw new IllegalArgumentException("Invalid number of trains: " + numTrains);
            }

            for (int t = 0; t < numTracks; t++) {
                readTrackData(scanner, t, fileType);
            }

            for (int c = 0; c < numTrains; c++) {
                readTrainData(scanner, c);
            }

            return numTrains;
        }
    }

    private void readTrackData(Scanner scanner, int trackIndex, int fileType) {
        tracks[trackIndex].setNum(scanner.nextInt());
        tracks[trackIndex].setSize(scanner.nextInt());
        tracks[trackIndex].setNumStops(scanner.nextInt());

        if (fileType == 2) {
            readProducerConsumerData(scanner, trackIndex);
        }

        for (int p = 0; p < tracks[trackIndex].getSize(); p++) {
            int x = scanner.nextInt();
            int y = scanner.nextInt();
            tracks[trackIndex].setPosition(p, new Position(x, y));
        }

        for (int s = 0; s < tracks[trackIndex].getNumStops(); s++) {
            int stopIndex = scanner.nextInt();
            int semaphoreIndex = scanner.nextInt();
            int capacity = scanner.nextInt();

            if (semaphoreIndex >= MAX_SEMAPHORES) {
                throw new IllegalArgumentException("Semaphore index too high: " + semaphoreIndex);
            }

            tracks[trackIndex].setStop(s, new Stop(stopIndex, semaphoreIndex, capacity));
        }
    }

    private void readProducerConsumerData(Scanner scanner, int trackIndex) {
        int producerStopIndex = scanner.nextInt();
        int producerCapacity = scanner.nextInt();
        int producerTime = scanner.nextInt() * 1000;

        int consumerStopIndex = scanner.nextInt();
        int consumerCapacity = scanner.nextInt();
        int consumerTime = scanner.nextInt() * 1000;

        producers[trackIndex] = new Producer(producerStopIndex, producerCapacity, producerTime);
        consumers[trackIndex] = new Consumer(consumerStopIndex, consumerCapacity, consumerTime);
    }

    private void readTrainData(Scanner scanner, int trainIndex) {
        int trainNum = scanner.nextInt();
        int trackIndex = scanner.nextInt();
        int position = scanner.nextInt();
        int speed = scanner.nextInt();

        if (trackIndex >= numTracks) {
            throw new IllegalArgumentException("Train " + trainNum + " references invalid track: " + trackIndex);
        }

        trains[trainIndex] = new Train(trainNum, trackIndex, position, speed);
    }

    public synchronized void fillBoard() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = ' ';
            }
        }

        for (int t = 0; t < numTracks; t++) {
            Track track = tracks[t];
            if (track == null || track.getSize() <= 0) continue;

            for (int p = 0; p < track.getSize(); p++) {
                Position pos = track.getPosition(p);
                if (pos == null) continue;

                int x = pos.getX();
                int y = pos.getY();

                if (x >= 0 && x < BOARD_SIZE && y >= 0 && y < BOARD_SIZE) {
                    if (track.isStopPosition(p)) {
                        board[y][x] = 'S';

                        if (producers[t] != null && producers[t].getStopIndex() == track.findStopIndexAtPosition(p)) {
                            board[y][x] = 'P';
                        } else if (consumers[t] != null && consumers[t].getStopIndex() == track.findStopIndexAtPosition(p)) {
                            board[y][x] = 'C';
                        }
                    } else {
                        board[y][x] = 'X';
                    }
                }
            }
        }

        for (int c = 0; c < numTrains; c++) {
            Train train = trains[c];
            if (train == null) continue;

            int trackIdx = train.getTrack();
            int posIdx = train.getPosition();

            if (trackIdx < 0 || trackIdx >= numTracks) continue;

            Track track = tracks[trackIdx];
            if (track == null || posIdx < 0 || posIdx >= track.getSize()) continue;

            Position pos = track.getPosition(posIdx);
            if (pos == null) continue;

            int x = pos.getX();
            int y = pos.getY();

            if (x >= 0 && x < BOARD_SIZE && y >= 0 && y < BOARD_SIZE) {
                board[y][x] = (char) ('0' + (train.getNum() % 10));
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
            printTrainInfo(train);
        }

        if (hasProducersConsumers()) {
            printProducerConsumerState();
        }

        System.out.println();
        System.out.println("----------------------------------------");
        System.out.println();
    }

    private void printTrainInfo(Train train) {
        int trackIdx = train.getTrack();
        int posIdx = train.getPosition();

        if (trackIdx >= 0 && trackIdx < numTracks &&
                posIdx >= 0 && posIdx < tracks[trackIdx].getSize()) {
            Position pos = tracks[trackIdx].getPosition(posIdx);

            if (pos != null) {
                if (train.hasRequest() || train.hasProduct()) {
                    StringBuilder status = new StringBuilder();
                    if (train.hasRequest()) {
                        status.append("Pedido");
                    }
                    if (train.hasProduct()) {
                        if (!status.isEmpty()) status.append(" - ");
                        status.append("Produto");
                    }
                    if (status.isEmpty()) {
                        status.append("-");
                    }

                    System.out.printf("T%d (%d,%d) - %d - %c - %s%n",
                            train.getNum(), pos.getX(), pos.getY(),
                            train.getSection(), train.getTrainState(),
                            status.toString());
                } else {
                    System.out.printf("T%d (%d,%d) - %d - %c%n",
                            train.getNum(), pos.getX(), pos.getY(),
                            train.getSection(), train.getTrainState());
                }
            }
        }
    }

    private boolean hasProducersConsumers() {
        for (int t = 0; t < numTracks; t++) {
            if (producers[t] != null || consumers[t] != null) {
                return true;
            }
        }
        return false;
    }

    private void printProducerConsumerState() {
        System.out.println();
        System.out.println("Tracks (Producers and Consumers):");
        for (int t = 0; t < numTracks; t++) {
            if (tracks[t] != null && tracks[t].getSize() > 0) {
                if (producers[t] != null && consumers[t] != null) {
                    System.out.printf("Track %d - Producer (%d/%d) - Consumer (%d/%d)%n",
                            tracks[t].getNum(),
                            producers[t].getCurrentProducts(), producers[t].getCapacity(),
                            consumers[t].getCurrentRequests(), consumers[t].getCapacity());
                }
            }
        }
    }

    public void startSimulation(int waitTime, int fileType) {
        for (int i = 0; i < numTrains; i++) {
            trains[i].initialize(tracks, semaphores, mutex, waitTime, fileType);

            if (fileType == 2) {
                int trackIdx = trains[i].getTrack();
                if (trackIdx >= 0 && trackIdx < numTracks) {
                    trains[i].setProducerConsumer(producers[trackIdx], consumers[trackIdx]);
                }
            }
        }

        for (int i = 0; i < numTrains; i++) {
            trains[i].start();
        }

        if (fileType == 2) {
            for (int t = 0; t < numTracks; t++) {
                if (producers[t] != null) {
                    producers[t].start();
                }
                if (consumers[t] != null) {
                    consumers[t].start();
                }
            }
        }

        try {
            while (true) {
                mutex.acquire();
                try {
                    fillBoard();
                    printState();
                } finally {
                    mutex.release();
                }
                Thread.sleep(waitTime);
            }
        } catch (InterruptedException e) {
            stopSimulation();
        }
    }

    private void stopSimulation() {
        for (int i = 0; i < numTrains; i++) {
            if (trains[i] != null && trains[i].isAlive()) {
                trains[i].interrupt();
            }
        }

        for (int t = 0; t < numTracks; t++) {
            if (producers[t] != null && producers[t].isAlive()) {
                producers[t].stopProducer();
            }
            if (consumers[t] != null && consumers[t].isAlive()) {
                consumers[t].stopConsumer();
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
            System.out.println("Usage: java Simulation <input_file> <wait_time_ms> [file_type]");
            System.exit(1);
        }

        String fileName = args[0];
        int waitTime;
        int fileType = 1;

        try {
            waitTime = Integer.parseInt(args[1]);
            if (waitTime <= 0) {
                throw new NumberFormatException("Wait time must be positive");
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid wait time: " + args[1]);
            System.exit(1);
            return;
        }

        if (args.length == 3) {
            try {
                fileType = Integer.parseInt(args[2]);
                if (fileType != 1 && fileType != 2) {
                    throw new NumberFormatException("File type must be 1 or 2");
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid file type: " + args[2]);
                System.exit(1);
                return;
            }
        }

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
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid data in file: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}