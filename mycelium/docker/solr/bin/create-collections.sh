#!/bin/sh

/opt/solr/bin/solr zk upconfig -d /confs/relationships-solr8-config -n relationships -z zookeeper:2181
/opt/solr/bin/solr create -c relationships -n relationships