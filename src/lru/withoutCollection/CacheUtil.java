package lru.withoutCollection;

public class CacheUtil {
    private final Node head = new Node(null, null);
    private final Node tail = new Node(null, null);

    public CacheUtil() {
        head.next = tail;
        tail.prev = head;
    }

    public void addInHead(Node node) {
        node.next=head.next;
        node.prev=head;
        head.next.prev=node;
        head.next=node;

    }

    public void remove(Node node) {
        node.prev.next=node.next;
        node.next.prev=node.prev;
    }

    public Node removeLast() {
        Node lastNode=tail.prev;
        remove(lastNode);
        return lastNode;
    }
}
