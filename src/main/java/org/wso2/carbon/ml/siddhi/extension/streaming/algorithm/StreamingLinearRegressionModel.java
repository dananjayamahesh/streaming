package org.wso2.carbon.ml.siddhi.extension.streaming.algorithm;

import org.apache.spark.mllib.regression.LinearRegressionModel;
import org.apache.spark.mllib.regression.LinearRegressionWithSGD;

/**
 * Created by mahesh on 6/4/16.
 */
public class StreamingLinearRegressionModel {

    private LinearRegressionModel model;
    private double mse;

    public StreamingLinearRegressionModel(LinearRegressionModel model, double mse){
        this.model = model;
        this.mse = mse;
    }
    public double getMSE(){
        return this.mse;
    }

    public LinearRegressionModel getModel(){
        return this.model;
    }

}
