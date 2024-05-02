echo "docker-compose --env-file .env.dev up --detach --build"
#docker-compose --env-file .env.dev up --detach --build
docker-compose --env-file .env.local up --detach --build

mvn  package -Dmaven.test.skip=true -f ChamaPayments/pom.xml