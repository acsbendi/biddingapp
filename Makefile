IMAGE_NAME ?= biddingapp/biddingapp
CONTAINER_NAME ?= biddingapp

deploy-maven:
	mvn package
	java -jar target/biddingapp-1.0-SNAPSHOT.jar db migrate biddingapp.yml
	java -jar target/biddingapp-1.0-SNAPSHOT.jar server biddingapp.yml

deploy-docker:
	make docker-build && make docker-run

docker-build:
	docker build -t $(IMAGE_NAME):latest .

docker-run:
	docker run -d -p 8080:8080 --name $(CONTAINER_NAME) $(IMAGE_NAME):latest

test:
	mvn test
