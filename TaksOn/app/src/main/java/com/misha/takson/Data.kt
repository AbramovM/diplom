package com.misha.takson

class Poezdka {
    private var id: Int
    private var destination: String

    public constructor(id: Int, destination: String) {
        this.id = id
        this.destination = destination;
    }

    public fun getId(): Int {
        return id
    }

    public fun getDestination(): String {
        return destination
    }

    override fun toString(): String {
        return destination
    }
}