package org.wso2.carbon.ml.siddhi.extension.streaming.algorithm;

import scala.Tuple2;
import org.apache.spark.api.java.*;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.mllib.regression.LinearRegressionModel;
import org.apache.spark.mllib.regression.LinearRegressionWithSGD;
import org.apache.spark.SparkConf;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Arrays;


/**
 * Created by mahesh on 5/28/16.
 */
public class StreamingLinearRegression {
    private int learnType=0;
    private int windowShift=1;
    private int paramCount = 0;                                         // Number of x variables +1
    private int batchSize = 10;                                 // Maximum # of events, used for regression calculation
    private double ci = 0.95;                                           // Confidence Interval
    private int numIterations = 100;
    private double stepSize = 0.00000001;
    private double miniBatchFraction =1;
    private List<String> eventsMem=null;

    private  LinearRegressionModel model;
    private SparkConf conf = null;
    private JavaSparkContext sc = null;
    private LinearRegressionModel prevModel=null;
    private JavaRDD<LabeledPoint> eventsRDD;
    private boolean isBuiltModel;
    private MODEL_TYPE type;
    public enum MODEL_TYPE {BATCH_PROCESS, MOVING_WINDOW,TIME_BASED }
    private final int BATCH_PROCESS=0;
    private final int MOVING_WINDOW=1;
    private final int TIME_BASED   =2;

    public StreamingLinearRegression(int learnType,int windowShift,int paramCount, int batchSize, double ci, int numIteration, double stepSize, double miniBatchFraction){

        System.out.println("StreamingLinearRegression");
        //this.type = learnType;
        this.learnType = learnType;
        this.windowShift=windowShift;
        this.paramCount =paramCount;
        this.batchSize = batchSize;
        this.ci = ci;
        this.numIterations = numIteration ;
        this.stepSize      = stepSize;
        this.miniBatchFraction = miniBatchFraction;
        this.isBuiltModel = false;
        type=MODEL_TYPE.BATCH_PROCESS;

        conf = new SparkConf().setMaster("local[*]").setAppName("Linear Regression Example").set("spark.driver.allowMultipleContexts", "true");
        sc = new JavaSparkContext(conf);
        eventsMem = new ArrayList<String>();

    }

    public Object[] regress(Double[] eventData){

        String str="";
        for (int i=0;i<paramCount;i++){
            str+= eventData[i];
            if(i!=paramCount-1)str+=",";
        }
        eventsMem.add(str);

        double mse=0.0;

        switch(learnType){
            case BATCH_PROCESS:
                return regressAsBatches();

            case TIME_BASED:
                return regressAsTimeBased();

            case MOVING_WINDOW:
                return regressAsMovingWindow();

            default:
                return null;
        }
    }

    public Object[] regressAsBatches(){
        int memSize=eventsMem.size();

        if(memSize >= batchSize){
            Object[]output= buildModel(eventsMem);
            eventsMem.clear();
            return output;

        }else{
            return null;
        }
    }

    //Time Based Learning Model
    public Object[] regressAsTimeBased(){
        return null;
    }

    //Moving Window Re-training Models
    public Object[] regressAsMovingWindow(){
        int memSize=eventsMem.size();
        Object[]output=null;
        if(memSize >= batchSize){
            int eventCounter=0;
            List<String>movingEventsMem=null;
            movingEventsMem = new ArrayList<String>();
            Iterator<String> memIter = eventsMem.iterator();

            while(memIter.hasNext() && eventCounter<=batchSize){
                movingEventsMem.add(memIter.next());
                eventCounter++;
            }
            output=buildModel(movingEventsMem);

            for(int i=0;i<windowShift;i++) {
                eventsMem.remove(0);
            }

        }else{
            output=null;
        }
        return output;
    }

    public Object[] buildModel(List<String> eventsMem){
        eventsRDD=getRDD(sc,eventsMem);
        //Learning Methods
        if(!isBuiltModel) {
            isBuiltModel = true;
            model = trainData(eventsRDD, numIterations, stepSize,miniBatchFraction);
        }
        else {
            model = trainStreamData(eventsRDD, numIterations, stepSize, miniBatchFraction,model);
        }

        double mse= getMSE(eventsRDD,model);
        Object[]output= new Object[paramCount+1];
        output[0]=mse;
        output[1]=model.intercept();
        Vector v= model.weights();
        double [] beta=v.toArray();
        for(int i=0;i<paramCount-1;i++){
            output[i+2]=beta[i];
        }

        StreamingLinearRegressionModel streamModel = new StreamingLinearRegressionModel(model,mse);

        return output;
    }

    public static JavaRDD<LabeledPoint> getRDD (JavaSparkContext sc ,List<String> events){

        JavaRDD<String> data = sc.parallelize(events);
        JavaRDD<LabeledPoint> parsedData = data.map(
                new Function<String, LabeledPoint>() {
                    public LabeledPoint call(String line) {

                        String[] features = line.split(",");
                        double[] v = new double[features.length-1];
                        for (int i = 0; i < features.length - 1; i++)
                            v[i] = Double.parseDouble(features[i+1]);
                        return new LabeledPoint(Double.parseDouble(features[0]), Vectors.dense(v));
                    }
                }
        );
        parsedData.cache();
        return parsedData;
    }

    public static double getMSE(JavaRDD<LabeledPoint> parsedData,final LinearRegressionModel builtModel){

        JavaRDD<Tuple2<Double, Double>> valuesAndPreds = parsedData.map(
                new Function<LabeledPoint, Tuple2<Double, Double>>() {
                    public Tuple2<Double, Double> call(LabeledPoint point) {
                        double prediction = builtModel.predict(point.features());
                        return new Tuple2<Double, Double>(prediction, point.label());
                    }
                }
        );
        double MSE = new JavaDoubleRDD(valuesAndPreds.map(
                new Function<Tuple2<Double, Double>, Object>() {
                    public Object call(Tuple2<Double, Double> pair) {
                        return Math.pow(pair._1() - pair._2(), 2.0);
                    }
                }
        ).rdd()).mean();
        return MSE;
    }

    //Standalone Learning Algorithms
    public static LinearRegressionModel trainData (JavaRDD<LabeledPoint> parsedData, int numIterations, double stepSize, double miniBatchFraction) {
        // Building the model
        final LinearRegressionModel model =  LinearRegressionWithSGD.train(JavaRDD.toRDD(parsedData), numIterations, stepSize, miniBatchFraction);
        return model;
    }


    //Incremental Learning Models
    public static LinearRegressionModel trainStreamData (JavaRDD<LabeledPoint> parsedData,int numIterations,  double stepSize, double miniBatchFraction,LinearRegressionModel prevModel ) {
        // Building the model
        final LinearRegressionModel model = LinearRegressionWithSGD.train(JavaRDD.toRDD(parsedData), numIterations, stepSize,miniBatchFraction,prevModel.weights());
        return model;
    }


}
