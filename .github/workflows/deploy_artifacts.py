from telethon import TelegramClient
import os
import subprocess

def get_git_commit_info():
    commit_author = subprocess.check_output(['git', 'log', '-1', '--pretty=format:%an']).decode('utf-8')
    commit_message = subprocess.check_output(['git', 'log', '-1', '--pretty=format:%s']).decode('utf-8')
    commit_hash = subprocess.check_output(['git', 'log', '-1', '--pretty=format:%H']).decode('utf-8')
    commit_hash_short = subprocess.check_output(['git', 'log', '-1', '--pretty=format:%h']).decode('utf-8')
    return commit_author, commit_message, commit_hash, commit_hash_short

# Telegram API credentials
api_id = int(os.getenv("API_ID"))
api_hash = os.getenv("API_HASH")
bot_token = os.getenv("BOT_TOKEN")
group_id = int(os.getenv("CHAT_ID"))

# File paths to send
apk_path = os.getenv("APK_PATH")

# Get the latest commit info
commit_author, commit_message, commit_hash, commit_hash_short = get_git_commit_info()

# Cleanup last session(if exists) before create client
session_file = "bot_session.session"
if os.path.exists(session_file):
    os.remove(session_file)

# Create the client with bot token directly
client = TelegramClient('bot_session', api_id, api_hash).start(bot_token=bot_token)
client.parse_mode = 'markdown'

def human_readable_size(size, decimal_places=2):
    for unit in ['B', 'KB', 'MB', 'GB', 'TB']:
        if size < 1024.0:
            break
        size /= 1024.0
    return f"{size:.{decimal_places}f} {unit}"


async def progress(current, total):
    progress_percentage = (current / total) * 100
    uploaded_size_readable = human_readable_size(current)
    total_size_readable = human_readable_size(total)
    print(f"{progress_percentage:.2f}% uploaded - {uploaded_size_readable}/{total_size_readable}", end='\r')


async def send_file(file_path):
    if not os.path.exists(file_path):
        print("File not found", file_path)
        return

    print(f"Sending file: {file_path} to the Telegram group")

    message = (
        f"**Commit by:** {commit_author}\n"
        f"**Commit message:** {commit_message}\n"
        f"**Commit hash:** #{commit_hash_short}\n"
        f"**Version:** Android >= 8"
    )

    try:
        await client.send_file(
            entity=group_id,
            file=file_path,
            parse_mode='markdown',
            caption=message,
            progress_callback=progress,
            reply_to=int(os.getenv("TOPIC_ID"))
        )
        print("\nFile sent successfully")
    except Exception as e:
        print(f"Failed to send file: {e}")

try:
    with client:
        client.loop.run_until_complete(send_file(apk_path))
finally:
    if client.is_connected():
        client.loop.run_until_complete(client.disconnect())
