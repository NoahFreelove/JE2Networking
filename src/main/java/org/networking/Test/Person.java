package org.networking.Test;

import org.networking.NetworkingObject;
import org.networking.SyncVar;

import java.io.Serializable;

public class Person implements Serializable, NetworkingObject {
    @SyncVar
    public String name;
    @SyncVar
    public String job;
    @SyncVar
    public int age;

    public Person(String name, String job, int age) {
        this.name = name;
        this.job = job;
        this.age = age;
    }

    @Override
    public String toString(){
        return "Name: " + name + ", Job: " + job + ", Age: " + age;
    }

    @Override
    public void queueNetworkSend() {

    }
}
