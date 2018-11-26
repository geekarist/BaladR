package me.cpele.baladr

import android.app.Application
import androidx.lifecycle.*

class PlaylistDisplayViewModel(application: Application, trackDao: TrackDao) : AndroidViewModel(application) {

    class Factory(
        private val application: Application,
        private val trackDao: TrackDao
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return modelClass.cast(PlaylistDisplayViewModel(application, trackDao)) as T
        }
    }

    private val tempoData: MutableLiveData<Int> = MutableLiveData()

    private val _isButtonEnabled = MutableLiveData<Boolean>()

    val isButtonEnabled
        get() = _isButtonEnabled

    val tracks: LiveData<List<TrackBo>> = Transformations.switchMap(tempoData) { tempo ->
        trackDao.findByTempo(tempo)
    }

    fun onPostTempo(newTempo: Int?) {
        tempoData.value = newTempo
    }

    fun onNetworkActive(isNetworkActive: Boolean?) {
        _isButtonEnabled.postValue(isNetworkActive == true)
    }
}
