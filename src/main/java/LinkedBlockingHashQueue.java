import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A hash based thread safe implementation of linked blocking queue.
 * This queue orders elements FIFO (first-in-first-out).
 *
 * @param <E> element tye
 */
public class LinkedBlockingHashQueue<E> extends AbstractQueue<E>
        implements BlockingQueue<E>, java.io.Serializable {
    private static final long serialVersionUID = 9122939650203291786L;
    /**
     * Underlying hash table
     */
    private final LinkedHashSet<E> hashSet;
    /**
     * The capacity bound, or Integer.MAX_VALUE if none
     */
    private final int capacity;
    /**
     * Current number of elements
     */
    private final AtomicInteger count = new AtomicInteger();
    /**
     * Queue lock used by put, take, poll etc.
     */
    private final ReentrantLock queueLock = new ReentrantLock();
    /**
     * Wait queue for waiting takes
     */
    private final Condition notEmpty = queueLock.newCondition();
    /**
     * Wait queue for waiting puts
     */
    private final Condition notFull = queueLock.newCondition();

    public LinkedBlockingHashQueue() {
        this(Integer.MAX_VALUE);
    }

    public LinkedBlockingHashQueue(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException();
        this.hashSet = new LinkedHashSet<>();
        this.capacity = capacity;
    }

    public Iterator<E> iterator() {
        return new HashQueueIter();
    }

    public int size() {
        return count.get();
    }

    public void put(E e) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        queueLock.lockInterruptibly();
        try {
            while (size() == capacity) {
                notFull.await();
            }
            enqueue(e);
        } finally {
            queueLock.unlock();
        }
    }

    public boolean offer(E e) {
        if (e == null) throw new NullPointerException();
        if (size() == capacity) {
            return false;
        }
        queueLock.lock();
        try {
            if (size() == capacity) {
                return false;
            }
            enqueue(e);
        } finally {
            queueLock.unlock();
        }

        return true;
    }

    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        long nanos = unit.toNanos(timeout);
        queueLock.lockInterruptibly();
        try {
            while (size() == capacity) {
                if (nanos <= 0L) {
                    return false;
                }
                nanos = notFull.awaitNanos(nanos);
            }
            enqueue(e);
        } finally {
            queueLock.unlock();
        }

        return true;
    }

    public E take() throws InterruptedException {
        queueLock.lockInterruptibly();
        try {
            while (size() == 0) {
                notEmpty.await();
            }
            final E head = dequeue();
            signalAfterTake(count.getAndDecrement());

            return head;
        } finally {
            queueLock.unlock();
        }
    }

    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        queueLock.lockInterruptibly();
        long nanos = unit.toNanos(timeout);
        try {
            while (size() == 0) {
                if (nanos <= 0L) {
                    return null;
                }
                nanos = notEmpty.awaitNanos(nanos);
            }
            final E head = dequeue();
            signalAfterTake(count.getAndDecrement());

            return head;
        } finally {
            queueLock.unlock();
        }
    }

    public E poll() {
        queueLock.lock();
        try {
            if (size() == 0) {
                return null;
            }
            final E head = dequeue();
            signalAfterTake(count.getAndDecrement());

            return head;
        } finally {
            queueLock.unlock();
        }
    }

    public int remainingCapacity() {
        return capacity - size();
    }

    public int drainTo(Collection<? super E> c) {
        return drainTo(c, capacity);
    }

    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null) throw new NullPointerException();
        if (c == this) throw new IllegalArgumentException();

        queueLock.lock();
        try {
            int n = Math.min(maxElements, size());
            int i = 0;
            while (i < n) {
                E e = dequeue();
                c.add(e);
                i++;
            }
            if (i > 0 && count.getAndAdd(-i) == capacity) {
                notFull.signal();
            }

            return n;
        } finally {
            queueLock.unlock();
        }
    }

    public E peek() {
        if (size() == 0) {
            return null;
        }
        queueLock.lock();
        try {
            if (size() == 0) {
                return null;
            }

            return head();
        } finally {
            queueLock.unlock();
        }
    }

    @Override
    public void clear() {
        queueLock.lock();
        try {
            hashSet.clear();
            final int c = count.getAndSet(0);
            if (c == capacity) {
                notFull.signal();
            }
        } finally {
            queueLock.unlock();
        }
    }

    @Override
    public boolean contains(Object o) {
        if (size() == 0) {
            return false;
        }
        queueLock.lock();
        try {
            return hashSet.contains(o);
        } finally {
            queueLock.unlock();
        }
    }

    @Override
    public boolean remove(Object o) {
        if (size() == 0) {
            return false;
        }
        queueLock.lock();
        try {
            if (size() == 0) {
                return false;
            }
            final boolean r = hashSet.remove(o);
            final int c = count.getAndDecrement();
            if (c == capacity) {
                notFull.signal();
            }

            return r;
        } finally {
            queueLock.unlock();
        }
    }

    private E head() {
        return hashSet.iterator().next();
    }

    private E dequeue() {
        final E head = head();
        hashSet.remove(head);


        return head;
    }

    private void enqueue(E e) {
        boolean first = hashSet.add(e);
        if (!first) {
            //e was not first but already exists, this will leave the queue unchanged
            return;
        }

        signalAfterPut(count.getAndIncrement());
    }

    private void signalAfterPut(final int c) {
        if (c + 1 < capacity) {
            notFull.signal();
        }
        if (c == 0) {
            notEmpty.signal();
        }
    }

    private void signalAfterTake(final int c) {
        if (c > 1) {
            notEmpty.signal();
        }
        if (c == capacity) {
            notFull.signal();
        }
    }

    private class HashQueueIter implements Iterator<E> {
        Iterator<E> iter;

        HashQueueIter() {
            queueLock.lock();
            try {
                iter = hashSet.iterator();
            } finally {
                queueLock.unlock();
            }
        }

        @Override
        public boolean hasNext() {
            queueLock.lock();
            try {
                return iter.hasNext();
            } finally {
                queueLock.unlock();
            }
        }

        @Override
        public E next() {
            queueLock.lock();
            try {
                return iter.next();
            } finally {
                queueLock.unlock();
            }
        }
    }
}
