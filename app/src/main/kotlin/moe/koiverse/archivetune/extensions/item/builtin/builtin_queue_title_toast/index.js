function onQueueBuild(payload) {
  var enabled = ArchiveTune.getBoolean("enabled", true);
  if (!enabled) return;

  var prefix = ArchiveTune.getString("prefix", "Queue: ");
  var showWhenEmpty = ArchiveTune.getBoolean("showWhenEmpty", false);

  var title = payload && payload.title ? payload.title : "";
  if (!title && !showWhenEmpty) return;
  if (!title) title = "Untitled";

  ArchiveTune.showToast(prefix + title);
}

