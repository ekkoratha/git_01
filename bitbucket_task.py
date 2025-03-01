#***********************************************************************
# This code is about a AI Agent which checked bitbucket code to find the 
# input and outputs kafka topic from Springboot code
#***********************************************************************

# requirements.txt
#------------------
# phidata
# python-dotenv
# yfinance
# packaging
# duckduckgo-search
# fastapi
# uvicorn
# groq
# openai

# OPENAI_API_KEY" environment key with my Open AI Key setup

# code cloned under specific folder
# clone_dir = "C:\\Work\\kk\\sample-spring-cloud-stream-kafka"

import os
import json
import re
import openai  # or deepseek API
from dotenv import load_dotenv
from openai import OpenAI

load_dotenv()

# Set your OpenAI API key
openai.api_key = os.getenv("OPENAI_API_KEY")  # Replace with your actual OpenAI API key

# Initialize the OpenAI client
client = OpenAI(api_key=os.getenv("OPENAI_API_KEY") )  # Replace with your actual OpenAI API key

def clone_repository(repo_url, clone_dir):
    """
    Clone a Bitbucket repository to a local directory.
    """
    if not os.path.exists(clone_dir):
        os.system(f"git clone {repo_url} {clone_dir}")
    else:
        print(f"Directory {clone_dir} already exists. Skipping clone.")

def find_files(directory, file_patterns):
    """
    Recursively find files matching specific patterns in a directory.
    """
    matched_files = []
    for root, _, files in os.walk(directory):
        for file in files:
            for pattern in file_patterns:
                if re.search(pattern, file):
                    matched_files.append(os.path.join(root, file))
    return matched_files

def ask_openai(prompt):
    """
    Send a prompt to OpenAI and return the response.
    """
    response = client.chat.completions.create(
        model="gpt-4",  # Use GPT-4 for better analysis
        messages=[
            {"role": "system", "content": "You are a helpful assistant that analyzes Spring Boot and Kafka code to identify Kafka topics."},
            {"role": "user", "content": prompt}
        ],
        max_tokens=500
    )
    return response.choices[0].message.content



def extract_kafka_topics_with_openai(file_content, file_type):
    """
    Use OpenAI to analyze file content and extract Kafka topics.
    """
    if file_type == "config":
        prompt = f"""
        Analyze the following Spring Boot configuration file and identify the Kafka input and output topics:
        {file_content}
        Return the topics in the following JSON format:
        {{
          "input_topics": ["topic1", "topic2"],
          "output_topics": ["topic3"]
        }}
        """
    elif file_type == "code":
        prompt = f"""
        Analyze the following Java code and identify the Kafka input and output topics:
        {file_content}
        Return the topics in the following JSON format:
        {{
          "input_topics": ["topic1", "topic2"],
          "output_topics": ["topic3"]
        }}
        """
    else:
        return {"input_topics": [], "output_topics": []}

    response = ask_openai(prompt)
    try:
        # Extract JSON portion from the response using regex
        json_match = re.search(r"\{.*\}", response, re.DOTALL)
        print (response)
        if json_match:
            json_str = json_match.group(0)
            return json.loads(json_str)
        else:
            print("No JSON found in OpenAI response.")
            return {"input_topics": [], "output_topics": []}
    except json.JSONDecodeError as e:
        print(f"Failed to parse OpenAI response as JSON: {e}")
        return {"input_topics": [], "output_topics": []}

def process_project(project_path):
    """
    Process a Spring Boot project to find Kafka-related configurations and code.
    """
    # Step 1: Find relevant files
    config_files = find_files(project_path, [r"application\.yml", r"application\.properties"])
    kafka_code_files = find_files(project_path, [r"KafkaConsumer\.java", r"KafkaProducer\.java", r"KafkaConfig\.java"])

    # Step 2: Extract Kafka topics using OpenAI
    input_topics = set()
    output_topics = set()
    for config_file in config_files:
        with open(config_file, "r") as file:
            content = file.read()
            result = extract_kafka_topics_with_openai(content, "config")
            input_topics.update(result.get("input_topics", []))
            output_topics.update(result.get("output_topics", []))

    for code_file in kafka_code_files:
        with open(code_file, "r") as file:
            content = file.read()
            result = extract_kafka_topics_with_openai(content, "code")
            input_topics.update(result.get("input_topics", []))
            output_topics.update(result.get("output_topics", []))

    # Step 3: Convert sets to lists
    return list(input_topics), list(output_topics)

def generate_json(project_name, input_topics, output_topics):
    """
    Generate JSON output for a project.
    """
    return {
        "project_name": project_name,
        "input_kafka_topics": input_topics,
        "output_kafka_topics": output_topics
    }

def main():
    # Step 1: Clone the repository
    repo_url = "https://bitbucket.org/your-username/your-repo.git"  # Replace with your Bitbucket repo URL
    clone_dir = "C:\\Work\\kk\\sample-spring-cloud-stream-kafka"
    clone_repository(repo_url, clone_dir)

    # Step 2: Process each project in the repository
    projects = [d for d in os.listdir(clone_dir) if os.path.isdir(os.path.join(clone_dir, d))]
    all_results = []
    for project in projects:
        project_path = os.path.join(clone_dir, project)
        input_topics, output_topics = process_project(project_path)
        json_output = generate_json(project, input_topics, output_topics)
        all_results.append(json_output)

    # Step 3: Print or save the JSON output
    print(json.dumps(all_results, indent=2))

if __name__ == "__main__":
    main()

# OUTPUT
# [Running] python -u "c:\Work\LLM\test\bitbucket_task.py"
# Directory C:\Work\kk\sample-spring-cloud-stream-kafka already exists. Skipping clone.
# In the provided Spring Boot configuration file, it appears that we don't have any input Kafka topics defined. However, we have two output Kafka topics which are `orders.buy` and `orders.sell`.

# So, based on the provided configuration, the JSON representation of Kafka topics would be:

# ```json
# {
#   "input_topics": [],
#   "output_topics": ["orders.buy", "orders.sell"]
# }
# ```
# The input topics in this Spring Boot configuration file are "pages" and "visits". The output topic is "page.visits". 

# Represented in JSON format:

# ```json
# {
#     "input_topics": ["pages", "visits"], 
#     "output_topics": ["page.visits"]
# }
# ```
# Here is the analysis of the given Spring Boot configuration file:

# The input topics are: "orders.buy", "orders.sell", and "transactions". These topics are identified from the various 'destination' properties under the 'bindings' tag for the functions with '-in-' in their names.

# The output topic is: "transactions". This topic is recognized from the 'destination' properties under the 'bindings' tag for the function with '-out-' in its name. 

# Return the topics in the requested JSON format:

# ```json
# {
#   "input_topics": ["orders.buy", "orders.sell", "transactions"],
#   "output_topics": ["transactions"]
# }
# ```
# [
#   {
#     "project_name": "order-service",
#     "input_kafka_topics": [],
#     "output_kafka_topics": [
#       "orders.sell",
#       "orders.buy"
#     ]
#   },
#   {
#     "project_name": "spring-cloud-stream-with-kafka-streams-join-example-master",
#     "input_kafka_topics": [
#       "pages",
#       "visits"
#     ],
#     "output_kafka_topics": [
#       "page.visits"
#     ]
#   },
#   {
#     "project_name": "stock-service",
#     "input_kafka_topics": [
#       "transactions",
#       "orders.sell",
#       "orders.buy"
#     ],
#     "output_kafka_topics": [
#       "transactions"
#     ]
#   }
# ]
