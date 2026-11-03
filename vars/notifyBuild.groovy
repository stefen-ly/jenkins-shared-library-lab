
def call(Map config = [:]) {

    def status = (
        config.status ?: currentBuild.currentResult
    ).toString()

    def emailTo = (
        config.emailTo ?: ''
    ).toString().trim()

    echo "===== Shared Library Notification ====="
    echo "Build Status: ${status}"

    // 1. Send Email
    if (emailTo) {
        try {
            emailext(
                to: emailTo,
                subject: "[Jenkins] ${status} - ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: """Jenkins Build Notification

Status: ${status}
Job: ${env.JOB_NAME}
Build: ${env.BUILD_NUMBER}
Build URL: ${env.BUILD_URL}
"""
            )

            echo 'Email notification submitted.'
        } catch (Exception ex) {
            echo 'Email notification failed.'
        }
    } else {
        echo 'Email skipped: no recipient.'
    }

    // 2. Send Telegram
    try {
        withCredentials([
            string(
                credentialsId: 'telegram-bot-token',
                variable: 'TELEGRAM_BOT_TOKEN'
            ),
            string(
                credentialsId: 'telegram-chat-id',
                variable: 'TELEGRAM_CHAT_ID'
            )
        ]) {
            withEnv(["NOTIFY_STATUS=${status}"]) {
                sh(
                    label: 'Shared Library Telegram',
                    script: '''
                        set +x

                        python3 - <<'PY'
import os
import json
import urllib.parse
import urllib.request

status = os.environ["NOTIFY_STATUS"]
job = os.environ["JOB_NAME"]
build = os.environ["BUILD_NUMBER"]

message = (
    f"Jenkins Build: {status}\\n"
    f"Job: {job}\\n"
    f"Build: #{build}"
)

token = os.environ["TELEGRAM_BOT_TOKEN"]
chat_id = os.environ["TELEGRAM_CHAT_ID"]

url = f"https://api.telegram.org/bot{token}/sendMessage"

data = urllib.parse.urlencode({
    "chat_id": chat_id,
    "text": message
}).encode("utf-8")

request = urllib.request.Request(
    url,
    data=data,
    method="POST"
)

with urllib.request.urlopen(request, timeout=15) as response:
    result = json.load(response)

if not result.get("ok"):
    raise RuntimeError("Telegram API rejected request")

print("Telegram notification accepted by API")
PY
                    '''
                )
            }
        }
    } catch (Exception ex) {
        echo 'Telegram notification failed.'
    }
}

