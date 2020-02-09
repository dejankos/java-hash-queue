import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LinkedBlockingHashQueueTest {

    @Test
    @DisplayName("should put to queue")
    public void put() throws InterruptedException {
        var queue = new LinkedBlockingHashQueue<Integer>(1);
        queue.put(1);

        assertEquals(1, queue.size());
    }

    @Test
    @DisplayName("should successfully offer to queue")
    public void offerAndAccept() {
        var queue = new LinkedBlockingHashQueue<Integer>(1);
        var accepted = queue.offer(1);

        assertTrue(accepted);
        assertEquals(1, queue.size());
    }

    @Test
    @DisplayName("should be rejected on offer because of capacity restriction")
    public void offerAndReject() {
        var queue = new LinkedBlockingHashQueue<Integer>(1);
        queue.offer(1); // ignore res
        var accepted = queue.offer(2);

        assertFalse(accepted);
        assertEquals(1, queue.size());
    }

    @Test
    @DisplayName("should offer null element and expect NPE")
    public void offerAndNPE() {
        var queue = new LinkedBlockingHashQueue<Integer>(1);
        assertThrows(NullPointerException.class, () -> queue.offer(null));
    }

    @Test
    @DisplayName("should offer and wait until timeout")
    public void offerAndTimeout() throws InterruptedException {
        var queue = new LinkedBlockingHashQueue<Integer>(1);
        queue.offer(1); // ignore res
        var accepted = queue.offer(2, 50L, MILLISECONDS);

        assertFalse(accepted);
        assertEquals(1, queue.size());
    }

    @Test
    @DisplayName("should put/offer same elements while queue remains unchanged")
    public void putSame() throws InterruptedException {
        var queue = new LinkedBlockingHashQueue<Integer>(2);
        queue.put(1);
        queue.put(1);

        assertTrue(queue.offer(1));
        assertEquals(1, queue.size());
    }

    @Test
    @DisplayName("should put and take element")
    public void putAndTake() throws InterruptedException {
        var queue = new LinkedBlockingHashQueue<Integer>(1);
        queue.put(1);

        assertEquals(1, queue.take());
        assertTrue(queue.isEmpty());
    }

    @Test
    @DisplayName("should offer and poll element with timeout")
    public void offerAndPoll() throws InterruptedException {
        var queue = new LinkedBlockingHashQueue<Integer>(1);
        queue.offer(1, 50L, MILLISECONDS);

        assertEquals(1, queue.poll(50L, MILLISECONDS));
        assertTrue(queue.isEmpty());
    }

    @Test
    @DisplayName("should drain to list")
    public void drainToList() throws InterruptedException {
        var queue = new LinkedBlockingHashQueue<Integer>(3);
        queue.put(1);
        queue.put(2);
        queue.put(3);

        var list = new LinkedList<>();
        queue.drainTo(list);

        assertTrue(queue.isEmpty());
        assertEquals(3, list.size());
    }

    @Test
    @DisplayName("should drain to list part of elements")
    public void drainToListPart() throws InterruptedException {
        var queue = new LinkedBlockingHashQueue<Integer>(5);
        queue.put(1);
        queue.put(2);
        queue.put(3);
        queue.put(4);
        queue.put(5);

        var list = new LinkedList<>();
        queue.drainTo(list, 3);

        assertEquals(2, queue.size());
        assertEquals(3, list.size());
    }

    @Test
    @DisplayName("should peek head of the queue")
    public void peek() throws InterruptedException {
        var queue = new LinkedBlockingHashQueue<Integer>(3);
        queue.put(1);
        queue.put(2);
        queue.put(3);

        assertEquals(1, queue.peek());
        assertEquals(3, queue.size());
    }

    @Test
    @DisplayName("should clear queue")
    public void clear() throws InterruptedException {
        var queue = new LinkedBlockingHashQueue<Integer>(3);
        queue.put(1);
        queue.put(2);
        queue.put(3);

        queue.clear();

        assertTrue(queue.isEmpty());
    }

    @Test
    @DisplayName("should contain an element")
    public void contains() throws InterruptedException {
        var queue = new LinkedBlockingHashQueue<Integer>(1);
        queue.put(1);

        assertTrue(queue.contains(1));
        assertEquals(1, queue.size());
    }

    @Test
    @DisplayName("should remove an element")
    public void remove() throws InterruptedException {
        var queue = new LinkedBlockingHashQueue<Integer>(2);
        queue.put(1);
        queue.put(2);

        assertTrue(queue.remove(1));
        assertTrue(queue.contains(2));
        assertEquals(1, queue.size());
    }

    @Test
    @DisplayName("should traverse all elements with iterator")
    public void iter() throws InterruptedException {
        var queue = new LinkedBlockingHashQueue<Integer>(3);
        queue.put(1);
        queue.put(2);
        queue.put(3);

        var iter = queue.iterator();
        var c = 0;
        while (iter.hasNext()) {
            c++;
            iter.next();
        }

        assertEquals(3, c);
    }

    @Test
    @DisplayName("should preserve FIFO order")
    public void fifo() throws InterruptedException {
        var queue = new LinkedBlockingHashQueue<Integer>(3);
        queue.put(1);
        queue.put(2);
        queue.put(3);

        var collect = new LinkedList<Integer>();
        while (queue.size() > 0) {
            collect.add(queue.take());
        }

        assertEquals(1, collect.get(0));
        assertEquals(2, collect.get(1));
        assertEquals(3, collect.get(2));
    }
}
