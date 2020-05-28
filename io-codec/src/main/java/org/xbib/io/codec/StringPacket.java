package org.xbib.io.codec;

/**
 *
 */
public class StringPacket implements Packet<String> {

    private String name;
    private String string;

    public StringPacket() {
    }

    public StringPacket name(String name) {
        this.name = name;
        return this;
    }

    public String name() {
        return name;
    }

    public StringPacket packet(String string) {
        this.string = string;
        return this;
    }

    public String packet() {
        return string;
    }

    @Override
    public String toString() {
        return string;
    }
}
