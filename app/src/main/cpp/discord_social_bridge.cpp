/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

#include <jni.h>
#include <android/log.h>

#include <algorithm>
#include <chrono>
#include <memory>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

#if ARCHIVETUNE_ENABLE_DISCORD_SOCIAL_SDK
#define DISCORDPP_IMPLEMENTATION
#include "discordpp.h"
#endif

namespace {

constexpr const char* kTag = "ArchiveTuneDiscord";
constexpr int kCallbackTimeoutMs = 8000;

std::string toString(JNIEnv* env, jstring value)
{
    if (value == nullptr) {
        return {};
    }

    const char* chars = env->GetStringUTFChars(value, nullptr);
    if (chars == nullptr) {
        return {};
    }

    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}

std::vector<std::string> toStringVector(JNIEnv* env, jobjectArray values)
{
    std::vector<std::string> result;
    if (values == nullptr) {
        return result;
    }

    const auto count = env->GetArrayLength(values);
    result.reserve(static_cast<size_t>(count));
    for (jsize index = 0; index < count; ++index) {
        auto item = static_cast<jstring>(env->GetObjectArrayElement(values, index));
        result.push_back(toString(env, item));
        env->DeleteLocalRef(item);
    }

    return result;
}

jstring error(JNIEnv* env, const std::string& message)
{
    return env->NewStringUTF(message.c_str());
}

jstring success()
{
    return nullptr;
}

#if ARCHIVETUNE_ENABLE_DISCORD_SOCIAL_SDK

std::mutex gMutex;
std::shared_ptr<discordpp::Client> gClient;
uint64_t gApplicationId = 0;
std::string gAccessToken;
bool gReady = false;
std::string gStatusError;

template <typename Predicate>
bool pumpUntil(Predicate predicate, int timeoutMs = kCallbackTimeoutMs)
{
    const auto deadline = std::chrono::steady_clock::now() + std::chrono::milliseconds(timeoutMs);
    while (!predicate()) {
        discordpp::RunCallbacks();
        if (std::chrono::steady_clock::now() >= deadline) {
            return predicate();
        }
        std::this_thread::sleep_for(std::chrono::milliseconds(16));
    }

    discordpp::RunCallbacks();
    return true;
}

discordpp::ActivityTypes toActivityType(int value)
{
    switch (value) {
        case 0:
            return discordpp::ActivityTypes::Playing;
        case 1:
            return discordpp::ActivityTypes::Streaming;
        case 2:
            return discordpp::ActivityTypes::Listening;
        case 3:
            return discordpp::ActivityTypes::Watching;
        case 5:
            return discordpp::ActivityTypes::Competing;
        default:
            return discordpp::ActivityTypes::Listening;
    }
}

discordpp::StatusDisplayTypes toStatusDisplayType(int value)
{
    switch (value) {
        case 0:
            return discordpp::StatusDisplayTypes::Name;
        case 2:
            return discordpp::StatusDisplayTypes::Details;
        default:
            return discordpp::StatusDisplayTypes::State;
    }
}

discordpp::StatusType toOnlineStatus(int value)
{
    switch (value) {
        case 3:
            return discordpp::StatusType::Idle;
        case 4:
            return discordpp::StatusType::Dnd;
        case 5:
            return discordpp::StatusType::Invisible;
        case 6:
            return discordpp::StatusType::Streaming;
        default:
            return discordpp::StatusType::Online;
    }
}

std::string ensureClientLocked(uint64_t applicationId, const std::string& accessToken)
{
    if (accessToken.empty()) {
        return "Discord access token is missing";
    }

    if (!gClient) {
        gClient = std::make_shared<discordpp::Client>();
        gClient->AddLogCallback(
            [](auto message, auto severity) {
                const int priority =
                    severity == discordpp::LoggingSeverity::Error ? ANDROID_LOG_ERROR :
                    severity == discordpp::LoggingSeverity::Warning ? ANDROID_LOG_WARN :
                    ANDROID_LOG_DEBUG;
                __android_log_write(priority, kTag, message.c_str());
            },
            discordpp::LoggingSeverity::Warning);

        gClient->SetStatusChangedCallback([](auto status, auto sdkError, auto details) {
            gReady = status == discordpp::Client::Status::Ready;
            if (sdkError != discordpp::Client::Error::None) {
                gStatusError = std::string("Discord Social SDK connection error: ") +
                    discordpp::Client::ErrorToString(sdkError) +
                    " (" + std::to_string(details) + ")";
            }
        });
    }

    if (gApplicationId != applicationId) {
        gClient->SetApplicationId(applicationId);
        gApplicationId = applicationId;
        gReady = false;
    }

    if (gAccessToken != accessToken) {
        bool tokenUpdated = false;
        std::string tokenError;
        gClient->UpdateToken(
            discordpp::AuthorizationTokenType::Bearer,
            accessToken,
            [&](discordpp::ClientResult result) {
                if (!result.Successful()) {
                    tokenError = result.ToString();
                }
                tokenUpdated = true;
            });

        if (!pumpUntil([&] { return tokenUpdated; })) {
            return "Timed out while updating Discord Social SDK token";
        }
        if (!tokenError.empty()) {
            return tokenError;
        }

        gAccessToken = accessToken;
        gReady = false;
    }

    if (!gReady) {
        gStatusError.clear();
        gClient->Connect();
        if (!pumpUntil([] { return gReady || !gStatusError.empty(); })) {
            return "Timed out while connecting Discord Social SDK";
        }
        if (!gStatusError.empty()) {
            return gStatusError;
        }
    }

    return {};
}

void setOnlineStatusLocked(int onlineStatus)
{
    bool statusDone = false;
    gClient->SetOnlineStatus(
        toOnlineStatus(onlineStatus),
        [&](discordpp::ClientResult result) {
            if (!result.Successful()) {
                __android_log_write(ANDROID_LOG_WARN, kTag, result.ToString().c_str());
            }
            statusDone = true;
        });
    pumpUntil([&] { return statusDone; }, 2000);
}

#endif

} // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_moe_koiverse_archivetune_discord_DiscordSocialNativeBridge_nativeIsSdkEnabled(
    JNIEnv*,
    jobject)
{
#if ARCHIVETUNE_ENABLE_DISCORD_SOCIAL_SDK
    return JNI_TRUE;
#else
    return JNI_FALSE;
#endif
}

extern "C" JNIEXPORT jstring JNICALL
Java_moe_koiverse_archivetune_discord_DiscordSocialNativeBridge_nativeStart(
    JNIEnv* env,
    jobject,
    jlong applicationId,
    jstring accessToken)
{
#if ARCHIVETUNE_ENABLE_DISCORD_SOCIAL_SDK
    std::lock_guard<std::mutex> lock(gMutex);
    const auto token = toString(env, accessToken);
    const auto failure = ensureClientLocked(static_cast<uint64_t>(applicationId), token);
    return failure.empty() ? success() : error(env, failure);
#else
    return error(env, "Discord Social SDK native bridge was built without discord_partner_sdk.aar");
#endif
}

extern "C" JNIEXPORT jstring JNICALL
Java_moe_koiverse_archivetune_discord_DiscordSocialNativeBridge_nativeUpdatePresence(
    JNIEnv* env,
    jobject,
    jlong applicationId,
    jstring accessToken,
    jint type,
    jstring name,
    jstring details,
    jstring state,
    jstring detailsUrl,
    jstring stateUrl,
    jstring largeImage,
    jstring largeText,
    jstring largeUrl,
    jstring smallImage,
    jstring smallText,
    jstring smallUrl,
    jobjectArray buttonLabels,
    jobjectArray buttonUrls,
    jlong startEpochSeconds,
    jlong endEpochSeconds,
    jint statusDisplayType,
    jint supportedPlatforms,
    jint onlineStatus)
{
#if ARCHIVETUNE_ENABLE_DISCORD_SOCIAL_SDK
    std::lock_guard<std::mutex> lock(gMutex);
    const auto token = toString(env, accessToken);
    const auto failure = ensureClientLocked(static_cast<uint64_t>(applicationId), token);
    if (!failure.empty()) {
        return error(env, failure);
    }

    discordpp::Activity activity;
    activity.SetApplicationId(static_cast<uint64_t>(applicationId));
    activity.SetType(toActivityType(type));

    const auto nameValue = toString(env, name);
    const auto detailsValue = toString(env, details);
    const auto stateValue = toString(env, state);
    const auto detailsUrlValue = toString(env, detailsUrl);
    const auto stateUrlValue = toString(env, stateUrl);

    if (!nameValue.empty()) {
        activity.SetName(nameValue);
    }
    if (!detailsValue.empty()) {
        activity.SetDetails(detailsValue);
    }
    if (!stateValue.empty()) {
        activity.SetState(stateValue);
    }
    if (!detailsUrlValue.empty()) {
        activity.SetDetailsUrl(detailsUrlValue);
    }
    if (!stateUrlValue.empty()) {
        activity.SetStateUrl(stateUrlValue);
    }

    discordpp::ActivityAssets assets;
    bool hasAssets = false;
    const auto largeImageValue = toString(env, largeImage);
    const auto largeTextValue = toString(env, largeText);
    const auto largeUrlValue = toString(env, largeUrl);
    const auto smallImageValue = toString(env, smallImage);
    const auto smallTextValue = toString(env, smallText);
    const auto smallUrlValue = toString(env, smallUrl);

    if (!largeImageValue.empty()) {
        assets.SetLargeImage(largeImageValue);
        hasAssets = true;
    }
    if (!largeTextValue.empty()) {
        assets.SetLargeText(largeTextValue);
        hasAssets = true;
    }
    if (!largeUrlValue.empty()) {
        assets.SetLargeUrl(largeUrlValue);
        hasAssets = true;
    }
    if (!smallImageValue.empty()) {
        assets.SetSmallImage(smallImageValue);
        hasAssets = true;
    }
    if (!smallTextValue.empty()) {
        assets.SetSmallText(smallTextValue);
        hasAssets = true;
    }
    if (!smallUrlValue.empty()) {
        assets.SetSmallUrl(smallUrlValue);
        hasAssets = true;
    }
    if (hasAssets) {
        activity.SetAssets(assets);
    }

    if (startEpochSeconds > 0 || endEpochSeconds > 0) {
        discordpp::ActivityTimestamps timestamps;
        if (startEpochSeconds > 0) {
            timestamps.SetStart(static_cast<uint64_t>(startEpochSeconds));
        }
        if (endEpochSeconds > 0) {
            timestamps.SetEnd(static_cast<uint64_t>(endEpochSeconds));
        }
        activity.SetTimestamps(timestamps);
    }

    const auto labels = toStringVector(env, buttonLabels);
    const auto urls = toStringVector(env, buttonUrls);
    const auto buttonCount = std::min<size_t>(2, std::min(labels.size(), urls.size()));
    for (size_t index = 0; index < buttonCount; ++index) {
        if (labels[index].empty() || urls[index].empty()) {
            continue;
        }
        discordpp::ActivityButton button;
        button.SetLabel(labels[index]);
        button.SetUrl(urls[index]);
        activity.AddButton(button);
    }

    activity.SetStatusDisplayType(toStatusDisplayType(statusDisplayType));
    activity.SetSupportedPlatforms(static_cast<discordpp::ActivityGamePlatforms>(supportedPlatforms));
    setOnlineStatusLocked(onlineStatus);

    bool updateDone = false;
    std::string updateError;
    gClient->UpdateRichPresence(
        activity,
        [&](discordpp::ClientResult result) {
            if (!result.Successful()) {
                updateError = result.ToString();
            }
            updateDone = true;
        });

    if (!pumpUntil([&] { return updateDone; })) {
        return error(env, "Timed out while updating Discord Rich Presence");
    }

    return updateError.empty() ? success() : error(env, updateError);
#else
    return error(env, "Discord Social SDK native bridge was built without discord_partner_sdk.aar");
#endif
}

extern "C" JNIEXPORT jstring JNICALL
Java_moe_koiverse_archivetune_discord_DiscordSocialNativeBridge_nativeClearPresence(
    JNIEnv* env,
    jobject)
{
#if ARCHIVETUNE_ENABLE_DISCORD_SOCIAL_SDK
    std::lock_guard<std::mutex> lock(gMutex);
    if (gClient) {
        gClient->ClearRichPresence();
        pumpUntil([] { return true; }, 250);
    }
    return success();
#else
    return error(env, "Discord Social SDK native bridge was built without discord_partner_sdk.aar");
#endif
}

extern "C" JNIEXPORT jstring JNICALL
Java_moe_koiverse_archivetune_discord_DiscordSocialNativeBridge_nativeClose(
    JNIEnv* env,
    jobject)
{
#if ARCHIVETUNE_ENABLE_DISCORD_SOCIAL_SDK
    std::lock_guard<std::mutex> lock(gMutex);
    if (gClient) {
        gClient->ClearRichPresence();
        gClient->Disconnect();
        pumpUntil([] { return true; }, 250);
        gClient.reset();
    }
    gApplicationId = 0;
    gAccessToken.clear();
    gReady = false;
    gStatusError.clear();
    return success();
#else
    return error(env, "Discord Social SDK native bridge was built without discord_partner_sdk.aar");
#endif
}

extern "C" JNIEXPORT jstring JNICALL
Java_moe_koiverse_archivetune_discord_DiscordSocialNativeBridge_nativeRunCallbacks(
    JNIEnv* env,
    jobject)
{
#if ARCHIVETUNE_ENABLE_DISCORD_SOCIAL_SDK
    std::lock_guard<std::mutex> lock(gMutex);
    discordpp::RunCallbacks();
    return success();
#else
    return error(env, "Discord Social SDK native bridge was built without discord_partner_sdk.aar");
#endif
}
