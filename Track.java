public class Track {
    private int number;
    private int[] trackPosition;
    private int[] tracksStops;

    public Track(int number, int[] trackPosition, int[] tracksStops) {
        this.number = number;
        this.trackPosition = trackPosition;
        this.tracksStops = tracksStops;
    }

    public int getNumber() {
        return number;
    }

    public int[] getTrackPosition() {
        return trackPosition;
    }

    public int[] getTracksStops() {
        return tracksStops;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public void setTrackPosition(int[] track_position) {
        this.trackPosition = track_position;
    }

    public void setTracksStops(int[] tracks_stops) {
        this.tracksStops = tracks_stops;
    }
}
