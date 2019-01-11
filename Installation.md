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

Mandatory variables to update

| Variable | Definition |
| ---  | --- |
| env | to denote environment |
| azure_storage_secret | Azure stoage secrets for blob |
| azure_account_name | Azure account name which you use |
| azure_public_container | Name of the container you store data |
| azure_backup_account_name | Name of the container to store backup |
| azure_backup_storage_secret | Secret key for the storage account to store backup |
| secrets_path | Ansible vault secrets file path |
| postgres:db_password | password for postgres |

> All values start with `vault_` in group_vars are passwords/keys you'll have to update

### Prerequisites

- [ansible v2.5.0](https://docs.ansible.com/ansible/latest/installation_guide/intro_installation.html#latest-releases-via-pip)

**Instances Required**

1. analytics-api - 1 (2 cpu, 8GB Memory)
2. Secor,Influxdb and kafka-indexer - 1 (4 cpu, 16GB Memory)
3. Saprk - 1 (2 cpu, 8GB Memory)
4. yarn-master - 1 (2 cpu, 8GB Memory)
5. yarn-slave - 3 (2 cpu, 8GB Memory)
6. kafka and zookeeper - 1 (2 cpu, 8GB Memory)
7. elasticsearch - 1 ( 2 cpu, 4GB Memory)
8. cassandra - 1 ( 2 cpu, 4GB Memory)

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
15.  dp_yarn_provision.yml
16.  dp_samza_telemetry_schemas.yml
17.  samza_deploy.yml
18.  influxdb_provision.yml
19.  dp_kafka_indexer.yml
