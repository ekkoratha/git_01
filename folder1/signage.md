spring:
  application:
    name: scs-100

  cloud.stream:
    bindings:
      ##
      inventoryChecking-out:
        destination: scs-100.inventoryChecking
      inventoryChecking-in:
        destination: scs-100.inventoryChecking
        group: ${spring.application.name}-inventoryChecking-group
        consumer:
          maxAttempts: 1 # on example we are simulating "out of stock" scenario, so there is no point for retrying after it failed in the first attempt

      order-dlq:
        destination: scs-100.ordering_dlq

      shipping-out:
        destination: scs-100.shipping
      shipping-in:
        destination: scs-100.shipping
        group: ${spring.application.name}-shipping-group

    kafka:
      bindings:
        # If Inventory Checking fails
        inventoryChecking-in.consumer:
          enableDlq: true
          dlqName: scs-100.ordering_dlq
          autoCommitOnError: true
          AutoCommitOffset: true

        # If shipping fails
        shipping-in.consumer:
          enableDlq: true
          dlqName: scs-100.ordering_dlq
          autoCommitOnError: true
          AutoCommitOffset: true

logging:
  level:
    com.ehsaniara.scs_kafka_intro: debug
	
	
	import yaml
import re

# pip install pyyaml
# python -m pip install --upgrade pip
def parse_kafka_properties(file_path):
    with open(file_path, 'r') as file:
        config = yaml.safe_load(file)

    app_name = config.get('spring', {}).get('application', {}).get('name', 'unknown-app')

    bindings = config.get('spring', {}).get('cloud.stream', {}).get('bindings', {})
    kafka_bindings = config.get('spring', {}).get('cloud.stream', {}).get('kafka', {}).get('bindings', {})

    producers = {}
    consumers = {}

    for key, value in bindings.items():
        if '-in' in key:
            consumers[key] = {
                'destination': value['destination'],
                'group': value.get('group', None),
                'dlq': kafka_bindings.get(f"{key}.consumer", {}).get('enableDlq', False)
            }
        elif '-out' in key:
            producers[key] = {
                'destination': value['destination']
            }

    return app_name, producers, consumers

def display_kafka_links(app_name, producers, consumers):
    print(f"Application Name: {app_name}")
    print("\nProducers:")
    for key, value in producers.items():
        print(f"  - {key}:")
        print(f"    Destination: {value['destination']}")

    print("\nConsumers:")
    for key, value in consumers.items():
        print(f"  - {key}:")
        print(f"    Destination: {value['destination']}")
        print(f"    Group: {value['group']}")
        print(f"    Dead Letter Queue: {'Enabled' if value['dlq'] else 'Disabled'}")

    print("\nLinks:")
    for prod_key, prod_value in producers.items():
        for cons_key, cons_value in consumers.items():
            if prod_value['destination'] == cons_value['destination']:
                print(f"  - {prod_key} -> {cons_key}")



def parse_kafka_streams_properties(file_path):
    with open(file_path, 'r') as file:
        config = yaml.safe_load(file)

    app_name = config.get('spring', {}).get('application', {}).get('name', 'unknown-app')
    bindings = config.get('spring', {}).get('cloud', {}).get('stream', {}).get('kafka', {}).get('streams', {}).get('bindings', {})
    brokers = config.get('spring', {}).get('cloud', {}).get('stream', {}).get('kafka', {}).get('streams', {}).get('binder', {}).get('brokers', 'localhost:9092')

    producers = {}
    consumers = {}

    for key, value in bindings.items():
        if 'consumer' in value:
            consumers[key] = {
                'destination': value['consumer']['destination'],
                'group': value['consumer'].get('group', None)
            }
        elif 'producer' in value:
            producers[key] = {
                'destination': value['producer']['destination']
            }

    return app_name, brokers, producers, consumers

def display_kafka_streams_links(app_name, brokers, producers, consumers):
    print(f"Application Name: {app_name}")
    print(f"Brokers: {brokers}")
    print("\nProducers:")
    for key, value in producers.items():
        print(f"  - {key}:")
        print(f"    Destination: {value['destination']}")

    print("\nConsumers:")
    for key, value in consumers.items():
        print(f"  - {key}:")
        print(f"    Destination: {value['destination']}")
        print(f"    Group: {value['group']}")

    print("\nLinks:")
    for prod_key, prod_value in producers.items():
        for cons_key, cons_value in consumers.items():
            if prod_value['destination'] == cons_value['destination']:
                print(f"  - {prod_key} -> {cons_key}")

# Specify the path to your application.yaml file
file_path = 'application.yml'
app_name, brokers, producers, consumers = parse_kafka_streams_properties(file_path)
display_kafka_streams_links(app_name, brokers, producers, consumers)

# Specify the path to your application.yaml file

app_name, producers, consumers = parse_kafka_properties(file_path)
display_kafka_links(app_name, producers, consumers)
