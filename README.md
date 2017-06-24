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

## Storage and Schema

(WIP)

## Implementation

(WIP)
