package org.succlz123.hohoplayer.app.support

import java.io.Serializable
import java.util.*

object DataProvider {
    const val VIDEO_URL_01 = "http://jiajunhui.cn/video/kaipao.mp4"
    const val VIDEO_URL_02 = "http://jiajunhui.cn/video/kongchengji.mp4"
    const val VIDEO_URL_03 = "http://jiajunhui.cn/video/allsharestar.mp4"
    const val VIDEO_URL_04 = "http://jiajunhui.cn/video/edwin_rolling_in_the_deep.flv"
    const val VIDEO_URL_05 = "http://jiajunhui.cn/video/crystalliz.flv"
    const val VIDEO_URL_06 = "http://jiajunhui.cn/video/big_buck_bunny.mp4"
    const val VIDEO_URL_07 = "http://jiajunhui.cn/video/trailer.mp4"
    const val VIDEO_URL_08 = "https://mov.bn.netease.com/open-movie/nos/mp4/2017/12/04/SD3SUEFFQ_hd.mp4"
    const val VIDEO_URL_09 = "https://mov.bn.netease.com/open-movie/nos/mp4/2017/05/31/SCKR8V6E9_hd.mp4"

    var urls = arrayOf(
            VIDEO_URL_01,
            VIDEO_URL_02,
            VIDEO_URL_03,
            VIDEO_URL_04,
            VIDEO_URL_05,
            VIDEO_URL_06,
            VIDEO_URL_07)

    val remoteVideoItems: List<VideoItem>
        get() {
            var item: VideoItem
            val items: MutableList<VideoItem> = ArrayList()
            val len = urls.size
            for (i in 0 until len) {
                item = VideoItem()
                item.path = urls[i]
                item.displayName = urls[i]
                items.add(item)
            }
            return items
        }

    val videoList: List<VideoItem>
        get() {
            val videoList: MutableList<VideoItem> = ArrayList()
            videoList.add(VideoItem(
                    "坚持与放弃",
                    "https://mov.bn.netease.com/open-movie/nos/mp4/2015/08/27/SB13F5AGJ_sd.mp4",
                    "https://ss2.bdstatic.com/70cFvnSh_Q1YnxGkpoWK1HF6hhy/it/u=3746149156,3846202622&fm=26&gp=0.jpg"))
            videoList.add(VideoItem(
                    "不想从被子里出来",
                    "https://mov.bn.netease.com/open-movie/nos/mp4/2018/01/12/SD70VQJ74_sd.mp4",
                    "https://ss1.bdstatic.com/70cFuXSh_Q1YnxGkpoWK1HF6hhy/it/u=1603365312,3218205429&fm=26&gp=0.jpg"))
            videoList.add(VideoItem(
                    "不耐烦的中国人?",
                    "https://mov.bn.netease.com/open-movie/nos/mp4/2017/05/31/SCKR8V6E9_hd.mp4",
                    "https://ss0.bdstatic.com/70cFuHSh_Q1YnxGkpoWK1HF6hhy/it/u=2853553659,1775735885&fm=26&gp=0.jpg"))
            videoList.add(VideoItem(
                    "神奇的珊瑚",
                    "https://mov.bn.netease.com/open-movie/nos/mp4/2016/01/11/SBC46Q9DV_hd.mp4",
                    "https://ss2.bdstatic.com/70cFvnSh_Q1YnxGkpoWK1HF6hhy/it/u=2114611637,2615047297&fm=26&gp=0.jpg"))
            videoList.add(VideoItem(
                    "怎样经营你的人脉",
                    "https://mov.bn.netease.com/open-movie/nos/mp4/2018/04/19/SDEQS1GO6_hd.mp4",
                    "https://ss3.bdstatic.com/70cFv8Sh_Q1YnxGkpoWK1HF6hhy/it/u=2496080802,3943155079&fm=26&gp=0.jpg"))
            videoList.add(VideoItem(
                    "怎么才能不畏将来",
                    "https://mov.bn.netease.com/open-movie/nos/mp4/2018/01/25/SD82Q0AQE_hd.mp4",
                    "https://ss0.bdstatic.com/70cFuHSh_Q1YnxGkpoWK1HF6hhy/it/u=2370333706,3894132172&fm=26&gp=0.jpg"))
            videoList.add(VideoItem(
                    "音乐和艺术如何改变世界",
                    "https://mov.bn.netease.com/open-movie/nos/mp4/2017/12/04/SD3SUEFFQ_hd.mp4",
                    "https://ss1.bdstatic.com/70cFvXSh_Q1YnxGkpoWK1HF6hhy/it/u=2202780618,895893289&fm=26&gp=0.jpg"))
            return videoList
        }
}

data class VideoItem(var displayName: String? = null,
                     var path: String? = null,
                     var cover: String? = null) : Serializable