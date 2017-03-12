package edu.uw.cs.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by shantanusinghal on 17/02/17 @ 12:38 AM.
 * NET-ID: singhal5
 * Campus ID: 9076101956
 */
public class Instance {

    private final int index;
    private String classLabel;
    private int classValueIndex;
    private Attribute classAttribute;
    private Map<Attribute, Double> attributeMap = new LinkedHashMap<Attribute, Double>();

    Instance(int index, weka.core.Instance instance) {
        this.index = index;
        this.classLabel = instance.stringValue(instance.classIndex());
        this.classValueIndex = instance.attribute(instance.classIndex()).indexOfValue(classLabel);
        this.classAttribute = new Attribute(instance.attribute(instance.classIndex()));
        for (int i = 0; i < instance.numAttributes() - 1; i++)
            this.attributeMap.put(new Attribute(instance.attribute(i)), instance.value(i));
    }

    public int getClassValueIndex() {
        return classValueIndex;
    }

    public String getClassLabel() {
        return classLabel;
    }

    public int getIndex() {
        return index;
    }

    public Double getValue(Attribute attribute) {
        if(attribute.equals(classAttribute)) throw new IllegalAccessError("Use getClassLabel to get class attribute value");
        return attributeMap.get(attribute);
    }

    @Override
    public String toString() {
        return "Instance{" + getIndex() + " : " + getClassLabel() + "}";
    }

}
