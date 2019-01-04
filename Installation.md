Installation steps for Data Pipeline Infrastructure
-

Sunbird Data Pipeline installation is automated with ansible as configuration management tool.

```sh
devops/
  playbooks.yml
  inventory/
    sample_env/
      group_vars/
        dp.yml
      hosts
  roles/
    ansible-roles
```

Above depicts the folder structure for ansible code distribution.

All the variables which can have default values, for example **cassandra_ip_address** is dynamically derived or constant values assigned and added in to defaults.
You can override that in group_vars though.

### Variables

TODO

### Install DP

> Execute the following playbooks in order

General ansible execute format is `ansible-playbook -i inventory/sample_env/hosts <playbook.yml>`

1.  lpa_cassandra_provision.yml
2.  postgres-provision.yml
3.  es_telemetry_cluster_setup.yml
4.  kibana_provision.yml
5.  dp_zookeeper_provision.yml
6.  dp_kafka_provision.yml
7.  dp_kafka_setup.yml
8.  lpa_api_provision.yml
9.  lpa_secor_provision.yml
10.  lpa_spark_provision.yml
11.  lpa_api_deploy.yml
12.  lpa_secor_deploy.yml
13.  lpa_telemetry_backup_deploy.yml
14.  lpa_data-products_deploy.yml
15.  dp_kafka_indexer.yml
16.  influxdb_backup.yml
17.  dp_yarn_provision.yml
18.  samza_deploy.yml
19.  dp_samza_telemetry_schemas.yml
