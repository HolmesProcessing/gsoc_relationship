# GSoC: Holmes Automated Malware Relationships (WIP)

## Introduction

![GitHub Logo](/images/architecture.png)
*Figure 1: System Architecture*

###### Overview

The purpose of this project is to develop a system capable of automatically
identifying and managing the relationships between malware objects (IP addresses,
Domains, Executables, Files etc). This system will use the analytic results as
generated and stored by Holmes-Totem and Holmes-Totem-Dynamic. The goals are:


1. Define the malware attributes necessary for relationship detection through querying.
2. Implement Machine Learning algorithms for relationship detection.
3. Implement an algorithm for aggregating and scoring the final relationships.
4. Visualize relationships for the client.

This system will perform malware relationship detection and scoring by using a range of queries and ML algorithms. We will implement and optimize some existing and new ML algorithms in order to ensure accuracy and efficiency. The whole relationship detection and rating process will go through two stages and at the end the user will receive a visual representation of the generated final relationships.

###### Technology

We will use **Apache Spark** and **Tensorflow** for writing and running the
necessary Queries and Machine Learning algorithms. The system will use a mix of
batch and stream processing so **Spark Streaming** and/or **Apache
Beam** are the framework of choice. **RabbitMQ** is the AMQP library of choice to
support the streaming functionality. The data is stored in **Apache Cassandra**.
Since this is a work in progress, there is a good chance that some new technologies and frameworks may be added along the way. This section will be updated accordingly.

## Defining Relationships

The relationship detection process goes through two stages. The first stage
(Offline Training) is
going to generate the first level of relationships, while the second stage (Final Relationships Generate) will
define the final relationships and their score by using the data created from
the first stage as seed.

From a technical standpoint, the analytics of the first stage happen independently of any user requests. The Query and ML components automatically perform all queries and ML algorithms for new malware analytic results based on specific events and triggers. All of the data generated at this stage is permanently stored on Cassandra using an appropriate schema. (During the rest of the section, I may refer to the primary relationships simply as relationships for simplicity’s sake.)

###### Primary Relationships

There are 4 types of artefacts in our database: IP addresses, Domains, Files, and Binary Executables. All of these types can potentially have a relationship with each other.

*For example:* An executable may issue a call to a specific domain who is associated with one or more IPs, which might be in turn related to other artefacts. In this scenario we already have identified several relationships:
1. Executable <-> Domain
2. Domain <-> IP
3. (and by the transitive property of a bidirectional connection): Executable <-> IP

The whole purpose of this stage of the process is to look for meaningful primary relationships between the different artefacts based on the available analytic results, and store these relationships permanently. The following graph is a high level view of the potential relationship types between artefacts.

![GitHub Logo](/images/Relationship_Types.png)
*Figure 2: High-level view of artefact relationships*

The relationships between artefacts will be defined in detail by the indicators that the Querying and ML components will detect/calculate. The first step of relationships discovery is finding good indicators of relationships between the different artefacts. These indicators are extracted by processing the analytic results from Holmes Totem (Dynamic). The components responsible for performing this analytic processes are the Query and ML components.

###### Final Relationships
There are three kinds of query for final relationships. They are queries for malware sample, domain and IP seperately.

1. When quering a malware sample, we can
	1.1 Give relationship scores between malware sample. We use an algorithm considering the every features between malware sample. It is **malware->malware**. 
	
	1.2 Give relationship scores between malware and domain. The relationships include direct relationships ( MalwareA <-> Domain ) and indirect relationships
( MalwareA <-> MalwareB <-> Domain and MalwareA <-> IP <-> Domain ). It is **malware->domain**.

	1.3 Give relationship scores between malware and IP. The relationships include direct and indirect relationships, which is similar to the malware->domain. It is **malware->IP**.
	
	1.4 Choose part of features to generate relationship scores between malware sample. In normal, we consider all features to give a score (1.1 does). Furthermore, in this pro mode, we choose the features we like. It is **malware->malware PRO**.
2. When quering a domain, we can

	2.1  Give relationship scores between domain and malware. It is similar to 1.2 and includes direct relationships ( Domain <-> MalwareA ) and indirect relationships
( Domain <-> MalwareA <-> MalwareB and Domain <-> IP <-> MalwareA ). It is **domain->malware**.

	2.2 Give relationship scores between domain. It includes direct relationships ( DomainX <-> DomainY ) and indirect relationships ( DomainX <-> Malware <-> DomainY and DomainX <-> IP <-> DomainY ). In addition, 2nd level indirect relationships ( DomainX <-> MalwareA <-> MalwareB <-> DomainY ) are also available. It is **domain->domain**. 
	
	2.3 Give relationship scores between domain and IP. It includes direct relationships ( Domain X <-> IP ) and indirect relationships ( DomainX <-> Malware <-> IP and DomainX <-> DomainY <-> IP and DomainX <-> IPA <-> IPB). It is **domain->IP**.
3. When quering an IP, the relationship results are similar to querying domain. They are **IP->malware**, **IP->IP** and **IP->domain**.

###### Direct and Indirect Relationships
The difference between direct and indirect relationships are based on whether this relationship can be obtained directly from the primary_table. 

We take **1.2 malware->domain** in the Final Relationships section as an example.
	
1. Direct relationships:

	To Malware\_A, we can get a list of domains by Cuckoo and these relationships are saved in primary\_table. They are direct relationships and we present them like this (Malware_A <-> Domain).

2. Indirect relationships:

	2.1 To Malware\_A, we can get a list of malwares with high confidence scores called Malware\_A\_list. We also can get a list of domains by Cuckoo’s analysis of these malwares, so there are indirect relationships between Malware\_A and domains.
( Malware\_A <-> Malware\_A\_list <-> Domains ).
 
	2.2 To Malware\_A, we can get a list of IPs directly connected to it. These IPs may associate with one or more domains. So there are indirect relationships between Malware\_A and domains. (Malware\_A <-> IP <-> Domains).
	
	2.3 To all indirect relationships, it is necessary to design an algorithm to give confidence relationship scores. 



## Storage and Schema

The storage schema will store the primary relationships for both the Query and the ML components. The schema should be easily and efficiently queried in order to provide stage 2 with all the necessary data with as little lag and computational effort as possible. The schema table will contain all the relationship_types and relationship_values generated by the Query and ML components. The relationship_types correspond to the ones that can be seen in Figure 1. The possible relationship_values can be seen in Table 1. Currently, Table 1 only includes potential relationship data based on the information provided by the Query Component. The table will be updated as the components are developed.

The table schema developed in this stage should satisfy 2 main queries:

1. **Q1:** Give me all relationships for obj_id
2. **Q2:** Give me all objects that subscribe to relationship_value
3. (**Q3:** Give me all objects that subscribe to relationship_type)

![GitHub Logo](/images/schema.png)

*Figure 3: Table View*

The picture above represents the two main tables that should satisfy the queries from Stage 2.
The original base table easily satisfies Q1. The table created through the MV can satisfy Q2.
Even Q3 can be easily addressed with a slightly different MV.

The relationship values for each primary relationship can be either direct values or references unique identifiers that can be used to query lookup tables for additional details on the relationship value. Lookup tables are generated for a specific subset of relationship values.

###### Final relationship storage
The final relationship storage is used to extract the query result when an artefact is queried second time or more. In storage, we do not distinguish between direct and indirect relationships and we use one table to save all final relationship.

	final_relationship_table(
		query_object_id text,
		relationship_object_id text,
		relationship_type text,              
		confidence_score int,
	same_indicators_between_object  list,     
		TTL int,
		PRIMARY KEY ( query_object_id, relationship_type )
		) 

1). query\_object\_id and relationship\_object\_id consist of the relationship pair.

2). relationship\_type represents the relationship type between query\_object and relationship\_object and is granularity. It can be malware\_direct\_domains,  malware\_malware\_indirect\_domains, malware\_IP\_indirect\_domains and so on.
We take malware\_IP\_indirect\_domain as an example. This relationship is indirect relationship and relationship are malware <-> IP <-> domain.

3). same\_indicators\_between\_object is a list and used to display the relationships in the website.


## Implementation

#### Offline Search and Training

###### Query Component


This component will look for atomic indicators of relationships. Atomic indicators are either identical values that can be shared by artefacts’ analytic results or calculated values that are used to provide some measure of similarity between artefacts. The Query component will generate the following relationship_types and values. These primary relationships will also have an assigned weight that can be used by the second stage of the process to calculate the final relationships.

 (relationship_type, service, relationship_value)  | weight_definition
 ------------------------------------------------- | -----------------
 (imphash_similar_to, PEInfo, imphash)  |   
 (pehash_similar_to, PEInfo, pehash) |  
 (signed_by, PEInfo, signature) |  
 (communicated_with_ip, CUCKOO, ip) |  
 (communicated_with_dom, CUCKOO, domain) |  
 (related_to_ip, DNSMeta, ip) |  
 (resolves_to_ip, DNSMeta, A Record )  |  
 (resolves_to_ip, DNSMeta, AAAA Record)  |  
 (related_to, DNSMeta, metadata)  |  
 (av_similar_to, VirusTotal, signature_similarity) |  
 (yara_similar_to, YARA, complex_AV_match) |  

*Table 1: Definitions for primary relationships*



###### ML Component

This component will utilize ML algorithms to train models based on a labeled dataset and then assign every new unknown incoming artefact (depending on the type of artefact) to one of the trained malicious clusters/classes.

#### Final Relationships Generator
###### Query Example
To get the final relationship score, we first query in the primary relationship table. I will show query examples.

1.1 malware->malware:

1). Do Q1 ( give me all relationships for malware\_A ) in primary\_relationships\_table and we can get malware\_A\_features. 

2). Do Q2 ( give me all malware objects that subscribe to the each element in the malware\_A\_features ) in mv\_primary\_relationships\_table and we can get related_malwares. 

3). Do Q1 ( give me all relationships for each element in the related\_malwares) in primary\_relationships\_table and then give the score between element in the related\_malwares and malware\_A. To give the exact final relationship score, we must consider the same and different parts of two malwares.

4). Finally we save the result malware\_final\_score in database. 
 
1.2 malware->domain: 

1). Do Q1 ( give me all domain\_relationships for malware\_A ) in primary\_relationships\_table and we can get a list of malware\_A\_direct\_domains. 

2). Do Q2 ( give me all domain\_relationships for each element in related\_malwares) and then we can give scores between malware\_A and each different\_domain (related\_malwares have and malware\_A dont have) by an algorithms considering the final relationship score between malwares and the number of the domain. So, we can get a list of malware\_A\_malware\_indirect\_domains.

3). Do Q1 ( give me all IP\_relationships for malware\_A ) and then still do Q1 ( give me all domain_relationships for IP ). So we can get a list of malware\_A\_IP\_indirect\_domains.

1.3 malware->IP: 

This query is similar to 1.2 (malware->domain)

1.4 malware->malware PRO:

This query is similar to 1.1 (malware->malware). The only difference is the algorithm.

The query and storage about domain and IP are similar to that of malware.
