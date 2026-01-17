function buildMessage(payload) {
  var includeArtists = ArchiveTune.getBoolean("includeArtists", true);
  var includeAlbum = ArchiveTune.getBoolean("includeAlbum", false);
  var prefix = ArchiveTune.getString("prefix", "Now playing: ");

  var title = payload && payload.title ? payload.title : "";
  var artists = payload && payload.artists ? payload.artists : "";
  var album = payload && payload.album ? payload.album : "";

  var parts = [];
  if (title) parts.push(title);
  if (includeArtists && artists) parts.push(artists);
  if (includeAlbum && album) parts.push(album);

  var body = parts.join(" • ");
  if (!body) body = "Unknown track";

  return prefix + body;
}

function shouldRunFor(kind) {
  var enabled = ArchiveTune.getBoolean("enabled", true);
  if (!enabled) return false;
  var when = ArchiveTune.getString("when", "play");
  if (when === "both") return true;
  return when === kind;
}

function onTrackPlay(payload) {
  if (!shouldRunFor("play")) return;
  ArchiveTune.showToast(buildMessage(payload));
}

function onTrackPause(payload) {
  if (!shouldRunFor("pause")) return;
  ArchiveTune.showToast(buildMessage(payload));
}

