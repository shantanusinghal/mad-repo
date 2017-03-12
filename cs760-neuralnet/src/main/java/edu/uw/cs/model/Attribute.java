package edu.uw.cs.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by shantanusinghal on 17/02/17 @ 12:40 AM.
 * NET-ID: singhal5
 * Campus ID: 9076101956
 */
public class Attribute {

    private String name;
    private boolean isClass;
    private List<String> values = new ArrayList<String>();

    Attribute(weka.core.Attribute attribute) {
        this(attribute, false);
    }

    Attribute(weka.core.Attribute attribute, boolean isClass) {
        this.isClass = isClass;
        this.name = attribute.name();
        for (int i = 0; i < attribute.numValues(); i++) this.values.add(attribute.value(i));
    }

    public static Pair pair(Attribute a1, Attribute a2) {
        return new Pair(a1, a2);
    }

    public String getName() {
        return name;
    }

    public List<String> getValues() {
        return values;
    }

    public boolean isClass() {
        return isClass;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    // TODO give this a second thought
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Attribute attribute = (Attribute) o;
        return name.equals(attribute.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    // Pairs are symmetric i.e. 'from' and 'to' are interchangeable
    public static class Pair {
        private Attribute to;
        private Attribute from;

        Pair(Attribute from, Attribute to) {
            this.to = to;
            this.from = from;
        }

        public Attribute getFrom() {
            return from;
        }

        public Attribute getTo() {
            return to;
        }

        @Override
        public int hashCode() {
            int result = from.hashCode();
            result = 31 * result + to.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pair pair = (Pair) o;
            return eq(pair.from, pair.to) || eq(pair.to, pair.from);
        }

        private boolean eq(Attribute o1, Attribute o2) {
            return from.equals(o1) && to.equals(o2);
        }
    }

}
