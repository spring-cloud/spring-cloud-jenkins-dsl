#! /bin/bash

set -e

echo "Running Jenkins"
docker-compose up -d

DOCKER_HOST=${DOCKER_HOST:-fake://localhost:fake}
JENKINS_PORT=${JENKINS_PORT:-8080}
HOST=`echo $DOCKER_HOST | cut -d ":" -f 2 | cut -d "/" -f 3`
JENKINS_ADDRESS="http://$HOST:$JENKINS_PORT"
WAIT_TIME="${WAIT_TIME:-10}"
RETRIES="${RETRIES:-10}"

echo "Jenkins host is $HOST, port is $JENKINS_PORT, address $JENKINS_ADDRESS"

mkdir -p build
wget -nc $JENKINS_ADDRESS/jnlpJars/jenkins-cli.jar -P build/

for i in $( seq 1 "${RETRIES}" ); do
    sleep "${WAIT_TIME}"
    java -jar build/jenkins-cli.jar -s $JENKINS_ADDRESS create-job spring-cloud-seed < seed/spring-cloud-seed/config.xml && break
    echo "Fail #$i/${RETRIES}... will try again in [${WAIT_TIME}] seconds"
done