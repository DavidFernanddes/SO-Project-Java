public class Track {
    public static final int MAX_POSITIONS = 50;
    public static final int MAX_STOPS = 10;

    private int num;
    private int size;
    private Position[] positions;
    private int numStops;
    private Stop[] stops;

    public Track() {
        this.num = 0;
        this.size = 0;
        this.positions = new Position[MAX_POSITIONS];
        this.numStops = 0;
        this.stops = new Stop[MAX_STOPS];

        for (int i = 0; i < MAX_POSITIONS; i++) {
            positions[i] = new Position();
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
        this.size = Math.max(0, size);
    }

    public Position getPosition(int index) {
        if (index >= 0 && index < MAX_POSITIONS && index < size) {
            return positions[index];
        }
        return null;
    }

    public void setPosition(int index, Position pos) {
        if (index >= 0 && index < MAX_POSITIONS && pos != null) {
            positions[index] = pos;
        }
    }

    public int getNumStops() {
        return numStops;
    }

    public void setNumStops(int numStops) {
        this.numStops = Math.max(0, Math.min(numStops, MAX_STOPS));
    }

    public Stop getStop(int index) {
        if (index >= 0 && index < MAX_STOPS && index < numStops) {
            return stops[index];
        }
        return null;
    }

    public void setStop(int index, Stop stop) {
        if (index >= 0 && index < MAX_STOPS && stop != null) {
            stops[index] = stop;
        }
    }

    /**
     * Encontra o índice da paragem numa determinada posição
     */
    public int findStopIndexAtPosition(int position) {
        for (int s = 0; s < numStops; s++) {
            Stop stop = getStop(s);
            if (stop != null && stop.getStopIndex() == position) {
                return s;
            }
        }
        return -1;
    }

    /**
     * Verifica se uma posição é uma paragem
     */
    public boolean isStopPosition(int position) {
        return findStopIndexAtPosition(position) != -1;
    }

    @Override
    public String toString() {
        return String.format("Track[num=%d, size=%d, stops=%d]", num, size, numStops);
    }
}