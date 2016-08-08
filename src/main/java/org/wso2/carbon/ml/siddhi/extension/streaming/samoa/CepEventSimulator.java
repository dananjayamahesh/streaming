package org.wso2.carbon.ml.siddhi.extension.streaming.samoa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Scanner;

/**
 * Created by mahesh on 7/30/16.
 */
public class CepEventSimulator {

    private static final Logger logger = LoggerFactory.getLogger(CepEventSimulator.class);

    public static Scanner scn;
    public static void main(String[] args){
        System.out.println("Starts");
        try {
            File f = new File("/home/mahesh/GSOC/DataSets/regression-ccpp/CCPP/ccpp1.csv");
            FileReader fr = new FileReader(f);
            BufferedReader br = new BufferedReader(fr);
            scn = new Scanner(br);

        }catch (Exception e){
            logger.info(e.toString());
        }
        int learnType = 0;
        int paramCount = 5;
        int batchSize = 1000;
        double ci = 0.95;
        int numClusters = 2;
        int numIterations = 10;
        int alpha = 1;
        int numInsancesSent=0;
        int numAttribute = 5;
        StreamingClustering streamingClusteringWithSamoa = new StreamingClustering(learnType,paramCount, batchSize, ci,numClusters, numIterations,alpha);

        new Thread(streamingClusteringWithSamoa).start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("Successfully Instatiated the Clustering with samoa");

        double [] cepEvent=new double[paramCount];
       /*for(int i=0;i<paramCount;i++){
            cepEvent[i]=(int)(Math.random()*100);

        }*/
        while(true){
            Object[] outputData = null;
            // logger.info("Sending Next Event"+numInsancesSent++);
            // Object[] outputData= streamingLinearRegression.addToRDD(eventData);
            //Calling the regress function
            if(scn.hasNext()) {
                String eventStr = scn.nextLine();
                String[] event=eventStr.split(",");
                for(int i=0;i<paramCount;i++){
                    cepEvent[i]=Double.parseDouble(event[i]);

                }
                outputData = streamingClusteringWithSamoa.cluster(cepEvent);

                if (outputData == null) {
                    //  System.out.println("null");
                } else {
                    System.out.println("Error: " + outputData[0]);
                    for (int i = 0; i < numClusters; i++) {
                        System.out.println("center " + i + ": " + outputData[i + 1]);
                    }
                }
            }


            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


        }
    }
}
