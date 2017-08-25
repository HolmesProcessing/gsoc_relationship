import sys
from cassandra.cluster import Cluster
from cassandra.auth import PlainTextAuthProvider

#User arguments should contain the following content in that order: ip address, username, password, keyspace

def cluster_setup(ip_address, username, password, keyspace):
    auth_provider = PlainTextAuthProvider(username=username, password=password)
    contact_point = []
    contact_point.append(ip_address)
    cluster = Cluster(contact_points=contact_point, auth_provider=auth_provider)
    session = cluster.connect(keyspace)
    return session

def schema_setup(ip_addresses, username, password, keyspace):

    session = cluster_setup(ip_addresses, username, password, keyspace)

    #Create the Knowledge Base tables and related Materialized Views
    session.execute(
        """CREATE TABLE IF NOT EXISTS analytics_knowledge_base(
        object_id text,
        feature_type text,
        feature_value blob,
        timestamp timeuuid,
        PRIMARY KEY ((object_id), feature_type, timestamp)
        );
        """
        )

    session.execute("""CREATE MATERIALIZED VIEW IF NOT EXISTS analytics_mv_knowledge_base_by_feature AS
        SELECT * FROM analytics_knowledge_base
        WHERE object_id IS NOT NULL
        AND feature_type IS NOT NULL
        AND feature_value IS NOT NULL
        AND timestamp IS NOT NULL
        PRIMARY KEY ((feature_type), object_id, timestamp);
        """
        )

    #Create the Primary Relationships table

    session.execute(
        """CREATE TABLE IF NOT EXISTS analytics_primary_relationships(
        object_id text,
        timestamp timeuuid,
        imphash blob,
        pehash blob,
        binary_signature blob,
        domain_requests blob,
        yara_rules blob,
        PRIMARY KEY (object_id));
        """
        )

def main(argv):
	
	if (len(argv) == 4):
		schema_setup(argv[0],argv[1],argv[2],argv[3])
		print "Storage setup was successful."
	else:
		print 'Number of arguments:', len(argv), 'arguments.'
		print "You need exactly 4 arguments: ip address, username, password, keyspace - in that order"
   		print 'Argument List:', str(argv)



if __name__ == '__main__':
    main(sys.argv[1:])

