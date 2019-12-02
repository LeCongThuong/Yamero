package helpers;

import java.util.LinkedList;
import java.util.Queue;

/**
 * QueueThread class implements a thread running in background and communicating with its parent through a queue.
 *
 * After starting, QueueThread will be waiting on its queue,
 *      when a new data is pushed into the queue by its parent (by calling `pushData`),
 *      QueueThread will execute `onQueue` method and do its task (`onQueue` method MUST BE implemented when defining a QueueThread object) .
 *
 * When parent thread has already pushed the whole needed data into QueueThread,
 *      it has to call `kill` method so as QueueThread would stop after finishing its task.
 *
 * Otherwise, parent thread can call `kill(true)` to stop QueueThread immediately.
 */
public abstract class QueueThread extends Thread {
    private final Queue<Object> mQueue = new LinkedList<>();

    /**
     * An boolean to tell the Thread that whether it could stop after fishing tasks on the pushed data into mQueue
     *      or have to wait for the next data.
     */
    private boolean stop = false;

    public void pushData(Object object) {
        synchronized (this.mQueue) {
            if (this.stop) return;
            this.mQueue.add(object);
        };
    }

    public Object getData() {
        synchronized (this.mQueue) {
            if (this.mQueue.isEmpty()) return null;
            return this.mQueue.remove();
        }
    }

    private boolean beContinue() {
        synchronized (this.mQueue) {
            if (!this.mQueue.isEmpty()) return true;
            return !this.stop;
        }
    }

    public void run() {
        System.out.println("Run thread " + this.getName());
        while (this.beContinue()) {
            synchronized (this.mQueue) {
                if (this.mQueue.isEmpty()) continue;
                onQueue();
            }
        }
    }

    public void onQueue() {}

    public void kill() { this.stop = true; }

    public void kill(boolean force) {
        if (force) {
            synchronized (this.mQueue) {
                this.mQueue.clear();
            }
        }
        this.stop = true;
    }
}
