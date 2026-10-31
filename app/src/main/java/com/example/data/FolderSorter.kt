package com.example.data

import com.example.model.EpisodeItem
import com.example.model.EpisodeSortOption
import com.example.model.FolderSortOption
import com.example.model.VideoFolderItem

object FolderSorter {

    fun sortFolders(
        folders: List<VideoFolderItem>,
        option: FolderSortOption,
        ascending: Boolean
    ): List<VideoFolderItem> {
        if (folders.size <= 1) return folders
        return folders.sortedWith { a, b ->
            val primary = when (option) {
                FolderSortOption.NAME -> {
                    if (ascending) {
                        MediaAutoMatcher.naturalCompare(a.name, b.name)
                    } else {
                        MediaAutoMatcher.naturalCompare(b.name, a.name)
                    }
                }

                FolderSortOption.LAST_PLAYED -> {
                    compareLongMetric(a.lastPlayedAt, b.lastPlayedAt, ascending)
                }

                FolderSortOption.DATE_MODIFIED -> {
                    compareLongMetric(a.lastModified, b.lastModified, ascending)
                }

                FolderSortOption.EPISODE_COUNT -> {
                    compareIntMetric(a.episodes.size, b.episodes.size, ascending)
                }

                FolderSortOption.TOTAL_SIZE -> {
                    compareLongMetric(a.totalSizeBytes, b.totalSizeBytes, ascending)
                }

                FolderSortOption.UNWATCHED -> {
                    compareIntMetric(a.unwatchedCount, b.unwatchedCount, ascending)
                }
            }

            if (primary != 0) {
                primary
            } else {
                MediaAutoMatcher.naturalCompare(a.name, b.name)
            }
        }
    }

    fun sortEpisodes(
        episodes: List<EpisodeItem>,
        option: EpisodeSortOption,
        ascending: Boolean
    ): List<EpisodeItem> {
        if (episodes.size <= 1) return episodes
        return episodes.sortedWith { a, b ->
            val primary = when (option) {
                EpisodeSortOption.EPISODE_NUMBER -> compareEpisodeSequence(a, b, ascending)
                EpisodeSortOption.DATE_MODIFIED -> compareLongMetric(a.lastModified, b.lastModified, ascending)
                EpisodeSortOption.FILE_SIZE -> compareLongMetric(a.sizeBytes, b.sizeBytes, ascending)
                EpisodeSortOption.DURATION -> compareLongMetric(a.durationMs, b.durationMs, ascending)
                EpisodeSortOption.RESOLUTION -> compareLongMetric(
                    a.effectivePixelCount,
                    b.effectivePixelCount,
                    ascending
                )
                EpisodeSortOption.LAST_PLAYED -> compareLongMetric(a.lastPlayedAt, b.lastPlayedAt, ascending)
            }
            if (primary != 0) {
                primary
            } else {
                compareEpisodeSequence(a, b, ascending = true)
            }
        }
    }

    private fun compareEpisodeSequence(a: EpisodeItem, b: EpisodeItem, ascending: Boolean): Int {
        val seasonA = a.seasonNumber
        val seasonB = b.seasonNumber
        if (seasonA != null && seasonB != null && seasonA != seasonB) {
            return compareIntMetric(seasonA, seasonB, ascending)
        }
        val epA = a.episodeNumber
        val epB = b.episodeNumber
        if (epA != null && epB != null && epA != epB) {
            return compareIntMetric(epA, epB, ascending)
        }
        return if (ascending) {
            MediaAutoMatcher.naturalCompare(a.fileName, b.fileName)
        } else {
            MediaAutoMatcher.naturalCompare(b.fileName, a.fileName)
        }
    }

    private fun compareLongMetric(a: Long, b: Long, ascending: Boolean): Int =
        if (ascending) a.compareTo(b) else b.compareTo(a)

    private fun compareIntMetric(a: Int, b: Int, ascending: Boolean): Int =
        if (ascending) a.compareTo(b) else b.compareTo(a)
}
