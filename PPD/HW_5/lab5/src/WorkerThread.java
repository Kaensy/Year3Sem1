class WorkerThread extends Thread {
    private final BoundedBlockingQueue queue;
    private final FineGrainedList resultList;
    private final int workerId;

    public WorkerThread(int id, BoundedBlockingQueue queue, FineGrainedList resultList) {
        this.workerId = id;
        this.queue = queue;
        this.resultList = resultList;
    }

    @Override
    public void run() {
        try {
            while (true) {
                ScoreEntry entry = queue.take();
                if (entry == null) {
                    // No more entries will be coming
                    break;
                }
                resultList.insert(entry.getId(), entry.getScore(), entry.getCountry());
            }
        } catch (InterruptedException e) {
            System.err.println("Worker " + workerId + " interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    public int getWorkerId() {
        return workerId;
    }
}