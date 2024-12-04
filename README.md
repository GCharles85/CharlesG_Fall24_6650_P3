# PAXOS upgrades: 
# Non-proposer servers set to automatically terminate at regular intervals
# Proposers does not ask learners if they can commit, only acceptors. Learners can only #                 # commit but cannot participate in the voting process (refer to canCommit() function in HandleRequests.java - line 187)
# I pass the sequence number from the client to the PAXOS nodes on line 44 of the client implementation NetworkClient.java
# Added functionality to check sequence number (refer to sendRequestToAllOtherServers(), line 137 of HandleRequest.java)

# Project Setup Instructions

This guide will walk you through the steps to set up and run the project using Docker. Follow the steps below carefully:

## Prerequisites
1. Install [Docker](https://www.docker.com/products/docker-desktop) if you haven't already.
   - Ensure both Docker and Docker Compose are installed and running.

## Setup Instructions

### Step 1: Build and Run Docker Services
1. Open a terminal window. You will see the requests' results here.
2. Run the following commands to build and start the Docker services:
   docker-compose build --no-cache
   docker-compose up --force-recreate --remove-orphans

### Step 2: Set Up the Client
1. Open a second terminal window.
2. Remove any existing client container (if applicable):
   docker rm client

3. Build the client container:
   docker build -t client -f ./client/dockerfile ./client

4. Run the client container:
   docker run --name client --network charlesg_fall24_6650_p3_kv-network -it client

### Step 3: Interact with the Application
1. Follow the prompts displayed in the client terminal window to interact with the application.

## Common Commands
- Stop all Docker services:
  docker-compose down

- Check running containers:
  docker ps
  
- View logs:
  docker-compose logs

## Troubleshooting
- If you encounter errors like `container name already in use`, ensure the container is removed by running:
  docker rm client

- Restart the `docker-compose` services if changes are made:
  docker-compose down
  docker-compose up --force-recreate


