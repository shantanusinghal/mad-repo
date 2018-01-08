package edu.uw.cs.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by shantanusinghal on 17/02/17 @ 12:38 AM.
 * NET-ID: singhal5
 * Campus ID: 9076101956
 */
public class Instance {

    private String classLabel;
    private Attribute classAttribute;
    private Map<Attribute, String> attributeMap = new LinkedHashMap<Attribute, String>();

    Instance(weka.core.Instance instance) {
        this.classLabel = instance.stringValue(instance.classIndex());
        this.classAttribute = new Attribute(instance.attribute(instance.classIndex()));
        // TODO verify class attribute is not being extracted
        for (int i = 0; i < instance.numAttributes() - 1; i++)
            this.attributeMap.put(new Attribute(instance.attribute(i)), instance.stringValue(i));
    }

    public boolean matches(Attribute attribute, String value) {
        return getValue(attribute).equals(value);
    }

    public boolean matches(Map<Attribute, String> partial) {
        for (Map.Entry<Attribute, String> entry : partial.entrySet()) {
            if(!getValue(entry.getKey()).equals(entry.getValue())) return false;
        }
        return true;
    }

    public Map<Attribute, String> findMatch(List<Map<Attribute, String>> partials) {
        for (Map<Attribute, String> partial : partials) if (this.matches(partial)) return partial;
        return Collections.emptyMap();
    }

    public String getClassLabel() {
        return classLabel;
    }

    // TODO handle missing keys
    public String getValue(Attribute attribute) {
        return attribute.equals(classAttribute) ? classLabel : attributeMap.get(attribute);
    }

}
