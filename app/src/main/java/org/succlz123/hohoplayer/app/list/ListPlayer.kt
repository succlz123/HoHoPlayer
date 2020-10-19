package org.succlz123.hohoplayer.app.list

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.succlz123.hohoplayer.config.PlayerCacher
import org.succlz123.hohoplayer.config.PlayerContext
import org.succlz123.hohoplayer.core.adapter.event.OnAdapterEventListener
import org.succlz123.hohoplayer.core.player.listener.OnPlayerEventListener
import org.succlz123.hohoplayer.core.render.AspectRatio
import org.succlz123.hohoplayer.core.source.DataSource
import org.succlz123.hohoplayer.support.message.HoHoMessage
import org.succlz123.hohoplayer.widget.helper.VideoPlayHelperAdapterEventHandler
import org.succlz123.hohoplayer.widget.helper.VideoPlayerHelper

/**
 *  Option 1
 *
 *  1. New List Player
 *  listPlayer = ListPlayer(requireContext())
 *
 *  2. Bind LifecycleOwner
 *  listPlayer.bindLifecycleOwner(viewLifecycleOwner)
 *
 *  3. Play video directly
 *  val dataSource = DataSource(entry.immersiveVideo?.url.orEmpty())
 *  bottomPlayer.play(dataSource, mBinding.layoutContainer, mBinding.image, 0)
 *
 *  4. Bind RecyclerView
 *  listPlayer.bindRecyclerView(mBinding.recyclerView, 0)
 *
 *
 *  Option 2
 *
 *  1. New List Player
 *  listPlayer = ListPlayer(requireContext())
 *
 *  2. Bind LifecycleOwner
 *  listPlayer.bindLifecycleOwner(viewLifecycleOwner)
 *
 *  3. VH Setting
 *  class xxxVH: ViewHolder, ListPlayerViewHolder {
 *      var videoUrl: String? = null
 *      ......
 *  }
 *
 *  4.1 Don't forget to recycle VH
 *  val adapter = xxAdapter(1, 2, 3) {
 *      listPlayer.handleViewRecycled(it)
 *  }
 *  4.2 or recycle view on adapter
 *  class xxAdapter: RecyclerView.Adapter {
 *
 *      override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
 *          if (holder is xxVH) {
 *              if (!holder.videoUrl.isNullOrEmpty()) {
 *                  holder.videoUrl = null
 *                  recyclerCb.invoke(holder.dataBinding.layoutContainer)
 *              }
 *          }
 *      }
 *  }
 */
class ListPlayer(var context: Context) : LifecycleObserver {
    private val videoPlayerHelper = VideoPlayerHelper(PlayerContext.context())

    private var lifecycleOwner: LifecycleOwner? = null

    private var outRecyclerView: RecyclerView? = null

    private var currentViewGroup: ViewGroup? = null
    private var currentImageView: View? = null
    private var currentPos: Int = -1

    private var verticalRecyclerStart = 0
    private var thresholdTop = 0
    private var thresholdBottom = 0

    private var adapter: RecyclerView.Adapter<*>? = null
    private var dataObserver: RecyclerView.AdapterDataObserver? = null
    private var scrollListener: RecyclerView.OnScrollListener? = null

    private var controllerCoverAdapter: ControllerCoverAdapter? = null

    var isMute: Boolean = true
    var playLayoutPos = -1

    fun bindLifecycleOwner(owner: LifecycleOwner) {
        lifecycleOwner?.lifecycle?.removeObserver(this)
        lifecycleOwner = owner
        owner.lifecycle.addObserver(this)

        videoPlayerHelper.setLooping(true)
        videoPlayerHelper.aspectRatio = AspectRatio.AspectRatio_FILL_PARENT
        videoPlayerHelper.adapterEventHandler = VideoPlayHelperAdapterEventHandler()
        attachListener()
        videoPlayerHelper.bridge.apply {
            clear()
            addAdapter(ControllerCoverAdapter(context, isMute).apply {
                controllerCoverAdapter = this
            })
        }
    }

    private fun attachListener() {
        videoPlayerHelper.onPlayerEventListener = object : OnPlayerEventListener {
            override fun onPlayerEvent(message: HoHoMessage) {
                when (message.what) {
                    OnPlayerEventListener.PLAYER_EVENT_ON_VIDEO_RENDER_START,
                    OnPlayerEventListener.PLAYER_EVENT_ON_START,
                    OnPlayerEventListener.PLAYER_EVENT_ON_RESUME -> {
                        currentImageView?.visibility = View.INVISIBLE
                    }
                }
            }
        }
        videoPlayerHelper.onAdapterEventListener = object : OnAdapterEventListener {
            override fun onAdapterEvent(message: HoHoMessage) {
                when (message.what) {
                    PlayerEvent.EVENT_CODE_REQUEST_MUTE -> {
                        isMute = (message.argObj as? Boolean) ?: true
                        if (isMute) {
                            videoPlayerHelper.setVolume(0f, 0f)
                        } else {
                            videoPlayerHelper.setVolume(1f, 1f)
                        }
                    }
                }
            }
        }
    }

    private fun deAttachListener() {
        videoPlayerHelper.onPlayerEventListener = null
        videoPlayerHelper.onErrorEventListener = null
        videoPlayerHelper.onAdapterEventListener = null
    }

    fun bindRecyclerView(recyclerView: RecyclerView, scrollerInterceptor: ((rv: RecyclerView) -> Boolean)? = null) {
        this.outRecyclerView = recyclerView
        this.adapter = recyclerView.adapter
        recyclerView.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val location = IntArray(2)
                recyclerView.getLocationOnScreen(location)
                verticalRecyclerStart = location[1]
                thresholdBottom = verticalRecyclerStart + recyclerView.height
                recyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
        dataObserver?.let {
            adapter?.unregisterAdapterDataObserver(it)
        }
        dataObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                getPlayerPosInRecyclerView(recyclerView)
            }

            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                super.onItemRangeChanged(positionStart, itemCount)
                getPlayerPosInRecyclerView(recyclerView)
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                getPlayerPosInRecyclerView(recyclerView)
            }

            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                super.onItemRangeMoved(fromPosition, toPosition, itemCount)
                getPlayerPosInRecyclerView(recyclerView)
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                super.onItemRangeRemoved(positionStart, itemCount)
                getPlayerPosInRecyclerView(recyclerView)
            }
        }.apply {
            adapter?.registerAdapterDataObserver(this)
        }

        var isFirst = true
        scrollListener?.let {
            recyclerView.removeOnScrollListener(it)
        }
        scrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState != RecyclerView.SCROLL_STATE_IDLE) {
                    return
                }
                if (scrollerInterceptor?.invoke(recyclerView) == true) {
                    return
                }
                checkPlayerPos(recyclerView)
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                getPlayerPosInRecyclerView(recyclerView, isFirst)
                if (isFirst) {
                    isFirst = !isFirst
                }
            }
        }.apply {
            recyclerView.addOnScrollListener(this)
        }
        recyclerView.post {
            checkPlayerPos(recyclerView)
        }
    }

    fun handleViewRecycled(viewGroup: ViewGroup) {
        processAction(PlayerAction(PlayerAction.ACTION_STOP, -1, viewGroup))
        // re-check
        getPlayerPosInRecyclerView(outRecyclerView)
    }

    private fun getPlayerPosInRecyclerView(recyclerView: RecyclerView?, checkVH: Boolean = true) {
        recyclerView?.post { innerGetPos(recyclerView, checkVH) }
    }

    private fun innerGetPos(recyclerView: RecyclerView, checkVH: Boolean) {
        val lm = recyclerView.layoutManager as? LinearLayoutManager
        lm ?: return
        val fistPos = lm.findFirstVisibleItemPosition()
        val vh = getItemHolder(recyclerView, fistPos)
        // top >= thresholdTop && is Video Player VH
        if (vh == null) {
            playLayoutPos = -1
            return
        }
        val top = vh.itemView.top
        val bottom = vh.itemView.bottom
        playLayoutPos = if (top >= thresholdTop) {
            if (thresholdBottom > 0 && bottom > thresholdBottom) {
                if ((top + vh.itemView.height / 2) > thresholdBottom) {
                    -1
                } else {
                    vh.adapterPosition
                }
            } else {
                vh.adapterPosition
            }
        } else {
            -1
        }
        if (checkVH) {
            checkPlayerPos(recyclerView)
        }
    }

    private fun processAction(playerAction: PlayerAction) {
        when (playerAction.action) {
            PlayerAction.ACTION_PLAY -> {
                play(DataSource(playerAction.url), playerAction.layoutContainer, playerAction)
            }
            PlayerAction.ACTION_PAUSE -> {
                pause()
            }
            PlayerAction.ACTION_STOP -> {
                stop(playerAction.layoutContainer)
            }
        }
    }

    private fun getItemHolder(recyclerView: RecyclerView, position: Int): RecyclerView.ViewHolder? {
        var findPos = position
        val itemCount = recyclerView.adapter?.itemCount ?: 0
        while (findPos < itemCount) {
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(findPos) ?: return null
            if (viewHolder is ListPlayerViewHolder && !viewHolder.videoUrl.isNullOrEmpty()) {
                val top = viewHolder.itemView.top
                if (top >= thresholdTop) {
                    return viewHolder
                }
            }
            findPos++
        }
        return null
    }

    private fun checkPlayerPos(recyclerView: RecyclerView) {
        if (playLayoutPos < 0) {
            pause()
        } else {
            val vh = recyclerView.findViewHolderForAdapterPosition(playLayoutPos)
            if (vh is ListPlayerViewHolder) {
                processAction(vh.onStartAction())
            }
        }
    }

    private fun attachContainer(userContainer: ViewGroup, updateRender: Boolean = true) {
        videoPlayerHelper.attachContainer(userContainer, updateRender)
    }

    fun play(dataSource: DataSource, viewGroup: ViewGroup, playerAction: PlayerAction) {
        play(dataSource, viewGroup, playerAction.imageView, playerAction.pos, playerAction.muteImage)
    }

    fun play(dataSource: DataSource,
             viewGroup: ViewGroup,
             imageView: View?,
             pos: Int,
             muteImage: Boolean = false
    ) {
        if (currentViewGroup == viewGroup && currentPos == pos) {
            if (viewGroup.visibility != View.VISIBLE) {
                viewGroup.visibility = View.VISIBLE
            }
            if (!isPlaying()) {
                resume()
            }
            return
        }
        val videoUrl = dataSource.data
        if (videoUrl.isNullOrEmpty()) {
            return
        }
        currentImageView?.visibility = View.VISIBLE
        currentImageView?.alpha = 1f
        currentViewGroup = viewGroup
        viewGroup.visibility = View.VISIBLE
        currentImageView = imageView
        currentPos = pos
        attachContainer(viewGroup)
        controllerCoverAdapter?.handleMuteImage(muteImage)
        play(DataSource(PlayerCacher.getProxyUrl(videoUrl)), true)
    }

    protected fun play(dataSource: DataSource, updateRender: Boolean) {
        videoPlayerHelper.dataSource = dataSource
        videoPlayerHelper.play(updateRender)
    }

    fun isInPlaybackState(): Boolean {
        return videoPlayerHelper.isInPlaybackState()
    }

    fun isPlaying(): Boolean {
        return videoPlayerHelper.isPlaying()
    }

    fun getCurrentPosition(): Int {
        return videoPlayerHelper.getCurrentPosition()
    }

    fun getState(): Int {
        return videoPlayerHelper.getState()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun pause() {
        videoPlayerHelper.pause()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun resume() {
        videoPlayerHelper.resume()
    }

    fun stop(viewGroup: ViewGroup) {
        if (viewGroup == currentViewGroup) {
            videoPlayerHelper.stop()
            currentImageView = null
            currentViewGroup = null
            currentPos = -1
        }
    }

    fun reset() {
        videoPlayerHelper.reset()
    }

    fun rePlay(position: Int) {
        videoPlayerHelper.rePlay(position)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun destroy() {
        // recycler view
        dataObserver?.let {
            adapter?.unregisterAdapterDataObserver(it)
        }
        dataObserver = null
        adapter = null
        scrollListener?.let {
            outRecyclerView?.removeOnScrollListener(it)
        }
        outRecyclerView = null
        scrollListener = null
        // item view in recycler view
        currentViewGroup = null
        currentImageView = null
        // lifecycle
        lifecycleOwner?.lifecycle?.removeObserver(this)
        // player
        controllerCoverAdapter = null
        videoPlayerHelper.bridge.clear()
        deAttachListener()
        videoPlayerHelper.destroy()
    }
}

object PlayerEvent {
    const val EVENT_CODE_REQUEST_MUTE = -101
}

data class PlayerAction(
        val action: Int,
        val pos: Int,
        val layoutContainer: ViewGroup,
        val imageView: View? = null,
        val url: String = "",
        val muteImage: Boolean = false
) {

    companion object {
        const val ACTION_PLAY = 1
        const val ACTION_PAUSE = 2
        const val ACTION_STOP = 3
    }
}

interface ListPlayerAdapter {

    fun recycleListPlayViewHolder(listPlayer: ListPlayer, vh: ListPlayerViewHolder) {
        vh.videoUrl = null
        listPlayer.handleViewRecycled(vh.getPlayerContainer())
    }
}

interface ListPlayerViewHolder {

    var videoUrl: String?

    fun getPlayerContainer(): ViewGroup

    fun getViewHolder(): RecyclerView.ViewHolder

    fun getCoverImage(): ImageView? {
        return null
    }

    fun onStartAction(): PlayerAction {
        val vh = getViewHolder()
        return PlayerAction(PlayerAction.ACTION_PLAY,
                vh.adapterPosition,
                getPlayerContainer(),
                getCoverImage(),
                videoUrl.orEmpty()
        )
    }
}