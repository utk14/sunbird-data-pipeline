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

### Playbooks to run in order

General ansible execute format is

`ansible-playbook -i inventory/sample_env/hosts <playbook.yml>`

- lpa_cassandra_provision.yml
- postgres-provision.yml
- es_telemetry_cluster_setup.yml
- kibana_provision.yml
- dp_zookeeper_provision.yml
- dp_kafka_provision.yml
- dp_kafka_setup.yml
- lpa_api_provision.yml
- lpa_secor_provision.yml
- lpa_spark_provision.yml
- lpa_api_deploy.yml
- lpa_secor_deploy.yml
- lpa_telemetry_backup_deploy.yml
- lpa_data-products_deploy.yml
- dp_kafka_indexer.yml
- influxdb_backup.yml
- dp_yarn_provision.yml
- samza_deploy.yml
- dp_samza_telemetry_schemas.yml
