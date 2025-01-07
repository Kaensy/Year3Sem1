import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

class BoundedBlockingQueue {
    private final Queue<ScoreEntry> queue;
    private final int capacity;
    private final ReentrantLock lock;
    private final Condition notFull;
    private final Condition notEmpty;
    private boolean isComplete;
    private int activeProducers;

    public BoundedBlockingQueue(int capacity, int numProducers) {
        this.capacity = capacity;
        this.queue = new LinkedList<>();
        this.lock = new ReentrantLock();
        this.notFull = lock.newCondition();
        this.notEmpty = lock.newCondition();
        this.isComplete = false;
        this.activeProducers = numProducers;
    }

    public void put(ScoreEntry entry) throws InterruptedException {
        lock.lock();
        try {
            while (queue.size() == capacity) {
                notFull.await();
            }
            queue.offer(entry);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public ScoreEntry take() throws InterruptedException {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                if (isComplete) {
                    return null;
                }
                notEmpty.await();
            }
            ScoreEntry entry = queue.poll();
            notFull.signal();
            return entry;
        } finally {
            lock.unlock();
        }
    }

    public void producerComplete() {
        lock.lock();
        try {
            activeProducers--;
            if (activeProducers == 0) {
                isComplete = true;
                notEmpty.signalAll(); // Wake up all waiting consumers
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean isFinished() {
        lock.lock();
        try {
            return isComplete && queue.isEmpty();
        } finally {
            lock.unlock();
        }
    }
}