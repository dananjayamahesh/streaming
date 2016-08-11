# streaming - Real Time Massive Predictive Analytics with Online Big Data
This project is to develop deep learning based streaming data analytics platform to predictive analytics which support retrain of machine learning models using advanced algorithms like Stochastic Gradient Descent based deep learning algorithms and mini btach processing. More importantly this supports massive online data analysis with the high speed streaming support which can be used alongside with the modern day distributed processing and data storin techniques such as apache storm, samza and p4. As a Google Summer of code contributor this platform is integrated with the WSO2 CEP (Complex Event Processing) plaform and their ML (Machine Learner) for massive data analysis real time saving lots of resources at run time.
This Framework currenlty support three real time streaming ml:
	1. Apache Spark Based Streaming Linear Regression based on SGD (Staochastic Gradient Descent) Optimization
	2. Apache Saprk Based Streaming KMeans Clustering based on mini-batch clustering
	3. Apache Samoa Based Streaming Clustering based on Mini-Batch Kernal Clustering

##For Samoa Based Streaming Clustering Topology

streamingml : streamclusteringsamoa ([learn-type], [window-shift], [batch-size],[num-iterations], [num-clusters], [alpha], [ci],[param0],...., [paramp]);

##For Spark Based Streaming Clustering Topology

streaming : streamclustering ([learn-type], [window-shift], [batch-size],[num-iterations], [num-clusters], [alpha], [ci],[param0],...., [paramp]);

##Example Query

@Import('ccppInputStream:1.0.0')
define stream ccppInputStream (PE double, ATV double, V double, AP double, RH double);

@Export('ccppOutputStream:1.0.0')
define stream ccppOutputStream (PE double, ATV double, V double, AP double, RH double, stderr double, center0 string, center1 string);

from ccppInputStream#streaming:streamclusteringsamoa(0, 0, 1000, 10, 2, 1, 0.95, PE, ATV, V, AP, RH)
select *
insert into ccppOutputStream;


##Paramters for Streaming CLustering


