package org.davidmoten.EDB.model;

import java.math.BigInteger;

public class Entity {
    private String keyword;
    private int[] state;          // For SC table
    private BigInteger bigIntValue; // For SS table
    private Object[] ciphertext;  // For PDB and KDB tables

    public Entity(String keyword) {
        this.keyword = keyword;
    }

    // Constructor for SC table
    public Entity(String keyword, int[] state) {
        this.keyword = keyword;
        this.state = state;
    }

    // Constructor for SS table
    public Entity(String keyword, BigInteger bigIntValue) {
        this.keyword = keyword;
        this.bigIntValue = bigIntValue;
    }

    // Constructor for PDB and KDB tables
    public Entity(String keyword, Object[] ciphertext) {
        this.keyword = keyword;
        this.ciphertext = ciphertext;
    }

    public String getKeyword() {
        return keyword;
    }

    public int[] getState() {
        return state;
    }

    public BigInteger getBigIntegerValue() {
        return bigIntValue;
    }

    public Object[] getCiphertext() {
        return ciphertext;
    }

    // ... additional methods if needed ...
}
