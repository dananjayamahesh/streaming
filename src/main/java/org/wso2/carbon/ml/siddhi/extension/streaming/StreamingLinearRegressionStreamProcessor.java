package org.wso2.carbon.ml.siddhi.extension.streaming;

import org.wso2.siddhi.core.config.ExecutionPlanContext;
import org.wso2.siddhi.core.event.ComplexEvent;
import org.wso2.siddhi.core.event.ComplexEventChunk;
import org.wso2.siddhi.core.event.stream.StreamEvent;
import org.wso2.siddhi.core.event.stream.StreamEventCloner;
import org.wso2.siddhi.core.event.stream.populater.ComplexEventPopulater;
import org.wso2.siddhi.core.exception.ExecutionPlanCreationException;
import org.wso2.siddhi.core.executor.ConstantExpressionExecutor;
import org.wso2.siddhi.core.executor.ExpressionExecutor;
import org.wso2.siddhi.core.query.processor.Processor;
import org.wso2.siddhi.core.query.processor.stream.StreamProcessor;
import org.wso2.siddhi.query.api.definition.AbstractDefinition;
import org.wso2.siddhi.query.api.definition.Attribute;
import org.wso2.carbon.ml.siddhi.extension.streaming.algorithm.StreamingLinearRegression;
import java.util.ArrayList;
import java.util.List;

/**
 * The methods supported by this function are
 * streaming:streaminglr((learnType), (batchSize/timeFrame), (numIterations), (stepSize), (miniBatchFraction), (ci), salary, rbi, walks, strikeouts, errors)
   This class is intended to re-train existing built ML models with streaming data
 */

public class StreamingLinearRegressionStreamProcessor extends StreamProcessor {

    private int learnType =0;
    private int windowShift=1;
    private int paramCount = 0;                                         // Number of x variables +1
    private int calcInterval = 1;                                       // The frequency of regression calculation
    private int batchSize = 10;                                   // Maximum # of events, used for regression calculation
    private double ci = 0.95;                                           // Confidence Interval
    private double miniBatchFraction=1;
    private int paramPosition = 0;

    private int numIterations = 100;
    private double stepSize = 0.00000001;
    private int featureSize=1;  //P
    private StreamingLinearRegression streamingLinearRegression=null;

//Lets Take the attributes of Streaming LR

    @Override
    protected List<Attribute> init(AbstractDefinition inputDefinition, ExpressionExecutor[] attributeExpressionExecutors, ExecutionPlanContext executionPlanContext) {
        paramCount = attributeExpressionLength;
        int PARAM_WIDTH=7;
        // Capture constant inputs
        if (attributeExpressionExecutors[0] instanceof ConstantExpressionExecutor) {

            paramCount = paramCount - PARAM_WIDTH;
            featureSize=paramCount;//

            paramPosition = PARAM_WIDTH;
            try {
                learnType = ((Integer) attributeExpressionExecutors[0].execute(null));
                windowShift = ((Integer) attributeExpressionExecutors[1].execute(null));
                batchSize = ((Integer) attributeExpressionExecutors[2].execute(null));
                numIterations = ((Integer) attributeExpressionExecutors[3].execute(null));

            } catch (ClassCastException c) {
                throw new ExecutionPlanCreationException("Calculation interval, batch size and range should be of type int");
            }

            try{

                stepSize = ((Double) attributeExpressionExecutors[4].execute(null));
                miniBatchFraction = ((Double) attributeExpressionExecutors[5].execute(null));

            }catch(ClassCastException c){
                throw new ExecutionPlanCreationException("Step Size, Mini Batch Fraction should be in double format");
            }

            try {
                ci = ((Double) attributeExpressionExecutors[6].execute(null));
            } catch (ClassCastException c) {
                throw new ExecutionPlanCreationException("Confidence interval should be of type double and a value between 0 and 1");
            }
        }
        System.out.println("Parameters: "+" "+batchSize+" "+" "+ci+"\n");
        // Pick the appropriate regression calculator

        streamingLinearRegression = new StreamingLinearRegression(learnType,windowShift,paramCount, batchSize, ci, numIterations, stepSize, miniBatchFraction);

        // Add attributes for standard error and all beta values
        String betaVal;
        ArrayList<Attribute> attributes = new ArrayList<Attribute>(paramCount);
        attributes.add(new Attribute("stderr", Attribute.Type.DOUBLE));

        for (int itr = 0; itr < paramCount; itr++) {
            betaVal = "beta" + itr;
            attributes.add(new Attribute(betaVal, Attribute.Type.DOUBLE));
        }

        return attributes;
    }

    @Override
    protected void process(ComplexEventChunk<StreamEvent> streamEventChunk, Processor nextProcessor, StreamEventCloner streamEventCloner, ComplexEventPopulater complexEventPopulater) {
        synchronized (this) {
            while (streamEventChunk.hasNext()) {
                ComplexEvent complexEvent = streamEventChunk.next();

                Object[] inputData = new Object[attributeExpressionLength - paramPosition];
                Double[] eventData = new Double[attributeExpressionLength - paramPosition];


                for (int i = paramPosition; i < attributeExpressionLength; i++) {
                    inputData[i - paramPosition] = attributeExpressionExecutors[i].execute(complexEvent);
                    eventData[i - paramPosition] = (Double) attributeExpressionExecutors[i].execute(complexEvent);
                }

                //Object[] outputData = regressionCalculator.calculateLinearRegression(inputData);

                // Object[] outputData= streamingLinearRegression.addToRDD(eventData);
                //Calling the regress function
                Object[] outputData = streamingLinearRegression.regress(eventData);

                // Skip processing if user has specified calculation interval
                if (outputData == null) {
                    streamEventChunk.remove();

                } else {
                    log.info("MSE: "+outputData[0]);
                    complexEventPopulater.populateComplexEvent(complexEvent, outputData);
                }
            }
        }
        nextProcessor.process(streamEventChunk);

    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public Object[] currentState() {
        return new Object[0];
    }

    @Override
    public void restoreState(Object[] state) {

    }

}
