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

	session.execute(
		"""CREATE TABLE IF NOT EXISTS relationship_primary_object_table(
		object_id TEXT,
		relationship_type TEXT,
		relationship_value TEXT,
		timestamp timeuuid,
		PRIMARY KEY ((object_id), relationship_type, relationship_value, timestamp)
		);
		"""
		)

	session.execute(
		"""CREATE MATERIALIZED VIEW IF NOT EXISTS mv_relationship_primary_object_value_table AS
		SELECT * FROM relationship_primary_object_table
		WHERE object_id IS NOT NULL
		AND relationship_type IS NOT NULL
		AND relationship_value IS NOT NULL
		AND timestamp IS NOT NULL
		PRIMARY KEY ((relationship_value), object_id, relationship_type, timestamp);
		"""
		)

	session.execute(
		"""CREATE MATERIALIZED VIEW IF NOT EXISTS mv_relationship_primary_object_type_table AS
		SELECT * FROM relationship_primary_object_table
		WHERE object_id IS NOT NULL
		AND relationship_type IS NOT NULL
		AND relationship_value IS NOT NULL
		AND timestamp IS NOT NULL
		PRIMARY KEY ((relationship_value), object_id, relationship_type, timestamp);
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
