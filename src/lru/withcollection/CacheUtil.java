package lru.withcollection;

import java.util.LinkedList;

public class CacheUtil {
    LinkedList<String> objectLinkedList=new LinkedList();

    public void  getLast(){
        objectLinkedList.getLast();
    }
    public String removeLast(){
        return objectLinkedList.removeLast();
    }
    public void addInHead(String val){
        objectLinkedList.addFirst(val);
    }
    public void remove(String val){
        objectLinkedList.remove(val);
    }
}
