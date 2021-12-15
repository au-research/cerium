# Mycelium

## Testing
### Testing with docker

```
# build
echo "Building core and mycelium"
docker compose run --rm -w=/cerium mycelium mvn -pl core,mycelium clean install -Dmaven.test.skip=true

echo "Writing javadoc to target/site/apidocs"
docker compose run --rm -w=/cerium mycelium mvn -pl mycelium javadoc:javadoc

# start up the containers and provision SOLR
echo "Starting up containers"
docker compose up -d && docker compose exec solr wait-for-solr.sh && docker compose exec solr /bin/create-collections.sh

# run tests
if test -f "src/test/resources/application-integration.properties"; then
    echo "Executing unit + integration tests with Clover coverage"
    docker compose run --rm -w=/cerium mycelium mvn -pl mycelium org.openclover:clover-maven-plugin:4.4.1:setup verify org.openclover:clover-maven-plugin:4.4.1:aggregate org.openclover:clover-maven-plugin:4.4.1:clover --fail-never
else
    echo "Executing unit tests with Clover coverage"
    docker compose run --rm -w=/cerium mycelium mvn -pl mycelium org.openclover:clover-maven-plugin:4.4.1:setup test org.openclover:clover-maven-plugin:4.4.1:aggregate org.openclover:clover-maven-plugin:4.4.1:clover --fail-never
fi
```

for workstation that runs Apple Silicon chipset (ie. Apple M1) use the `docker-compose-m1.yml` file to bootstrap the test dependency containers. for .e.g 
```
docker compose -f docker-compose-m1.yml up -d
```