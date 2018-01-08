package edu.uw.cs;

import edu.uw.cs.model.Dataset;

/**
 * Created by shantanusinghal on 16/02/17 @ 6:53 PM.
 * NET-ID: singhal5
 * Campus ID: 9076101956
 */
interface Classifier {

    Predictions predict(Dataset dataset);

    String getStructureInfo();

}
