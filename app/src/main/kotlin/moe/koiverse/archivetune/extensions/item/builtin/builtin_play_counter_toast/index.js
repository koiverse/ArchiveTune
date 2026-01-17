var __playCount = 0;

function onTrackPlay(payload) {
  var enabled = ArchiveTune.getBoolean("enabled", true);
  if (!enabled) return;

  var every = ArchiveTune.getInt("every", 10);
  if (every < 1) every = 1;

  __playCount = __playCount + 1;
  if (__playCount % every !== 0) return;

  var template = ArchiveTune.getString("message", "Played {count} tracks");
  var msg = String(template).split("{count}").join(String(__playCount));
  ArchiveTune.showToast(msg);
}

