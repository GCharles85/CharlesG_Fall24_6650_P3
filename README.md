todo do this
in one window: docker-compose build --no-cache,  docker-compose up --force-recreate --remove-orphans
in another: docker build -t client -f ./client/dockerfile ./client, docker run --name client --network charlesg_fall24_6650_p3_kv-network -it client