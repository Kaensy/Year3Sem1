import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A thread-safe bounded blocking queue implementation using explicit locks and conditions.
 * This queue has a maximum capacity and blocks when full/empty.
 */
class BoundedBlockingQueue {
    // Underlying queue to store elements
    private final Queue<ScoreEntry> queue;
    // Maximum number of elements the queue can hold
    private final int capacity;
    // Lock for synchronizing access to the queue
    private final ReentrantLock lock;
    // Condition for producers to wait when queue is full
    private final Condition notFull;
    // Condition for consumers to wait when queue is empty
    private final Condition notEmpty;
    // Flag to indicate when all producers have finished
    private boolean isComplete;
    // Counter for active producers
    private int activeProducers;

    /**
     * Creates a new bounded blocking queue with specified capacity and number of producers
     */
    public BoundedBlockingQueue(int capacity, int numProducers) {
        this.capacity = capacity;
        this.queue = new LinkedList<>();
        this.lock = new ReentrantLock();
        this.notFull = lock.newCondition();
        this.notEmpty = lock.newCondition();
        this.isComplete = false;
        this.activeProducers = numProducers;
    }

    /**
     * Puts an element into the queue, blocking if the queue is full
     * Used by producer threads (readers)
     */
    public void put(ScoreEntry entry) throws InterruptedException {
        lock.lock();
        try {
            // Wait while queue is full
            while (queue.size() == capacity) {
                notFull.await();
            }
            // Add the entry and signal consumers
            queue.offer(entry);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Takes an element from the queue, blocking if the queue is empty
     * Returns null if queue is empty and all producers are done
     * Used by consumer threads (workers)
     */
    public ScoreEntry take() throws InterruptedException {
        lock.lock();
        try {
            // Wait while queue is empty and producers are still active
            while (queue.isEmpty()) {
                if (isComplete) {
                    return null; // No more items will be produced
                }
                notEmpty.await();
            }
            // Remove and return an entry, signal producers
            ScoreEntry entry = queue.poll();
            notFull.signal();
            return entry;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Called by a producer when it finishes producing
     * When all producers are done, signals waiting consumers
     */
    public void producerComplete() {
        lock.lock();
        try {
            activeProducers--;
            if (activeProducers == 0) {
                isComplete = true;
                // Wake up all consumers to check completion status
                notEmpty.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Checks if the queue is finished (all producers done and queue empty)
     */
    public boolean isFinished() {
        lock.lock();
        try {
            return isComplete && queue.isEmpty();
        } finally {
            lock.unlock();
        }
    }
}