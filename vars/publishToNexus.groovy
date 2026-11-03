
def call(Map config = [:]) {

    String nexusUrl = (
        config.nexusUrl ?: 'http://127.0.0.1:8081'
    ).toString()

    String repository = (
        config.repository ?: 'devops-releases'
    ).toString()

    String artifactFile = (
        config.artifactFile ?: 'publish/task-api.jar'
    ).toString()

    String version = (
        config.version ?: ''
    ).toString()

    String groupId = (
        config.groupId ?: 'com.example'
    ).toString()

    String artifactId = (
        config.artifactId ?: 'task-api'
    ).toString()

    if (!version) {
        error 'Artifact version is required!'
    }

    echo "===== Shared Library: Nexus Publish ====="
    echo "Artifact: ${artifactId}"
    echo "Version: ${version}"
    echo "Repository: ${repository}"

    withEnv([
        "LIB_NEXUS_URL=${nexusUrl}",
        "LIB_NEXUS_REPO=${repository}",
        "LIB_ARTIFACT_FILE=${artifactFile}",
        "LIB_VERSION=${version}",
        "LIB_GROUP_ID=${groupId}",
        "LIB_ARTIFACT_ID=${artifactId}"
    ]) {

        withCredentials([
            usernamePassword(
                credentialsId: 'nexus-credentials',
                usernameVariable: 'NEXUS_USER',
                passwordVariable: 'NEXUS_PASS'
            )
        ]) {

            sh '''
                set +x
                set -eu

                test -s "$LIB_ARTIFACT_FILE"

                SETTINGS=$(mktemp)
                trap 'rm -f "$SETTINGS"' EXIT
                chmod 600 "$SETTINGS"

                cat > "$SETTINGS" <<'EOF'
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0">
    <servers>
        <server>
            <id>nexus-releases</id>
            <username>${env.NEXUS_USER}</username>
            <password>${env.NEXUS_PASS}</password>
        </server>
    </servers>
</settings>
EOF

                mkdir -p "$WORKSPACE/.m2/repository"

                mvn -B -ntp \
                    -s "$SETTINGS" \
                    -Dmaven.repo.local="$WORKSPACE/.m2/repository" \
                    org.apache.maven.plugins:maven-deploy-plugin:3.2.0:deploy-file \
                    -DrepositoryId=nexus-releases \
                    -Durl="$LIB_NEXUS_URL/repository/$LIB_NEXUS_REPO/" \
                    -Dfile="$LIB_ARTIFACT_FILE" \
                    -DgroupId="$LIB_GROUP_ID" \
                    -DartifactId="$LIB_ARTIFACT_ID" \
                    -Dversion="$LIB_VERSION" \
                    -Dpackaging=jar \
                    -DgeneratePom=true

                echo "Nexus publishing SUCCESS!"
            '''
        }
    }
}

