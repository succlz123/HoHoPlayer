package org.succlz123.hohoplayer.ui.list

import android.content.Context
import android.graphics.Bitmap
import android.view.TextureView
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
import org.succlz123.hohoplayer.app.list.ControllerCoverAdapter
import org.succlz123.hohoplayer.config.PlayerCacher
import org.succlz123.hohoplayer.config.PlayerContext
import org.succlz123.hohoplayer.core.adapter.event.OnAdapterEventListener
import org.succlz123.hohoplayer.core.player.listener.OnPlayerEventListener
import org.succlz123.hohoplayer.core.render.AspectRatio
import org.succlz123.hohoplayer.core.render.IRender.Companion.RENDER_TAG
import org.succlz123.hohoplayer.core.source.DataSource
import org.succlz123.hohoplayer.support.message.HoHoMessage
import org.succlz123.hohoplayer.widget.helper.VideoPlayHelperAdapterEventHandler
import org.succlz123.hohoplayer.widget.helper.VideoPlayerHelper


/**
 *  ------- Option 1 -------
 *
 *  1. New List Player & Bind LifecycleOwner
 *  val listPlayer = ListPlayer(requireContext())
 *  listPlayer.bindLifecycleOwner(viewLifecycleOwner)
 *
 *  2. Play video directly
 *  val dataSource = DataSource(entry.immersiveVideo?.url.orEmpty())
 *  bottomPlayer.play(dataSource, mBinding.layoutContainer, mBinding.image, 0)
 *
 *  ------- Option 2 -------
 *
 *  1. New List Player & Bind LifecycleOwner
 *  val listPlayer = ListPlayer(requireContext())
 *  listPlayer.bindLifecycleOwner(viewLifecycleOwner)
 *
 *  2. Adapter setting
 *  class xxAdapter(val listPlayer: ListPlayer): RecyclerView.Adapter {
 *
 *      override fun onViewRecycled(holder: BaseVH<*, *, *>) {
 *          super.onViewRecycled(holder)
 *          recycleListPlayViewHolder(listPlayer, holder)
 *      }
 *  }
 *
 *  3. VH setting
 *  class xxxVH: ViewHolder, ListPlayerViewHolder {
 *      var videoUrl: String? = null
 *      ......
 *  }
 */
class ListPlayer(var context: Context) : LifecycleObserver {
    private val videoPlayerHelper = VideoPlayerHelper(PlayerContext.context())

    private var lifecycleOwner: LifecycleOwner? = null

    private var outRecyclerView: RecyclerView? = null

    private var currentPlayerContainer: ViewGroup? = null
    private var currentCover: ImageView? = null
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
    var lastVideoPlayPos: HashMap<String, Int> = hashMapOf()

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
            addAdapter(ControllerCoverAdapter(context, isMute) {
                getItemHolder()?.onItemClick(it)
            }.apply {
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
                        currentPlayerContainer?.visibility = View.VISIBLE
                    }
                    OnPlayerEventListener.PLAYER_EVENT_ON_VIDEO_SIZE_CHANGE -> if (message.extra != null) {
                        val autoFitHeight = getItemHolder()?.autoFixHeight() ?: false
                        if (autoFitHeight) {
                            val videoWidth = message.getIntFromExtra("videoWidth", 0)
                            val videoHeight = message.getIntFromExtra("videoHeight", 0)
                            currentPlayerContainer?.let {
                                it.layoutParams?.apply {
                                    height = (videoHeight * 1f / videoWidth * it.width).toInt()
                                }
                            }
                            currentPlayerContainer?.let {
                                it.layoutParams?.apply {
                                    height = (videoHeight * 1f / videoWidth * it.width).toInt()
                                }
                            }
                        }
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

    private fun getItemHolder(): ListPlayerViewHolder? {
        outRecyclerView?.run {
            val vh = getItemHolder(this, currentPos)
            if (vh is ListPlayerViewHolder) {
                return vh
            }
        }
        return null
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
             playerContainer: ViewGroup,
             imageView: ImageView?,
             pos: Int,
             muteImage: Boolean = true
    ) {
        if (currentPlayerContainer == playerContainer && currentPos == pos) {
            if (playerContainer.visibility != View.VISIBLE) {
                playerContainer.visibility = View.VISIBLE
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
        currentPlayerContainer?.let { vg ->
            val bitmap = takesTextureScreenshot(vg.findViewWithTag(RENDER_TAG))
            currentCover?.let { ig ->
                if (bitmap != null) {
                    ig.setImageBitmap(bitmap)
                }
            }
            truePlay(videoUrl, playerContainer, imageView, pos, muteImage)
        } ?: kotlin.run { truePlay(videoUrl, playerContainer, imageView, pos, muteImage) }
    }

    private fun truePlay(videoUrl: String,
                         viewGroup: ViewGroup,
                         imageView: ImageView?,
                         pos: Int,
                         muteImage: Boolean = false) {
        val currentUrl = getCurrentDataSourece()?.data
        if (!currentUrl.isNullOrEmpty()) {
            lastVideoPlayPos[currentUrl] = getCurrentPosition()
        }
        currentPlayerContainer = viewGroup
        viewGroup.visibility = View.INVISIBLE
        currentCover = imageView
        currentPos = pos
        attachContainer(viewGroup)
        controllerCoverAdapter?.handleMuteImage(muteImage)
        val playUrl = PlayerCacher.getProxyUrl(videoUrl)
        val lastPos = lastVideoPlayPos[videoUrl] ?: 0
        play(DataSource(playUrl), true, lastPos)
    }

    private fun takesTextureScreenshot(v: View): Bitmap? {
        if (v is TextureView) {
            return v.bitmap
        }
        return null
    }

    protected fun play(dataSource: DataSource, updateRender: Boolean, lastPos: Int) {
        videoPlayerHelper.dataSource = dataSource
        videoPlayerHelper.play(updateRender, lastPos)
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

    fun getCurrentDataSourece(): DataSource? {
        return videoPlayerHelper.dataSource
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
        if (viewGroup == currentPlayerContainer) {
            videoPlayerHelper.stop()
            currentCover = null
            currentPlayerContainer = null
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
        currentPlayerContainer = null
        currentCover = null
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
        val imageView: ImageView? = null,
        val url: String = "",
        val muteImage: Boolean = true
) {

    companion object {
        const val ACTION_PLAY = 1
        const val ACTION_PAUSE = 2
        const val ACTION_STOP = 3
    }
}

interface ListPlayerAdapter {

    fun recycleListPlayViewHolder(listPlayer: ListPlayer, vh: ListPlayerViewHolder) {
        listPlayer.handleViewRecycled(vh.getPlayerContainer())
        listPlayer.lastVideoPlayPos.remove(vh.videoUrl)
        vh.videoUrl = null
    }
}

interface ListPlayerViewHolder {

    var videoUrl: String?

    fun getPlayerContainer(): ViewGroup

    fun getViewHolder(): RecyclerView.ViewHolder

    fun getCoverImage(): ImageView? {
        return null
    }

    fun autoFixHeight(): Boolean {
        return false
    }

    fun onItemClick(view: View) {
    }

    fun onStartAction(): PlayerAction {
        val vh = getViewHolder()
        return PlayerAction(
                PlayerAction.ACTION_PLAY,
                vh.absoluteAdapterPosition,
                getPlayerContainer(),
                getCoverImage(),
                videoUrl.orEmpty()
        )
    }
}