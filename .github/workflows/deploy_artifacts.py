from telethon import TelegramClient
import os
import subprocess
import glob

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

# File path pattern(s) to send
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


def resolve_apk_paths(path_value):
    if not path_value:
        return []
    patterns = [item.strip() for item in path_value.split(";") if item.strip()]
    if not patterns:
        patterns = [path_value.strip()]
    resolved = []
    for pattern in patterns:
        matched = glob.glob(pattern, recursive=True)
        if matched:
            resolved.extend(matched)
            continue
        if os.path.isdir(pattern):
            resolved.extend(glob.glob(os.path.join(pattern, "*.apk")))
    unique_files = []
    seen = set()
    for path in resolved:
        normalized = os.path.normpath(path)
        if normalized in seen or not os.path.isfile(normalized):
            continue
        seen.add(normalized)
        unique_files.append(normalized)
    return unique_files


def extract_abi_name(file_path):
    file_name = os.path.basename(file_path).lower()
    known_abis = [
        "arm64",
        "armeabi",
        "x86_64",
        "x86"
        "universal"
    ]
    for abi in known_abis:
        if abi in file_name:
            return abi
    return "universal"


async def send_files(file_paths):
    existing_files = [path for path in file_paths if os.path.exists(path)]
    if not existing_files:
        print("No valid files to send")
        return

    print(f"Sending {len(existing_files)} files to the Telegram group")
    abi_names = sorted({extract_abi_name(path) for path in existing_files})
    topic_id = os.getenv("TOPIC_ID")

    message = (
        f"**Commit by:** {commit_author}\n"
        f"**Commit message:** {commit_message}\n"
        f"**Commit hash:** #{commit_hash_short}\n"
        f"**ABI:** {', '.join(abi_names)}\n"
        f"**Files:** {len(existing_files)}\n"
        f"**Version:** Android >= 8"
    )

    try:
        send_kwargs = {
            "entity": group_id,
            "file": existing_files,
            "parse_mode": "markdown",
            "caption": message,
            "progress_callback": progress
        }
        if topic_id:
            send_kwargs["reply_to"] = int(topic_id)
        await client.send_file(
            **send_kwargs
        )
        print("\nFiles sent successfully")
    except Exception as e:
        print(f"Failed to send files: {e}")

try:
    apk_files = resolve_apk_paths(apk_path)
    if not apk_files:
        print("File not found", apk_path)
        raise SystemExit(1)
    with client:
        client.loop.run_until_complete(send_files(apk_files))
finally:
    if client.is_connected():
        client.loop.run_until_complete(client.disconnect())
