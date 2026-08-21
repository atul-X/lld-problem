package lru.withcollection.service;

import lru.withcollection.CacheUtil;

import java.util.HashMap;

public class LRUService implements Cache{
    HashMap<String,Object> kvHashMap=new HashMap<>();
    private int capacity;
    CacheUtil cache;
    private static LRUService lruService=null;

    private LRUService() {
    }

    public  synchronized LRUService getInstance(int capacity) {
        if (lruService==null) {
            if (capacity < 1) {
                throw new IllegalArgumentException();
            }
            cache = new CacheUtil();
            this.capacity = capacity;
            lruService=new LRUService();
        }
        return lruService;
    }

    @Override
    public synchronized Object get(String key) {
        if(kvHashMap.containsKey(key)) {
            Object val = kvHashMap.get(key);
            cache.remove(key);
            cache.addInHead(key);
            return val;
        }else{
            throw new IllegalStateException();
        }
    }

    @Override
    public synchronized void put(String key, Object value) {
        if (kvHashMap.containsKey(key)) {
            kvHashMap.put(key, value);
            cache.remove(key);
            cache.addInHead(key);
        }else{
            if (kvHashMap.size()>=capacity){
                String keyToRemove=cache.removeLast();
                kvHashMap.remove(keyToRemove);
            }
            kvHashMap.put(key,value);
            cache.addInHead(key);
        }
    }

    @Override
    public synchronized Object remove(String key) {
        Object val= kvHashMap.remove(key);
        cache.remove(key);
        return val;
    }
}
