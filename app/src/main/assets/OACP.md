# ArchiveTune — OACP Context

## What this app does
YouTube Music streaming client for Android. Searches and plays music from YouTube Music's catalog over the internet. Supports background playback, lyrics, playlists, albums, and artist browsing.

## Capabilities
- play_music: Searches YouTube Music and starts playback; takes optional query (song/album) and artist parameters
- pause_music: Pauses the current playback
- next_track: Skips to the next track in the queue
- previous_track: Goes back to the previous track
- toggle_shuffle: Toggles shuffle mode on/off
- toggle_repeat: Cycles repeat mode (off/repeat all/repeat one)
- toggle_like: Likes or unlikes the currently playing track

## Disambiguation
- "play music/song/album" → play_music
- "play [artist name]" → play_music (with query parameter)
- "play [song name]" → play_music (with query parameter)
- "pause/stop music" → pause_music
- "next/skip song" → next_track
- "previous/back/last song" → previous_track
- "shuffle" → toggle_shuffle
- "repeat/loop" → toggle_repeat
- "like/heart/favorite this" → toggle_like
- This is a STREAMING music player (YouTube Music) — do NOT confuse with local-only players like Auxio
- Requires internet connection to search and stream music
