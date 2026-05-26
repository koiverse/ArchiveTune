cmake_minimum_required(VERSION 3.22.1)

project(archivetune_discord_social)

add_library(archivetune_discord_social SHARED discord_social_bridge.cpp)

target_compile_features(archivetune_discord_social PRIVATE cxx_std_17)

target_link_libraries(archivetune_discord_social android log)

if(ARCHIVETUNE_ENABLE_DISCORD_SOCIAL_SDK)
    find_package(discord_partner_sdk REQUIRED CONFIG)
    target_compile_definitions(archivetune_discord_social PRIVATE ARCHIVETUNE_ENABLE_DISCORD_SOCIAL_SDK=1)
    target_link_libraries(archivetune_discord_social discord_partner_sdk::discord_partner_sdk)
else()
    target_compile_definitions(archivetune_discord_social PRIVATE ARCHIVETUNE_ENABLE_DISCORD_SOCIAL_SDK=0)
endif()
