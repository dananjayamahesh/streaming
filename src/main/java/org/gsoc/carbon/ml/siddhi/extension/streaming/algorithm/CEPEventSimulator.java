package org.gsoc.carbon.ml.siddhi.extension.streaming.algorithm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Scanner;

/**
 * Created by mahesh on 8/5/16.
 */
public class CEPEventSimulator {
    // private static final Logger logger = LoggerFactory.getLogger(CepEventSimulator.class);

    public static Scanner scn;
    public static void main(String[] args){

        try {
            File file = new File("/home/mahesh/GSOC/DataSets/regression-ccpp/CCPP/ccpp1.csv");
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            scn = new Scanner(br);
        }catch(Exception e){

        }
        System.out.println("Starts");
        int learnType = 0;
        int paramCount = 5;
        int batchSize = 10000;
        double ci = 0.95;
        int numClusters = 2;
        int numIterations = 10;
        int alpha = 1;
        int numInsancesSent=0;
        int numAttribute = 5;

        double stepSize=0.00000001;
        double miniBatchFraction=1;
        int movingShift=0;
        StreamingKMeansClustering streamingClusteringWithSamoa = new StreamingKMeansClustering(learnType,paramCount, batchSize, ci,numClusters, numIterations,alpha);
        //StreamingLinearRegression streamingClusteringWithSamoa = new StreamingLinearRegression(learnType,movingShift,paramCount, batchSize, ci, numIterations, stepSize, miniBatchFraction);

        //new Thread(streamingClusteringWithSamoa).start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //logger.info("Successfully Instatiated the Clustering with samoa");
        Double [] cepEvent=new Double[paramCount];

        while(true){

            Object[] outputData = null;
            // logger.info("Sending Next Event"+numInsancesSent++);
            // Object[] outputData= streamingLinearRegression.addToRDD(eventData);
            //Calling the regress function

            if(scn.hasNext()) {
                String eventStr = scn.next();
                String[]events=eventStr.split(",");
                for(int i=0;i<paramCount;i++){
                    cepEvent[i]=Double.parseDouble(events[i]);
                }
                outputData = streamingClusteringWithSamoa.cluster(cepEvent);
                // outputData = streamingClusteringWithSamoa.regress(cepEvent);
                if (outputData == null) {
                    //  System.out.println("null");
                } else {
                    System.out.println("Error: " + outputData[0]);
                    for (int i = 0; i < numClusters; i++) {
                        System.out.println("center " + i + ": " + outputData[i + 1]);
                    }
                }
            }else{

            }


            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


        }
    }

}