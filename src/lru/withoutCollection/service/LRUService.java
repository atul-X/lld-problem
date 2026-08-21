package lru.withoutCollection.service;

import lru.withoutCollection.CacheUtil;
import lru.withoutCollection.Node;

import java.util.HashMap;
import java.util.Map;

public class LRUService implements Cache {
    private final Map<String, Node> nodeMap = new HashMap<>();
    private final int capacity;
    private final CacheUtil cache;

    public LRUService(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be >= 1");
        }
        this.cache = new CacheUtil();
        this.capacity = capacity;
    }

    @Override
    public synchronized Object get(String key) {
        Node node = nodeMap.get(key);
        if (node == null) {
            throw new IllegalStateException("No such key: " + key);
        }
        cache.remove(node);
        cache.addInHead(node);
        return node.value;
    }

    @Override
    public synchronized void put(String key, Object value) {
        Node existing = nodeMap.get(key);
        if (existing != null) {
            existing.value = value;
            cache.remove(existing);
            cache.addInHead(existing);
            return;
        }
        if (nodeMap.size() >= capacity) {
            Node lru = cache.removeLast();
            nodeMap.remove(lru.key);
        }
        Node node = new Node(key, value);
        nodeMap.put(key, node);
        cache.addInHead(node);
    }

    @Override
    public synchronized Object remove(String key) {
        Node node = nodeMap.remove(key);
        if (node == null) {
            return null;
        }
        cache.remove(node);
        return node.value;
    }
}
