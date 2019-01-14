package com.example.dots.model;

/**
 * the class is used by Wave algorithm
 */

public class Node {
   public short distance;// represents distance from source
   public static final short IMPASSABLE = Short.MAX_VALUE; // wave can't pass node with distance
   public static final short PASSABLE = Short.MAX_VALUE-1; // wave can pass this node

    public Node() {
        this.distance = PASSABLE;
    }


}
