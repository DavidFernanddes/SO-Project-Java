public class Train {
    private int trainNumber;
    private int trackIndex;
    private int positionIndex;
    private int frequency;
    private char state;
    private int sectionIndex;
    private int turnCounter;

    public Train(int trainNumber, int trackIndex, int positionIndex, int frequency, char state, int sectionIndex) {
        this.trainNumber = trainNumber;
        this.trackIndex = trackIndex;
        this.positionIndex = positionIndex;
        this.frequency = frequency;
        this.state = state;
        this.sectionIndex = sectionIndex;
        this.turnCounter = 0;
    }

    public int getTrainNumber() {
        return trainNumber;
    }

    public int getTrackIndex() {
        return trackIndex;
    }

    public int getPositionIndex() {
        return positionIndex;
    }

    public int getFrequency() {
        return frequency;
    }

    public char getState() {
        return state;
    }

    public int getSectionIndex() {
        return sectionIndex;
    }

    public int getTurnCounter() {
        return turnCounter;
    }

    public void setTrainNumber(int trainNumber) {
        this.trainNumber = trainNumber;
    }

    public void setTrackIndex(int trackIndex) {
        this.trackIndex = trackIndex;
    }

    public void setPositionIndex(int positionIndex) {
        this.positionIndex = positionIndex;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public void setState(char state) {
        this.state = state;
    }

    public void setSectionIndex(int sectionIndex) {
        this.sectionIndex = sectionIndex;
    }

    public void setTurnCounter(int turnCounter) {
        this.turnCounter = turnCounter;
    }


}
