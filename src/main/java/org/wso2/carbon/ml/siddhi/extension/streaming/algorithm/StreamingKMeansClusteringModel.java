package org.wso2.carbon.ml.siddhi.extension.streaming.algorithm;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.mllib.clustering.KMeans;
import org.apache.spark.mllib.clustering.KMeansModel;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;
import java.util.List;
import java.util.ArrayList;
/**
 * Created by mahesh on 6/7/16.
 */
public class StreamingKMeansClusteringModel {
    private KMeansModel model;
    private Vector[] clusterCenters;
    private Vector clusterWeights;

    public StreamingKMeansClusteringModel(KMeansModel model, Vector [] clusterCenters, Vector clusterWeights){
        this.model = model;
        this.clusterCenters=clusterCenters;
        this.clusterWeights=clusterWeights;
    }

    public KMeansModel getModel(){
        return this.model;
    }

    public void setModel(KMeansModel model){
        this.model = model;
    }

    public Vector[] getClusterCenters(){
        return this.clusterCenters;
    }

    public void setClutserCenters(Vector[]clusterCenters){
        this.clusterCenters = clusterCenters;
    }

    public Vector getClusterWeigts(){
        return this.clusterWeights;
    }

    public void setClusterWeights(Vector clusterWeights){
        this.clusterWeights = clusterWeights;
    }


}
