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

import java.util.Iterator;

/**
 * Created by mahesh on 6/4/16.
 */
public class StreamingKMeansClustering {
    private int learnType=0;
    private int paramCount = 0;                                         // Number of x variables +1
    private int batchSize = 10;                                 // Maximum # of events, used for regression calculation
    private double ci = 0.95;                                           // Confidence Interval
    private int numClusters=1;
    private int numIterations = 100;
    private double alpha=0;
    private int nt=0;
    private int mt=0;
    private Vector []clusterCenters;
    private JavaRDD<String> events=null;
    private List<String> eventsMem=null;

    private  KMeansModel model;

    private StreamingKMeansClusteringModel streamingKMeansClusteringModel;

    private SparkConf conf = null;
    private JavaSparkContext sc = null;
    private KMeansModel prevModel=null;
    private JavaRDD<Vector> eventsRDD;
    private boolean isBuiltModel;
    private MODEL_TYPE type;
    public enum MODEL_TYPE {BATCH_PROCESS, MOVING_WINDOW,TIME_BASED }

    public final int BATCH_PROCESS =0;
    public final int MOVING_WINDOW = 1;
    public final int TIME_BASED =2;

    public StreamingKMeansClustering(int learnType,int paramCount, int batchSize, double ci, int numClusters,int numIteration, double alpha){
        this.learnType = learnType;
        this.paramCount =paramCount;
        this.batchSize = batchSize;
        this.ci = ci;
        this.numClusters = numClusters;
        this.numIterations = numIteration ;
        this.alpha = alpha;
        this.isBuiltModel = false;
        type=MODEL_TYPE.BATCH_PROCESS;
        conf = new SparkConf().setMaster("local[*]").setAppName("Linear Regression Example").set("spark.driver.allowMultipleContexts", "true") ;
        sc = new JavaSparkContext(conf);
        eventsMem = new ArrayList<String>();

    }

    public Object[] cluster(Double[] eventData){

        String str="";
        for (int i=0;i<paramCount;i++){
            str+= eventData[i];
            if(i!=paramCount-1)str+=",";
        }
        eventsMem.add(str);


        Object[]output=null;
        switch(learnType){
            case BATCH_PROCESS:
                output=clusterAsBatches();
                return output;

            case TIME_BASED:
                output= clusterAsTimeBased();
                return output;

            case MOVING_WINDOW:
                output= clusterAsMovingWindow();
                return output;

            default:
                return null;
        }
    }

    public Object[] clusterAsBatches(){
        //double mse=0;
        int memSize=eventsMem.size();
        if(memSize >= batchSize){

            System.out.println("Start Training");
            Object[]output= buildModel(eventsMem);
            eventsMem.clear();
            return output;

        }else {
            mt=0;
            return null;
        }
    }

    //Time Based Learning Model
    public Object[] clusterAsTimeBased(){
        double wssse=0;
        return null;
    }

    public Object[] clusterAsMovingWindow(){
        double wssse=0;
        return null;
    }

    public Object[] buildModel(List<String> eventsMem){
        System.out.println("Build Model");
        eventsRDD = getRDD(sc,eventsMem);
        KMeansModel newModel = null;
        Vector clusterWeights = null;
        //Learning Methods
        if(!isBuiltModel) {
            isBuiltModel = true;
            newModel = trainData(eventsRDD,numClusters, numIterations);
            clusterCenters = newModel.clusterCenters();
            clusterWeights = getClutserWeights(eventsRDD,newModel,numClusters);
            streamingKMeansClusteringModel = new StreamingKMeansClusteringModel(newModel,clusterCenters,clusterWeights);

        }
        else {
            newModel = trainData(eventsRDD, numClusters, numIterations);
            clusterCenters = newModel.clusterCenters();
            clusterWeights = getClutserWeights(eventsRDD, newModel,numClusters);
            StreamingKMeansClusteringModel newStreamingModel= new StreamingKMeansClusteringModel(newModel,clusterCenters,clusterWeights);
            streamingKMeansClusteringModel = retrainModel(streamingKMeansClusteringModel, newStreamingModel,numClusters,paramCount);
        }

        model=newModel;
        double wssse= getWSSSE(eventsRDD,newModel);
        Object[]output= new Object[numClusters+1];
        output[0]=wssse;
        // output[1]=1.0;
        /*for(int i=0;i<paramCount-1;i++){
            output[i+2]=1.0;
        }*/
        //output Cluster Centers
        for(int i=0;i<numClusters;i++){
            String centerStr="";
            double[] center = clusterCenters[i].toArray();
            centerStr += center[0]+"";
            for(int j=1;j<paramCount;j++){
                centerStr += (","+center[j]);
            }
            output[i+1] = centerStr;
        }


        return output;
    }

    public static JavaRDD<Vector> getRDD (JavaSparkContext sc ,List<String> events){
        System.out.println("Train-Stream-Data\n");
        JavaRDD<String> data = sc.parallelize(events);
        JavaRDD<Vector> parsedData = data.map(
                new Function<String, Vector>() {
                    public Vector call(String s) {
                        String[] sarray = s.split(",");
                        double[] values = new double[sarray.length];
                        for (int i = 0; i < sarray.length; i++)
                            values[i] = Double.parseDouble(sarray[i]);
                        return Vectors.dense(values);
                    }
                }
        );
        parsedData.cache();
        return parsedData;
    }

    public static double getWSSSE(JavaRDD<Vector> parsedData,final KMeansModel clusters){

        double WSSSE = clusters.computeCost(parsedData.rdd());
        System.out.println("Within Set Sum of Squared Errors = " + WSSSE);
        System.out.println("training Mean Squared Error = " + WSSSE);
        return WSSSE;
    }

    public static KMeansModel trainData(JavaRDD<Vector>points, int numClusters, int numIterations){
        int runs=1;
        KMeansModel model = KMeans.train(points.rdd(),numClusters,numIterations, runs, KMeans.K_MEANS_PARALLEL());
        return model;
    }

    public static StreamingKMeansClusteringModel retrainModel(StreamingKMeansClusteringModel prevModel,StreamingKMeansClusteringModel newModel, int numClusters, int dim ){
        //Add the Streaming algorithms
        return newModel;
    }

    public static Vector getClutserWeights(JavaRDD<Vector> eventsRDD, KMeansModel model, int numClusters ){
        System.out.println("CLuster Weights");
        JavaRDD<Integer> weights = model.predict(eventsRDD);
        List<Integer> list = weights.collect();
        double [] w = new double[numClusters];
        Iterator<Integer> iter = list.iterator();

        while(iter.hasNext()){
            int i= iter.next();
            //System.out.println(i);
            w[i]++;

        }
        return Vectors.dense(w);
    }

}
