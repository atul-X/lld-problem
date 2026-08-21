package lru.withcollection.service;

public interface Cache {
    Object get(String key);
    void put(String key,Object value);
    Object remove(String key);
}
