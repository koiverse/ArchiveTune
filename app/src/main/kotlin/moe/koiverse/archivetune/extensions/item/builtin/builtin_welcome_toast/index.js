var __didToast = false;

function onLoad(api) {
  var enabled = api.getBoolean("enabled", true);
  if (!enabled) return;
  if (__didToast) return;
  __didToast = true;
  var message = api.getString("message", "ArchiveTune extensions are running");
  api.showToast(message);
}

