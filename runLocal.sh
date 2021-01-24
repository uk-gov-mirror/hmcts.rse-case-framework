set -e

export IDAM_URI='http://localhost:8090/auth/realms/rse'
export NG_COMMAND='ng serve --host 0.0.0.0 --configuration local --proxy-config proxy.conf.dev.json'
docker-compose -p dev -f docker-compose.yml -f docker-compose.dev.yml up -d -V --no-deps --build db keycloak

# Backend toggleable
if [ "$#" -ne 1 ]; then
  ./gradlew java:backend:bootRun -is
fi
