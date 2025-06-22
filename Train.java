import java.util.concurrent.Semaphore;

public class Train extends Thread {
    private int num;
    private int track;
    private int position;
    private int speed;
    private int counter;
    private char state;
    private int section;
    private int lastStop;
    private int nextStop;

    // References to shared data
    private Track[] tracks;
    private Semaphore[] semaphores;
    private Semaphore mutex;
    private int waitTime;
    private int fileType;

    public Train() {
        this.num = 0;
        this.track = 0;
        this.position = 0;
        this.speed = 1;
        this.counter = 0;
        this.state = 'M';
        this.section = 0;
        this.lastStop = -1;
        this.nextStop = -1;
    }

    public Train(int num, int track, int position, int speed) {
        this();
        this.num = num;
        this.track = track;
        this.position = position;
        this.speed = speed;
    }

    public void initialize(Track[] tracks, Semaphore[] semaphores, Semaphore mutex, int waitTime, int fileType) {
        this.tracks = tracks;
        this.semaphores = semaphores;
        this.mutex = mutex;
        this.waitTime = waitTime;
        this.fileType = fileType;

        if (fileType == 2) {
            findTrainStops();
        } else {
            initializeSimpleMode();
        }
    }

    @Override
    public void run() {
        if (fileType == 2) {
            trainProcessWithStops();
        } else {
            trainProcessSimple();
        }
    }

    private void initializeSection() {
        Track currentTrack = tracks[track];

        // Encontrar a seção atual baseada na posição
        int currentSection = 0;
        for (int s = 0; s < currentTrack.getNumStops(); s++) {
            Stop stop = currentTrack.getStop(s);
            if (stop != null && stop.getStopIndex() <= position) {
                currentSection = s;
            }
        }

        this.section = currentSection;

        // Se não estiver numa paragem, adquirir semáforo da seção
        boolean atStop = false;
        for (int s = 0; s < currentTrack.getNumStops(); s++) {
            Stop stop = currentTrack.getStop(s);
            if (stop != null && stop.getStopIndex() == position) {
                state = 'S';
                atStop = true;
                break;
            }
        }

        if (!atStop && currentSection < currentTrack.getNumStops()) {
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

    private void trainProcessSimple() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(waitTime);

                try {
                    mutex.acquire();
                    counter++;
                    if (counter % speed == 0) {

                        Track currentTrack = tracks[track];
                        int nextPos = (position + 1) % currentTrack.getSize();

                        // Verificar se a próxima posição é uma paragem
                        boolean nextIsStop = false;
                        int nextStopIndex = -1;
                        for (int s = 0; s < currentTrack.getNumStops(); s++) {
                            Stop stop = currentTrack.getStop(s);
                            if (stop != null && stop.getStopIndex() == nextPos) {
                                nextIsStop = true;
                                nextStopIndex = s;
                                break;
                            }
                        }

                        // Verificar se está atualmente numa paragem
                        boolean currentlyAtStop = false;
                        int currentStopIndex = -1;
                        for (int s = 0; s < currentTrack.getNumStops(); s++) {
                            Stop stop = currentTrack.getStop(s);
                            if (stop != null && stop.getStopIndex() == position) {
                                currentlyAtStop = true;
                                currentStopIndex = s;
                                break;
                            }
                        }

                        if (currentlyAtStop) {
                            // Está numa paragem - tentar sair
                            state = 'W';

                            // Encontrar próxima seção
                            int nextSectionIndex = (currentStopIndex + 1) % currentTrack.getNumStops();
                            Stop nextSection = currentTrack.getStop(nextSectionIndex);

                            mutex.release();

                            // Tentar adquirir semáforo da próxima seção
                            if (semaphores[nextSection.getSemaphoreIndex()].tryAcquire()) {
                                mutex.acquire();

                                // Conseguiu - mover para fora da paragem
                                position = nextPos;
                                state = 'M';
                                section = nextSectionIndex;

                                // Libertar capacidade da paragem
                                currentTrack.getStop(currentStopIndex).getCapacitySemaphore().release();
                            } else {
                                // Não conseguiu - ficar à espera
                                mutex.acquire();
                                state = 'W';
                            }

                        } else if (nextIsStop) {
                            // Próxima posição é paragem - tentar entrar
                            state = 'W';
                            Stop nextStop = currentTrack.getStop(nextStopIndex);

                            mutex.release();

                            // Tentar adquirir capacidade da paragem
                            if (nextStop.getCapacitySemaphore().tryAcquire()) {
                                mutex.acquire();

                                // Conseguiu - mover para a paragem
                                position = nextPos;
                                state = 'S';

                                // Libertar semáforo da seção atual
                                if (section >= 0 && section < currentTrack.getNumStops()) {
                                    Stop currentSection = currentTrack.getStop(section);
                                    semaphores[currentSection.getSemaphoreIndex()].release();
                                }

                                section = nextStopIndex;
                            } else {
                                // Não conseguiu - ficar à espera
                                mutex.acquire();
                                state = 'W';
                            }

                        } else {
                            // Movimento normal na seção
                            position = nextPos;
                            state = 'M';
                        }
                    }

                    if (mutex.availablePermits() == 0) {
                        mutex.release();
                    }

                } catch (InterruptedException e) {
                    if (mutex.availablePermits() == 0) {
                        mutex.release();
                    }
                    Thread.currentThread().interrupt();
                    break;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void trainProcessWithStops() {
        Track currentTrack = tracks[track];

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep((waitTime * speed) / 1000);

                try {
                    mutex.acquire();
                    moveTrainStep(currentTrack);
                } finally {
                    if (mutex.availablePermits() == 0) {
                        mutex.release();
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void moveTrainStep(Track currentTrack) throws InterruptedException {
        // Verificar se estamos atualmente numa paragem
        if (isAtStop(currentTrack)) {
            // Estamos numa paragem, tentar sair
            state = 'W'; // À espera para sair da paragem

            int nextSemIdx = currentTrack.getStop(nextStop).getSemaphoreIndex();

            // Libertar mutex antes de tentar adquirir semáforo da seção
            mutex.release();

            // Tentar adquirir próxima seção (bloqueia se estiver ocupada)
            semaphores[nextSemIdx].acquire();

            // Re-adquirir mutex para atualizar estado
            mutex.acquire();

            // Conseguiu adquirir próxima seção, sair da paragem
            state = 'M';
            position++;
            if (position >= currentTrack.getSize()) {
                position = 0;
            }

            // Libertar capacidade da paragem
            currentTrack.getStop(section).getCapacitySemaphore().release();

            // Atualizar tracking da seção
            section = nextStop;
            nextStop = (nextStop + 1) % currentTrack.getNumStops();

        } else {
            // Verificar se a próxima posição é uma paragem
            int nextPos = (position + 1) % currentTrack.getSize();
            boolean nextIsStop = false;
            int stopIndex = -1;

            for (int s = 0; s < currentTrack.getNumStops(); s++) {
                if (currentTrack.getStop(s).getStopIndex() == nextPos) {
                    nextIsStop = true;
                    stopIndex = s;
                    break;
                }
            }

            if (nextIsStop) {
                // Próxima posição é uma paragem, tentar entrar
                state = 'W'; // À espera para entrar na paragem

                Stop stop = currentTrack.getStop(stopIndex);

                // Libertar mutex antes de tentar adquirir capacidade da paragem
                mutex.release();

                // Tentar adquirir capacidade da paragem (bloqueia se estiver cheia)
                stop.getCapacitySemaphore().acquire();

                // Re-adquirir mutex para atualizar estado
                mutex.acquire();

                // Conseguiu adquirir capacidade da paragem, mover para a paragem
                state = 'S';
                position = nextPos;

                // Libertar semáforo da seção anterior
                if (section >= 0 && section < currentTrack.getNumStops()) {
                    int prevSemIdx = currentTrack.getStop(section).getSemaphoreIndex();
                    semaphores[prevSemIdx].release();
                }

                // Atualizar tracking da seção
                section = stopIndex;

            } else {
                // Movimento normal dentro da seção
                state = 'M';
                position++;
                if (position >= currentTrack.getSize()) {
                    position = 0;
                }
            }
        }
    }

    private boolean isAtStop(Track currentTrack) {
        for (int s = 0; s < currentTrack.getNumStops(); s++) {
            if (currentTrack.getStop(s).getStopIndex() == position) {
                return true;
            }
        }
        return false;
    }

    private void findTrainStops() {
        Track currentTrack = tracks[track];
        int pos = position;
        int lastStopIdx = -1;
        int nextStopIdx = -1;

        // Encontrar paragem atual ou última passada
        for (int s = 0; s < currentTrack.getNumStops(); s++) {
            if (currentTrack.getStop(s).getStopIndex() <= pos) {
                if (lastStopIdx == -1 ||
                        currentTrack.getStop(s).getStopIndex() > currentTrack.getStop(lastStopIdx).getStopIndex()) {
                    lastStopIdx = s;
                }
            }
        }

        if (lastStopIdx == -1) {
            lastStopIdx = currentTrack.getNumStops() - 1;
        }

        // Encontrar próxima paragem
        for (int s = 0; s < currentTrack.getNumStops(); s++) {
            if (currentTrack.getStop(s).getStopIndex() > pos) {
                if (nextStopIdx == -1 ||
                        currentTrack.getStop(s).getStopIndex() < currentTrack.getStop(nextStopIdx).getStopIndex()) {
                    nextStopIdx = s;
                }
            }
        }

        if (nextStopIdx == -1) {
            nextStopIdx = 0;
        }

        lastStop = lastStopIdx;
        nextStop = nextStopIdx;
        section = lastStopIdx;

        // Verificar se está atualmente numa paragem
        boolean atStop = false;
        for (int s = 0; s < currentTrack.getNumStops(); s++) {
            if (currentTrack.getStop(s).getStopIndex() == pos) {
                state = 'S';
                section = s;
                atStop = true;
                break;
            }
        }

        // Se não estiver numa paragem, adquirir semáforo da seção atual
        if (!atStop && section >= 0 && section < currentTrack.getNumStops()) {
            try {
                int semIdx = currentTrack.getStop(section).getSemaphoreIndex();
                semaphores[semIdx].acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void initializeSimpleMode() {
        Track currentTrack = tracks[track];

        // Encontrar em que seção o comboio está
        int currentSection = 0;
        for (int s = 0; s < currentTrack.getNumStops(); s++) {
            Stop stop = currentTrack.getStop(s);
            if (stop != null && stop.getStopIndex() <= position) {
                currentSection = s;
            }
        }

        this.section = currentSection;

        // Verificar se está numa paragem
        boolean atStop = false;
        for (int s = 0; s < currentTrack.getNumStops(); s++) {
            Stop stop = currentTrack.getStop(s);
            if (stop != null && stop.getStopIndex() == position) {
                state = 'S';
                atStop = true;
                break;
            }
        }

        // Se não está numa paragem, adquirir semáforo da seção atual
        if (!atStop && currentSection < currentTrack.getNumStops()) {
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

    // Getters e setters synchronized
    public synchronized int getNum() { return num; }
    public synchronized void setNum(int num) { this.num = num; }

    public synchronized int getTrack() { return track; }
    public synchronized void setTrack(int track) { this.track = track; }

    public synchronized int getPosition() { return position; }
    public synchronized void setPosition(int position) { this.position = position; }

    public synchronized int getSpeed() { return speed; }
    public synchronized void setSpeed(int speed) { this.speed = speed; }

    public synchronized int getCounter() { return counter; }
    public synchronized void setCounter(int counter) { this.counter = counter; }

    public synchronized char getTrainState() { return state; }
    public synchronized void setTrainState(char state) { this.state = state; }

    public synchronized int getSection() { return section; }
    public synchronized void setSection(int section) { this.section = section; }

    public synchronized int getLastStop() { return lastStop; }
    public synchronized void setLastStop(int lastStop) { this.lastStop = lastStop; }

    public synchronized int getNextStop() { return nextStop; }
    public synchronized void setNextStop(int nextStop) { this.nextStop = nextStop; }
}