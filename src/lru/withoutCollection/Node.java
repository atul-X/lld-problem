package lru.withoutCollection;

public class Node {
    public String key;
    public Object value;
    public Node prev;
    public Node next;

    public Node(String key, Object value) {
        this.key = key;
        this.value = value;
    }
}
