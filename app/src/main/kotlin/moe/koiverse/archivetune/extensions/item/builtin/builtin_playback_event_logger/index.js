function isEnabled() {
  return ArchiveTune.getBoolean("enabled", true);
}

function prefix() {
  return ArchiveTune.getString("prefix", "[Extension]");
}

function safeString(v) {
  if (v === null || v === undefined) return "";
  return String(v);
}

function onTrackPlay(payload) {
  if (!isEnabled()) return;
  if (!ArchiveTune.getBoolean("logTrackPlay", true)) return;
  var title = payload && payload.title ? payload.title : "";
  var artists = payload && payload.artists ? payload.artists : "";
  ArchiveTune.log(prefix() + " onTrackPlay title=" + safeString(title) + " artists=" + safeString(artists));
}

function onTrackPause(payload) {
  if (!isEnabled()) return;
  if (!ArchiveTune.getBoolean("logTrackPause", true)) return;
  var title = payload && payload.title ? payload.title : "";
  ArchiveTune.log(prefix() + " onTrackPause title=" + safeString(title));
}

function onQueueBuild(payload) {
  if (!isEnabled()) return;
  if (!ArchiveTune.getBoolean("logQueueBuild", true)) return;
  var title = payload && payload.title ? payload.title : "";
  ArchiveTune.log(prefix() + " onQueueBuild title=" + safeString(title));
}

