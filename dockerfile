# Use OpenJDK image
FROM openjdk:11-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the compiled classes into the container
COPY . /app

# Compile Java files
RUN javac Server.java

# Set the entry point to run the server
ENTRYPOINT ["java", "Server"]
