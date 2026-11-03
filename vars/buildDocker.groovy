
def call(Map config = [:]) {

    String project = (
        config.project ?: 'fullstack-shared'
    ).toString()

    String service = (
        config.service ?: 'backend'
    ).toString()

    String artifactPath = (
        config.artifactPath ?: ''
    ).toString()

    String outputFile = (
        config.outputFile ?: ''
    ).toString()

    echo "===== Shared Library: Docker Build ====="
    echo "Compose Project: ${project}"
    echo "Service: ${service}"

    withEnv([
        "LIB_COMPOSE_PROJECT=${project}",
        "LIB_SERVICE=${service}"
    ]) {

        sh '''
            set -eu

            echo "Building Docker image..."

            docker compose \
                -p "$LIB_COMPOSE_PROJECT" \
                build "$LIB_SERVICE"

            IMAGE="${LIB_COMPOSE_PROJECT}-${LIB_SERVICE}:latest"

            docker image inspect "$IMAGE" > /dev/null

            echo "Docker image build SUCCESS!"
        '''

        // Optional: Extract artifact from image
        if (artifactPath && outputFile) {

            withEnv([
                "LIB_ARTIFACT_PATH=${artifactPath}",
                "LIB_OUTPUT_FILE=${outputFile}"
            ]) {

                sh '''
                    set -eu

                    IMAGE="${LIB_COMPOSE_PROJECT}-${LIB_SERVICE}:latest"

                    mkdir -p "$(dirname "$LIB_OUTPUT_FILE")"

                    CID=$(docker create "$IMAGE")

                    trap 'docker rm -f "$CID" >/dev/null 2>&1 || true' EXIT

                    docker cp \
                        "$CID:$LIB_ARTIFACT_PATH" \
                        "$LIB_OUTPUT_FILE"

                    test -s "$LIB_OUTPUT_FILE"

                    echo "Artifact extracted successfully!"

                    ls -lh "$LIB_OUTPUT_FILE"
                '''
            }
        }
    }
}

