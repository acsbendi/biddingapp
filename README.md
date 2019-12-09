## Design decisions
The following list contains the major design decisions I made during the project, along with their justification. Some of these were not explicitly and unambiguously laid out in the specification, others were, but I decided to use a different implementation for the good of the project.
* There was some ambiguity regarding the type of the `"bidId"` JSON field in the specification: in the **Resources** section, it was shown as a number (`1`), but in the **Request/response examples** section, it's used as a string (`"1"`). I decided to resolve this ambiguity by consistently using a number, because it makes more sense considering that the actual content of the field is always a numerical ID (`1` or `"1"`) in the specification.
* The last example in the specification shows that a `201 Created` response is returned for a `GET` request. I think it's better to return a `200 OK` for this operation, since it does not create anything.
* Since the specification mentions that it's a real-time app, I tried to focus on performance. This resulted in some less obvious implementation solutions, for example, I run database queries directly instead of making use of the Hibernate ORM layer (see [this `UPDATE`](https://github.com/acsbendi/biddingapp/blob/db55096bcca5df6c7efff03941877ca723793d06/src/main/java/com/bendeguz/biddingapp/core/Campaign.java#L19) query for a specific example).
* For simplicity, I used an H2 database which was shown in the [Dropwizard example](https://github.com/dropwizard/dropwizard/blob/184dadf82319ab4c6dc3237ddc303114e89c086c/dropwizard-example/example.yml#L6).

## Deployment
You can deploy the service using one of the following commands:
* `make`: this will build a single jar file and run it. Requirements: Maven.
* `make deploy-docker`: this will build a Docker image and start the service in a container. Requirements: Docker.

If you'd like to avoid make, you have to execute the following commands seperately:
* For Maven: 
1. `mvn package`
1. `java -jar target/biddingapp-1.0-SNAPSHOT.jar db migrate biddingapp.yml`
1. `java -jar target/biddingapp-1.0-SNAPSHOT.jar server biddingapp.yml`
* For Docker: 
1. `docker build -t biddingapp/biddingapp:latest .`
2. `docker run -d -p 8080:8080 --name biddingapp biddingapp/biddingapp:latest`

## Testing
Tests can be run by executing `make test` or `mvn test`.
