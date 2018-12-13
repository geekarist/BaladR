package me.cpele.baladr.feature.playlist_display

import android.app.Application
import android.util.Log
import android.view.View
import androidx.lifecycle.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.cpele.baladr.R
import me.cpele.baladr.common.LiveEvent
import me.cpele.baladr.common.database.*

class PlaylistDisplayViewModel(
    private val app: Application,
    private val trackDao: TrackDao,
    private val playlistDao: PlaylistDao,
    private val playlistTrackDao: PlaylistTrackDao
) : AndroidViewModel(app) {

    class Factory(
        private val application: Application,
        private val trackDao: TrackDao,
        private val playlistDao: PlaylistDao,
        private val playlistTrackDao: PlaylistTrackDao
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return modelClass.cast(
                PlaylistDisplayViewModel(
                    application,
                    trackDao,
                    playlistDao,
                    playlistTrackDao
                )
            ) as T
        }
    }

    private val tempoData: MutableLiveData<Int> = MutableLiveData()

    private val connectivityStatusData = MutableLiveData<Boolean>()

    private val _isButtonEnabled: LiveData<Boolean> = Transformations.switchMap(connectivityStatusData) { status ->
        Transformations.map(tracksData) { tracks -> status && tracks.isNotEmpty() }
    }

    val isButtonEnabled: LiveData<Boolean>
        get() = _isButtonEnabled

    val tracksData: LiveData<List<TrackEntity>> = Transformations.switchMap(tempoData) { tempo ->
        trackDao.findByTempo(tempo)
    }

    val emptyViewVisibility: LiveData<Int> = Transformations.map(tracksData) {
        if (it.isEmpty()) View.VISIBLE else View.INVISIBLE
    }

    val recyclerViewVisibility: LiveData<Int> = Transformations.map(tracksData) {
        if (it.isNotEmpty()) View.VISIBLE else View.INVISIBLE
    }

    fun onPostTempo(newTempo: Int?) {
        tempoData.value = newTempo
    }

    private val _playlistSaveEvent = MutableLiveData<LiveEvent<PlaylistEntity>>()
    val playlistSaveEvent: LiveData<LiveEvent<PlaylistEntity>>
        get() = _playlistSaveEvent

    fun onConfirmSave(playlistName: String) {
        tracksData.value?.let { tracks ->
            GlobalScope.launch {
                try {
                    val notBlankName =
                        if (playlistName.isNotBlank()) playlistName
                        else app.getString(R.string.playlist_naming_default_title)
                    val playlist = PlaylistEntity(name = notBlankName)
                    val insertedPlaylistId = playlistDao.insert(playlist)
                    trackDao.insertAll(tracks)
                    val playlistTrackEntities = tracks.map {
                        PlaylistTrackEntity(insertedPlaylistId, it.id)
                    }
                    playlistTrackDao.insertAll(playlistTrackEntities)
                    _playlistSaveEvent.postValue(LiveEvent(playlist))
                } catch (e: Exception) {
                    Log.d(javaClass.simpleName, "Error saving playlist", e)
                }
            }
        }
    }

    fun onConnectivityChange(status: Boolean) {
        connectivityStatusData.postValue(status)
    }
}
