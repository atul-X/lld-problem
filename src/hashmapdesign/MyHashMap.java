package hashmapdesign;

import java.util.Objects;

public class MyHashMap<K, V> {

    private static final int DEFAULT_CAPACITY = 16;
    private static final float LOAD_FACTOR = 0.75f;

    private Entry<K, V>[] buckets;
    private int size;
    private int threshold;

    public MyHashMap() {
        this(DEFAULT_CAPACITY);
    }

    public MyHashMap(int capacity) {
        buckets = (Entry<K, V>[]) new Entry[capacity];
        threshold = (int) (capacity * LOAD_FACTOR);
    }

    // Average O(1)
    public void put(K key, V value) {

        int hash = hash(key);
        int index = getIndex(hash);

        Entry<K, V> current = buckets[index];

        // Key already exists
        while (current != null) {

            if (current.hash == hash &&
                    Objects.equals(current.key, key)) {

                current.value = value;
                return;
            }

            current = current.next;
        }

        // Add new entry
        Entry<K, V> newEntry =
                new Entry<>(key, value, hash);

        newEntry.next = buckets[index];
        buckets[index] = newEntry;

        size++;

        if (size >= threshold) {
            resize();
        }
    }

    // Average O(1)
    public V get(K key) {

        int hash = hash(key);
        int index = getIndex(hash);

        Entry<K, V> current = buckets[index];

        while (current != null) {

            if (current.hash == hash &&
                    Objects.equals(current.key, key)) {

                return current.value;
            }

            current = current.next;
        }

        return null;
    }

    // Average O(1)
    public V remove(K key) {

        int hash = hash(key);
        int index = getIndex(hash);

        Entry<K, V> current = buckets[index];
        Entry<K, V> previous = null;

        while (current != null) {

            if (current.hash == hash &&
                    Objects.equals(current.key, key)) {

                if (previous == null) {
                    // Removing first node
                    buckets[index] = current.next;
                } else {
                    // Removing middle/last node
                    previous.next = current.next;
                }

                size--;

                return current.value;
            }

            previous = current;
            current = current.next;
        }

        return null;
    }

    public boolean containsKey(K key) {
        return get(key) != null;
    }

    public int size() {
        return size;
    }

    private int hash(K key) {

        if (key == null) {
            return 0;
        }

        int hash = key.hashCode();

        // Spread high bits into low bits
        return hash ^ (hash >>> 16);
    }

    private int getIndex(int hash) {
        return (buckets.length - 1) & hash;
    }

    private void resize() {

        Entry<K, V>[] oldBuckets = buckets;

        int newCapacity = oldBuckets.length * 2;

        Entry<K, V>[] newBuckets =
                (Entry<K, V>[]) new Entry[newCapacity];

        buckets = newBuckets;

        threshold = (int) (newCapacity * LOAD_FACTOR);

        // Rehash all entries
        for (Entry<K, V> head : oldBuckets) {

            Entry<K, V> current = head;

            while (current != null) {

                Entry<K, V> next = current.next;

                int newIndex =
                        (newCapacity - 1) & current.hash;

                current.next = newBuckets[newIndex];
                newBuckets[newIndex] = current;

                current = next;
            }
        }
    }

    private static class Entry<K, V> {

        K key;
        V value;
        int hash;

        Entry<K, V> next;

        Entry(K key, V value, int hash) {
            this.key = key;
            this.value = value;
            this.hash = hash;
        }
    }
}
