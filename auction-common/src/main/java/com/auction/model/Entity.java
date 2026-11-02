package com.auction.model;

import java.io.Serializable;

/**
 * Lớp cơ sở cho toàn bộ thực thể trong project
 */

public abstract class Entity implements Serializable{
    private static final long serialVersionUID = 1L;
    protected Integer id;

    public Entity() {}
    public Entity(Integer id) {
        this.id = id;
    }

    public Integer getId() {return id;}
    public void setId(Integer id) {this.id = id;}

}
