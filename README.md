This is a standard Spring Boot project created just to depict the bug with Spring AI M2

Steps:

1. Set up the credentials needed in application.properties
2. Upload a file for ETL to vector database, Postgres vector - local instance
3. Now go to the http://localhost:8080 via Postman or Browser and you will see there are no details of
token consumption being printed in the console.
4. On changing the version of Spring AI from M2 to M1 and redoing the step 3 one can find the token
consumption in the console.

