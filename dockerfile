# Use OpenJDK image
FROM openjdk:11-jre-slim

# Set the working directory
WORKDIR /app

# Copy the compiled classes into the container
COPY . /app

# Set the entry point to run the server
ENTRYPOINT ["java", "Server"]
