package edu.uw.cs.model;

import weka.core.Instances;
import weka.core.converters.ArffLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by shantanusinghal on 17/02/17 @ 12:37 AM.
 * NET-ID: singhal5
 * Campus ID: 9076101956
 */
public class Dataset {

    private Attribute classAttribute;
    private List<Instance> instances = new ArrayList<Instance>();
    private List<Attribute> attributes = new ArrayList<Attribute>();
    private List<String> classValues = new ArrayList<String>();

    private Dataset(File file) throws IOException {
        // read arff file from reader
        BufferedReader reader = new BufferedReader(new FileReader(file));
        ArffLoader.ArffReader arff = new ArffLoader.ArffReader(reader);
        Instances wekaData = arff.getData();

        // set the last attribute as the class attribute
        wekaData.setClassIndex(wekaData.numAttributes() - 1);

        // perform sanity checks
        if(!wekaData.classAttribute().isNominal() || wekaData.classAttribute().numValues() != 2)
            throw new IllegalStateException("This is a binary classification problem!!");

        extractAttributesFrom(wekaData);
        extractInstancesFrom(wekaData);
        extractClassValuesFrom(wekaData);
    }

    public static Dataset readFrom(File file) throws IOException {
        return new Dataset(file);
    }

    public List<Instance> getInstances() {
        return instances;
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public List<String> getClassValues() {
        return classValues;
    }

    public String getClassValueAt(int index) {
        return classValues.get(index);
    }

    private void extractClassValuesFrom(Instances wekaData) {
        for (Enumeration<Object> e = wekaData.classAttribute().enumerateValues(); e.hasMoreElements(); ) {
            classValues.add(String.valueOf(e.nextElement()));
        }
    }

    private void extractInstancesFrom(Instances wekaData) {
        int i = 0; for (Enumeration<weka.core.Instance> e = wekaData.enumerateInstances(); e.hasMoreElements(); ) {
            instances.add(new Instance(i++,e.nextElement()));
        }
    }

    private void extractAttributesFrom(Instances wekaData) {
        for (Enumeration<weka.core.Attribute> e = wekaData.enumerateAttributes(); e.hasMoreElements(); ) {
            attributes.add(new Attribute(e.nextElement()));
        }
        classAttribute = new Attribute(wekaData.attribute(wekaData.classIndex()), true);
    }
}
