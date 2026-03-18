package moe.koiverse.archivetune.playback

import androidx.media3.common.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Smart Shuffle Manager
 * 
 * Implements intelligent shuffle algorithm that:
 * - Distributes artists evenly throughout the queue
 * - Avoids playing the same artist consecutively
 * - Considers recently played tracks
 * - Maintains better listening flow
 * 
 * Inspired by Apple Music's smart shuffle algorithm
 * 
 * @author @cenzer0
 */
class SmartShuffleManager {
    
    private val recentlyPlayedIds = mutableSetOf<String>()
    private val maxRecentlyPlayed = 20
    
    /**
     * Performs smart shuffle on a list of media items
     * 
     * Algorithm:
     * 1. Group tracks by artist
     * 2. Shuffle within each artist group
     * 3. Distribute artists evenly across the queue
     * 4. Avoid consecutive same-artist plays
     * 5. Deprioritize recently played tracks
     * 
     * Time complexity: O(n log n)
     * Space complexity: O(n)
     */
    suspend fun smartShuffle(
        items: List<MediaItem>,
        currentIndex: Int = -1
    ): List<MediaItem> = withContext(Dispatchers.Default) {
        if (items.size <= 1) return@withContext items
        
        // Keep current item at the beginning if specified
        val currentItem = if (currentIndex >= 0 && currentIndex < items.size) {
            items[currentIndex]
        } else null
        
        val itemsToShuffle = if (currentItem != null) {
            items.filter { it.mediaId != currentItem.mediaId }
        } else items
        
        if (itemsToShuffle.isEmpty()) return@withContext items
        
        // Group by artist
        val artistGroups = itemsToShuffle.groupBy { item ->
            extractArtist(item)
        }
        
        // Shuffle within each artist group
        val shuffledGroups = artistGroups.mapValues { (_, tracks) ->
            tracks.shuffled()
        }
        
        // Distribute artists evenly
        val result = distributeArtistsEvenly(shuffledGroups)
        
        // Apply recently played penalty
        val finalResult = applyRecentlyPlayedPenalty(result)
        
        // Add current item at the beginning if it exists
        if (currentItem != null) {
            listOf(currentItem) + finalResult
        } else {
            finalResult
        }
    }
    
    /**
     * Distributes artists evenly throughout the queue
     * to avoid consecutive plays from the same artist
     */
    private fun distributeArtistsEvenly(
        artistGroups: Map<String, List<MediaItem>>
    ): List<MediaItem> {
        val result = mutableListOf<MediaItem>()
        val artistQueues = artistGroups.mapValues { it.value.toMutableList() }.toMutableMap()
        
        // Calculate how many tracks each artist should contribute per round
        val totalTracks = artistQueues.values.sumOf { it.size }
        val artistCount = artistQueues.size
        
        // Distribute tracks round-robin style
        while (artistQueues.isNotEmpty()) {
            // Get artists sorted by remaining track count (descending)
            val sortedArtists = artistQueues.entries
                .sortedByDescending { it.value.size }
                .map { it.key }
            
            // Take one track from each artist in this round
            for (artist in sortedArtists) {
                val queue = artistQueues[artist] ?: continue
                
                if (queue.isNotEmpty()) {
                    // Avoid consecutive same artist if possible
                    val track = if (result.isNotEmpty() && 
                                   extractArtist(result.last()) == artist &&
                                   queue.size > 1) {
                        // Try to pick a different track from this artist
                        queue.removeAt(Random.nextInt(queue.size))
                    } else {
                        queue.removeAt(0)
                    }
                    
                    result.add(track)
                }
                
                // Remove artist if no more tracks
                if (queue.isEmpty()) {
                    artistQueues.remove(artist)
                }
            }
        }
        
        return result
    }
    
    /**
     * Applies penalty to recently played tracks by moving them
     * towards the end of the queue
     */
    private fun applyRecentlyPlayedPenalty(items: List<MediaItem>): List<MediaItem> {
        if (recentlyPlayedIds.isEmpty()) return items
        
        val recentlyPlayed = mutableListOf<MediaItem>()
        val notRecentlyPlayed = mutableListOf<MediaItem>()
        
        items.forEach { item ->
            if (recentlyPlayedIds.contains(item.mediaId)) {
                recentlyPlayed.add(item)
            } else {
                notRecentlyPlayed.add(item)
            }
        }
        
        // Put recently played tracks at the end
        return notRecentlyPlayed + recentlyPlayed.shuffled()
    }
    
    /**
     * Extracts artist name from media item
     */
    private fun extractArtist(item: MediaItem): String {
        return item.mediaMetadata.artist?.toString() ?: "Unknown Artist"
    }
    
    /**
     * Marks a track as recently played
     */
    fun markAsPlayed(mediaId: String) {
        recentlyPlayedIds.add(mediaId)
        
        // Keep only the most recent tracks
        if (recentlyPlayedIds.size > maxRecentlyPlayed) {
            val toRemove = recentlyPlayedIds.size - maxRecentlyPlayed
            recentlyPlayedIds.drop(toRemove).forEach { 
                recentlyPlayedIds.remove(it) 
            }
        }
    }
    
    /**
     * Clears recently played history
     */
    fun clearRecentlyPlayed() {
        recentlyPlayedIds.clear()
    }
    
    /**
     * Gets the number of recently played tracks
     */
    fun getRecentlyPlayedCount(): Int = recentlyPlayedIds.size
}
