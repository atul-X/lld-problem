package lru.withoutCollection;

import lru.withoutCollection.service.LRUService;

public class Simulation {
    public static void main(String[] args) {
        LRUService cache = new LRUService(2);

        cache.put("A", 1);
        cache.put("B", 2);
        System.out.println("get A -> " + cache.get("A")); // touches A, order: A, B

        cache.put("C", 3); // evicts B (least recently used)
        System.out.println("get A -> " + cache.get("A"));
        System.out.println("get C -> " + cache.get("C"));
        try {
            cache.get("B");
        } catch (IllegalStateException e) {
            System.out.println("get B -> evicted as expected: " + e.getMessage());
        }

        cache.put("A", 99); // update existing key at full capacity, no eviction
        System.out.println("get A -> " + cache.get("A"));
        System.out.println("get C -> " + cache.get("C"));
    }
}
