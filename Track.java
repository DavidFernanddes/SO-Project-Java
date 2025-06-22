public class Track {
    public static final int MAX_POSITIONS = 50;
    public static final int MAX_STOPS = 10;

    private int num;
    private int size;
    private Position[] position;
    private int numStops;
    private Stop[] stops;

    public Track() {
        this.num = 0;
        this.size = 0;
        this.position = new Position[MAX_POSITIONS];
        this.numStops = 0;
        this.stops = new Stop[MAX_STOPS];

        // Initialize arrays
        for (int i = 0; i < MAX_POSITIONS; i++) {
            position[i] = new Position();
        }
        for (int i = 0; i < MAX_STOPS; i++) {
            stops[i] = new Stop();
        }
    }

    public Track(int num) {
        this();
        this.num = num;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public Position[] getPosition() {
        return position;
    }

    public Position getPosition(int index) {
        if (index >= 0 && index < MAX_POSITIONS) {
            return position[index];
        }
        return null;
    }

    public void setPosition(int index, Position pos) {
        if (index >= 0 && index < MAX_POSITIONS) {
            position[index] = pos;
        }
    }

    public int getNumStops() {
        return numStops;
    }

    public void setNumStops(int numStops) {
        this.numStops = numStops;
    }

    public Stop[] getStops() {
        return stops;
    }

    public Stop getStop(int index) {
        if (index >= 0 && index < MAX_STOPS) {
            return stops[index];
        }
        return null;
    }

    public void setStop(int index, Stop stop) {
        if (index >= 0 && index < MAX_STOPS) {
            stops[index] = stop;
        }
    }
}