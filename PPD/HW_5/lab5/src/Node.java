import java.util.concurrent.locks.ReentrantLock;

class Node {
    ScoreEntry data;
    Node next;
    final ReentrantLock lock;

    // Constructor for regular nodes
    public Node(ScoreEntry data) {
        this.data = data;
        this.next = null;
        this.lock = new ReentrantLock();
    }

    // Constructor for sentinel nodes (dummy head/tail)
    public Node() {
        this.data = null;
        this.next = null;
        this.lock = new ReentrantLock();
    }

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }

    public boolean isLocked() {
        return lock.isLocked();
    }
}