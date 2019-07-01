package com.gxy.kotlin

import android.view.View

/**
 *    时间: 2019-06-27 18:56
 *    版本: 1.0
 *    描述:kotlin
 *    修改说明:
 */
abstract class BaseCardAdapter<T> {
    /**
     * 获取卡片的数量
     *
     * @return
     */
    abstract val count: Int

    /**
     * 获取卡片view的layout id
     *
     * @return
     */
    abstract val cardLayoutId: Int

    /**
     * 获取可见的cardview的数目，默认是3
     * @return
     */
    val visibleCardCount: Int
        get() {
            return 3
        }

    /**
     * 将卡片和数据绑定在一起
     *
     * @param position 数据在数据集中的位置
     * @param cardview 要绑定数据的卡片
     */
    abstract fun onBindData(position: Int, cardview: View)
}
