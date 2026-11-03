
def call() {
    echo "========== Jenkins Build Info =========="
    echo "Job Name: ${env.JOB_NAME}"
    echo "Build Number: ${env.BUILD_NUMBER}"
    echo "Node Name: ${env.NODE_NAME}"
    echo "Workspace: ${env.WORKSPACE}"
    echo "========================================"
}

