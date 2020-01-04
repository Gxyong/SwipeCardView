package com.gxy.kotlin


import android.annotation.TargetApi
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Rect
import android.os.Build
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.Interpolator
import android.widget.LinearLayout
import android.widget.Scroller

import com.gxy.swipecard.R

import java.util.ArrayList

/**
 * @Author: 文西
 * 时间:     2020/1/4$ 20:04$
 * 版本:
 * 描述: dec
 * 修改说明:
 */

class SwipeCardsView constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : LinearLayout(context, attrs, defStyle) {
    private val viewList = ArrayList<View>() // 存放的是每一层的view，从顶到底
    private val releasedViewList = ArrayList<View>() // 手指松开后存放的view列表

    private var initLeft = 0
    private var initTop = 0 // 正常状态下 topView的left和top
    private var mWidth = 0 // swipeCardsView的宽度
    private var mHeight = 0 // swipeCardsView的高度
    private var mCardWidth = 0 // 每一个子View对应的宽度

    private var yOffsetStep = 0 // view叠加垂直偏移量的步长
    private var scaleOffsetStep = 0f // view叠加缩放的步长
    private var alphaOffsetStep = 0 //view叠加透明度的步长

    private var mCardsSlideListener: com.gxy.java.SwipeCardsView.CardsSlideListener? = null // 回调接口
    private var mCount: Int = 0 // 卡片的数量
    private var mShowingIndex = 0 // 当前正在显示的卡片位置
    private val btnListener: View.OnClickListener

    private var mAdapter: BaseCardAdapter<*>? = null
    private val mScroller: Scroller
    private val mTouchSlop: Int
    private var mLastY = -1 // save event y
    private var mLastX = -1 // save event x
    private var mInitialMotionY: Int = 0
    private var mInitialMotionX: Int = 0
    private val SCROLL_DURATION = 300 // scroll back duration
    private var hasTouchTopView: Boolean = false
    private var mVelocityTracker: VelocityTracker? = null
    private val mMaxVelocity: Float
    private val mMinVelocity: Float
    private var isIntercepted = false
    private var isTouching = false
    private var tempShowingIndex = -1
    private var cardVisibleCount = 3
    /**
     * 卡片是否在移动的标记，如果在移动中则不执行onLayout中的layout操作
     */
    private var mScrolling = false

    private var mWaitRefresh = false

    private var mRetainLastCard = false

    private var mEnableSwipe = true

    private var mLastMoveEvent: MotionEvent? = null
    private var mHasSendCancelEvent = false

    private val topView: View?
        get() = if (viewList.size > 0) {
            viewList[0]
        } else null

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.SwipCardsView)
        yOffsetStep = a.getDimension(R.styleable.SwipCardsView_yOffsetStep, yOffsetStep.toFloat()).toInt()
        alphaOffsetStep = a.getInt(R.styleable.SwipCardsView_alphaOffsetStep, alphaOffsetStep)
        scaleOffsetStep = a.getFloat(R.styleable.SwipCardsView_scaleOffsetStep, scaleOffsetStep)

        a.recycle()

        btnListener = OnClickListener { view ->
            if (null != mCardsSlideListener && view.scaleX == 1f) {
                mCardsSlideListener!!.onItemClick(view, mShowingIndex)
            }
        }
        mScroller = Scroller(getContext(), sInterpolator)
        mTouchSlop = ViewConfiguration.get(getContext()).scaledTouchSlop
        mMaxVelocity = ViewConfiguration.get(getContext()).scaledMaximumFlingVelocity.toFloat()
        mMinVelocity = ViewConfiguration.get(getContext()).scaledMinimumFlingVelocity.toFloat()
    }

    private fun getCardLayoutId(layoutid: Int): Int {
        val resourceTypeName = context.resources.getResourceTypeName(layoutid)
        if (resourceTypeName!="layout") {
            val errorMsg = context.resources.getResourceName(layoutid) + " is a illegal layoutid , please check your layout id first "
            throw RuntimeException(errorMsg)
        }
        return layoutid
    }

    private fun bindCardData(position: Int, cardview: View) {
        if (mAdapter != null) {
            mAdapter!!.onBindData(position, cardview)
            cardview.tag = position
        }
        cardview.visibility = View.VISIBLE
    }

    /**
     * 刷新ui
     *
     * @param index 当前显示的卡片下标
     */
    fun notifyDatasetChanged(index: Int) {
        if (canResetView()) {
            refreshUI(index)
        } else {
            mWaitRefresh = true
            tempShowingIndex = index
        }
    }

    private fun refreshUI(index: Int) {
        if (mAdapter == null) {
            throw RuntimeException("adapter==null")
        }
        mShowingIndex = index
        mCount = mAdapter!!.count
        //        cardVisibleCount = mAdapter.getVisibleCardCount();
        cardVisibleCount = Math.min(cardVisibleCount, mCount)
        for (i in mShowingIndex .. mShowingIndex + cardVisibleCount) {
            val childView = viewList[i - mShowingIndex] ?: return
            if (i < mCount) {
                bindCardData(i, childView)
            } else {
                childView.visibility = View.GONE
            }
            setOnItemClickListener(childView)
        }
        if (null != mCardsSlideListener) {
            mCardsSlideListener!!.onShow(mShowingIndex)
        }
    }

    private fun setOnItemClickListener(childView: View) {
        childView.setOnClickListener(btnListener)
    }

    fun setAdapter(adapter: BaseCardAdapter<*>?) {
        if (adapter == null) {
            throw RuntimeException("adapter==null")
        }
        mAdapter = adapter
        mShowingIndex = 0
        removeAllViewsInLayout()
        viewList.clear()
        mCount = mAdapter!!.count
        var cardVisibleCount = mAdapter!!.visibleCardCount
        cardVisibleCount = Math.min(cardVisibleCount, mCount)
        for (i in mShowingIndex .. mShowingIndex + cardVisibleCount) {
            val childView = LayoutInflater.from(context).inflate(getCardLayoutId(mAdapter!!.cardLayoutId), this, false)
                    ?: return
            if (i < mCount) {
                bindCardData(i, childView)
            } else {
                childView.visibility = View.GONE
            }
            viewList.add(childView)
            setOnItemClickListener(childView)
            addView(childView, 0)
        }
        if (null != mCardsSlideListener) {
            mCardsSlideListener!!.onShow(mShowingIndex)
        }
    }

    /**
     * whether retain last card
     *
     * @param retain defalut false
     */
    fun retainLastCard(retain: Boolean) {
        mRetainLastCard = retain
    }

    private fun canMoveCard(): Boolean {
        return !mRetainLastCard || mRetainLastCard && mShowingIndex != mCount - 1
    }

    fun enableSwipe(enable: Boolean) {
        mEnableSwipe = enable
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.actionMasked
        acquireVelocityTracker(ev)
        var deltaY = 0
        var deltaX = 0
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mScroller.abortAnimation()
                resetViewGroup()
                if (isTouchTopView(ev) && canMoveCard() && mEnableSwipe) {
                    isTouching = true
                }
                hasTouchTopView = false
                mLastY = ev.rawY.toInt()
                mLastX = ev.rawX.toInt()
                mInitialMotionY = mLastY
                mInitialMotionX = mLastX
            }
            MotionEvent.ACTION_MOVE -> {
                if (!canMoveCard() || !mEnableSwipe) {
                    return super.dispatchTouchEvent(ev)
                }
                mLastMoveEvent = ev
                val currentY = ev.rawY.toInt()
                val currentX = ev.rawX.toInt()
                deltaY = currentY - mLastY
                deltaX = currentX - mLastX
                mLastY = currentY
                mLastX = currentX
                if (!isIntercepted) {
                    val distanceX = Math.abs(currentX - mInitialMotionX)
                    val distanceY = Math.abs(currentY - mInitialMotionY)
                    if (distanceX * distanceX + distanceY + distanceY >= mTouchSlop * mTouchSlop) {
                        isIntercepted = true
                    } else {
                        return super.dispatchTouchEvent(ev)
                    }
                }

                if (isIntercepted && (hasTouchTopView || isTouchTopView(ev))) {
                    hasTouchTopView = true
                    moveTopView(deltaX, deltaY)
                    invalidate()
                    sendCancelEvent()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                hasTouchTopView = false
                isTouching = false
                isIntercepted = false
                mHasSendCancelEvent = false
                mVelocityTracker!!.computeCurrentVelocity(1000, mMaxVelocity)
                val velocityX = mVelocityTracker!!.xVelocity
                val velocityY = mVelocityTracker!!.yVelocity
                val xvel = clampMag(velocityX, mMinVelocity, mMaxVelocity)
                val yvel = clampMag(velocityY, mMinVelocity, mMaxVelocity)

                releaseTopView(xvel, yvel)
                releaseVelocityTracker()
            }
        }//                invalidate();
        return super.dispatchTouchEvent(ev)
    }

    private fun sendCancelEvent() {
        if (!mHasSendCancelEvent) {
            mHasSendCancelEvent = true
            val last = mLastMoveEvent
            val e = MotionEvent.obtain(
                    last!!.downTime,
                    last.eventTime + ViewConfiguration.getLongPressTimeout(),
                    MotionEvent.ACTION_CANCEL, last.x, last.y,
                    last.metaState)
            dispatchTouchEventSupper(e)
        }
    }


    //    @Override
    //    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
    //        boolean ret = false;
    //        int canvasRestore = -1;
    //        int index = indexOfChild(child);
    //        View topView = getTopView();
    //        LogUtil.e("index=" + index + " is topview " + (topView == child));
    //        if (topView != child) {
    //            Rect thisRect = new Rect();
    //            getGlobalVisibleRect(thisRect);
    //            Rect childRect = new Rect();
    //            child.getGlobalVisibleRect(childRect);
    //            childRect.set(childRect.left - thisRect.left, childRect.top - thisRect.top, childRect.left - thisRect.left + childRect.width(), childRect.height() + childRect.top - thisRect.top);
    //            Rect topRect = new Rect();
    //            topView.getGlobalVisibleRect(topRect);
    //            topRect.set(topRect.left - thisRect.left, topRect.top - thisRect.top, topRect.left - thisRect.left + topRect.width(), topRect.height() + topRect.top - thisRect.top);
    //            Rect newrect = new Rect(thisRect.left, topRect.bottom, thisRect.right, child.getBottom());
    //            canvasRestore = canvas.save();
    //            canvas.clipRect(topRect, Region.Op.XOR);
    //
    ////            Paint mPaint = new Paint();
    ////            mPaint.setAntiAlias(true);
    ////            mPaint.setStyle(Paint.Style.STROKE);
    ////            mPaint.setColor(Color.RED);
    ////            canvas.drawRect(newrect, mPaint);
    //            LogUtil.e("test index=" + index + ";newrect=" + newrect.toShortString() + "child.bottom=" + child.getBottom() + ";thisRect=" + thisRect.toShortString() + ";canvas.getClipBounds()=" + canvas.getClipBounds().toShortString());
    //        }
    //        ret = super.drawChild(canvas, child, drawingTime);
    //        if (canvasRestore != -1) {
    //            canvas.restoreToCount(canvasRestore);
    //        }
    //        return ret;
    //    }

    fun dispatchTouchEventSupper(e: MotionEvent): Boolean {
        return super.dispatchTouchEvent(e)
    }

    private fun releaseTopView(xvel: Float, yvel: Float) {
        mScrolling = true
        val topView = topView
        if (topView != null && canMoveCard() && mEnableSwipe) {
            onTopViewReleased(topView, xvel, yvel)
        }
    }

    /**
     * 是否摸到了某个view
     *
     * @param ev
     * @return
     */
    private fun isTouchTopView(ev: MotionEvent): Boolean {
        val topView = topView
        if (topView != null && topView.visibility == View.VISIBLE) {
            val bounds = Rect()
            topView.getGlobalVisibleRect(bounds)
            val x = ev.rawX.toInt()
            val y = ev.rawY.toInt()
            return if (bounds.contains(x, y)) {
                true
            } else {
                false
            }
        }
        return false
    }

    private fun moveTopView(deltaX: Int, deltaY: Int) {
        val topView = topView
        if (topView != null) {
            topView.offsetLeftAndRight(deltaX)
            topView.offsetTopAndBottom(deltaY)
            processLinkageView(topView)
        }
    }


    fun startScrollTopView(finalLeft: Int, finalTop: Int, duration: Int, flyType: com.gxy.java.SwipeCardsView.SlideType) {
        val topView = topView
        if (topView == null) {
            mScrolling = false
            return
        }
        if (finalLeft != initLeft) {
            releasedViewList.add(topView)
        }
        val startLeft = topView.left
        val startTop = topView.top
        val dx = finalLeft - startLeft
        val dy = finalTop - startTop
        if (dx != 0 || dy != 0) {
            mScroller.startScroll(topView.left, topView.top, dx, dy, duration)
            ViewCompat.postInvalidateOnAnimation(this)
        } else {
            mScrolling = false
        }
        if (flyType != com.gxy.java.SwipeCardsView.SlideType.NONE && mCardsSlideListener != null) {
            mCardsSlideListener!!.onCardVanish(mShowingIndex, flyType)
        }
    }

    /**
     * @param event 向VelocityTracker添加MotionEvent
     * @see VelocityTracker.obtain
     * @see VelocityTracker.addMovement
     */
    private fun acquireVelocityTracker(event: MotionEvent) {
        if (null == mVelocityTracker) {
            mVelocityTracker = VelocityTracker.obtain()
        }
        mVelocityTracker!!.addMovement(event)
    }

    /**
     * 释放VelocityTracker
     *
     * @see VelocityTracker.clear
     * @see VelocityTracker.recycle
     */
    private fun releaseVelocityTracker() {
        if (null != mVelocityTracker) {
            mVelocityTracker!!.clear()
            mVelocityTracker!!.recycle()
            mVelocityTracker = null
        }
    }

    /**
     * Clamp the magnitude of value for absMin and absMax.
     * If the value is below the minimum, it will be clamped to zero.
     * If the value is above the maximum, it will be clamped to the maximum.
     *
     * @param value  Value to clamp
     * @param absMin Absolute value of the minimum significant value to return
     * @param absMax Absolute value of the maximum value to return
     * @return The clamped value with the same sign as `value`
     */
    private fun clampMag(value: Float, absMin: Float, absMax: Float): Float {
        val absValue = Math.abs(value)
        if (absValue < absMin) return 0f
        return if (absValue > absMax) if (value > 0) absMax else -absMax else value
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measureChildrenWithMargins(widthMeasureSpec, heightMeasureSpec)
        val maxWidth = View.MeasureSpec.getSize(widthMeasureSpec)
        val maxHeight = View.MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, 0), resolveSizeAndState(maxHeight, heightMeasureSpec, 0))
        mWidth = measuredWidth
        mHeight = measuredHeight
    }

    private fun measureChildrenWithMargins(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = childCount
        for (i in 0 .. size) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (hasTouchTopView || mScrolling) {
            return
        }
        val size = viewList.size
        if (size == 0) {
            return
        }
        for (i in 0 .. size) {
            val child = viewList[i]
            layoutChild(child, i)
        }
        // 初始化一些中间参数
        initLeft = viewList[0].left
        initTop = viewList[0].top
        mCardWidth = viewList[0].measuredWidth
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private fun layoutChild(child: View, index: Int) {
        val lp = child.layoutParams as LinearLayout.LayoutParams
        val width = child.measuredWidth
        val height = child.measuredHeight

        var gravity = lp.gravity
        if (gravity == -1) {
            gravity = Gravity.TOP or Gravity.START
        }

        val layoutDirection = layoutDirection
        val absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection)
        val verticalGravity = gravity and Gravity.VERTICAL_GRAVITY_MASK

        val childLeft: Int
        val childTop: Int
        when (absoluteGravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
            Gravity.CENTER_HORIZONTAL -> childLeft = (getWidth() + paddingLeft - paddingRight - width) / 2 + lp.leftMargin - lp.rightMargin
            Gravity.END -> childLeft = getWidth() + paddingRight - width - lp.rightMargin
            Gravity.START -> childLeft = paddingLeft + lp.leftMargin
            else -> childLeft = paddingLeft + lp.leftMargin
        }
        when (verticalGravity) {
            Gravity.CENTER_VERTICAL -> childTop = (getHeight() + paddingTop - paddingBottom - height) / 2 + lp.topMargin - lp.bottomMargin
            Gravity.BOTTOM -> childTop = getHeight() - paddingBottom - height - lp.bottomMargin
            Gravity.TOP -> childTop = paddingTop + lp.topMargin
            else -> childTop = paddingTop + lp.topMargin
        }
        child.layout(childLeft, childTop, childLeft + width, childTop + height)
        val offset = yOffsetStep * index
        val scale = 1 - scaleOffsetStep * index
        val alpha = 1.0f * (100 - alphaOffsetStep * index) / 100
        child.offsetTopAndBottom(offset)
        child.scaleX = scale
        child.scaleY = scale
        child.alpha = alpha
    }

    override fun computeScroll() {
        if (mScroller.computeScrollOffset()) {
            val topView = topView ?: return
            val x = mScroller.currX
            val y = mScroller.currY
            val dx = x - topView.left
            val dy = y - topView.top
            if (x != mScroller.finalX || y != mScroller.finalY) {
                moveTopView(dx, dy)
            }
            ViewCompat.postInvalidateOnAnimation(this)
        } else {
            mScrolling = false
            onAnimalStop()
        }
    }

    private fun onAnimalStop() {
        if (canResetView()) {
            resetViewGroup()
        }
    }

    private fun canResetView(): Boolean {
        return !mScroller.computeScrollOffset() && !isTouching
    }

    /**
     * 对View重新排序
     */
    private fun resetViewGroup() {
        if (releasedViewList.size == 0) {
            mScrolling = false
            if (mWaitRefresh) {
                mWaitRefresh = false
                refreshUI(tempShowingIndex)
            }
            if (viewList.size != 0) {
                val topView = topView
                if (topView != null) {
                    if (topView.left != initLeft || topView.top != initTop) {
                        topView.offsetLeftAndRight(initLeft - topView.left)
                        topView.offsetTopAndBottom(initTop - topView.top)
                    }
                }
            }
        } else {
            val changedView = releasedViewList[0]
            if (changedView.left == initLeft) {
                releasedViewList.removeAt(0)
                mScrolling = false
                return
            }
            viewList.remove(changedView)
            viewList.add(changedView)
            mScrolling = false
            val viewSize = viewList.size
            removeViewInLayout(changedView)
            addViewInLayout(changedView, 0, changedView.layoutParams, true)
            requestLayout()
            //            removeView(changedView);
            //            addView(changedView,0);
            if (mWaitRefresh) {
                mWaitRefresh = false
                val index = ++tempShowingIndex
                refreshUI(index)
            } else {
                val newIndex = mShowingIndex + viewSize
                if (newIndex < mCount) {
                    bindCardData(newIndex, changedView)
                } else {
                    changedView.visibility = View.GONE
                }
                if (mShowingIndex + 1 < mCount) {
                    mShowingIndex++
                    if (null != mCardsSlideListener) {
                        mCardsSlideListener!!.onShow(mShowingIndex)
                    }
                } else {
                    //no card showing
                    mShowingIndex = -1
                }
            }
            releasedViewList.removeAt(0)
        }
        tempShowingIndex = -1
    }

    /**
     * 顶层卡片View位置改变，底层的位置需要调整
     *
     * @param changedView 顶层的卡片view
     */
    private fun processLinkageView(changedView: View) {
        val changeViewLeft = changedView.left
        val changeViewTop = changedView.top
        val distance = Math.abs(changeViewTop - initTop) + Math.abs(changeViewLeft - initLeft)
        val rate = distance / MAX_SLIDE_DISTANCE_LINKAGE.toFloat()

        for (i in 1 .. viewList.size) {
            var rate3 = rate - 0.2f * i
            if (rate3 > 1) {
                rate3 = 1f
            } else if (rate3 < 0) {
                rate3 = 0f
            }
            ajustLinkageViewItem(changedView, rate3, i)
        }
    }

    // 由index对应view变成index-1对应的view
    private fun ajustLinkageViewItem(changedView: View, rate: Float, index: Int) {
        val changeIndex = viewList.indexOf(changedView)

        val initPosY = yOffsetStep * index
        val initScale = 1 - scaleOffsetStep * index
        val initAlpha = 1.0f * (100 - alphaOffsetStep * index) / 100

        val nextPosY = yOffsetStep * (index - 1)
        val nextScale = 1 - scaleOffsetStep * (index - 1)
        val nextAlpha = 1.0f * (100 - alphaOffsetStep * (index - 1)) / 100

        val offset = (initPosY + (nextPosY - initPosY) * rate).toInt()
        val scale = initScale + (nextScale - initScale) * rate
        val alpha = initAlpha + (nextAlpha - initAlpha) * rate

        val ajustView = viewList[changeIndex + index]
        ajustView.offsetTopAndBottom(offset - ajustView.top + initTop)
        ajustView.scaleX = scale
        ajustView.scaleY = scale
        ajustView.alpha = alpha
    }

    /**
     * 松手时处理滑动到边缘的动画
     *
     * @param xvel X方向上的滑动速度
     */
    private fun onTopViewReleased(changedView: View, xvel: Float, yvel: Float) {
        var finalX = initLeft
        var finalY = initTop
        var flyType: com.gxy.java.SwipeCardsView.SlideType = com.gxy.java.SwipeCardsView.SlideType.NONE

        var dx = changedView.left - initLeft
        val dy = changedView.top - initTop
        if (dx == 0) {
            // 由于dx作为分母，此处保护处理
            dx = 1
        }
        if (dx > X_DISTANCE_THRESHOLD || xvel > X_VEL_THRESHOLD && dx > 0) {//向右边滑出
            finalX = mWidth
            finalY = dy * (mCardWidth + initLeft) / dx + initTop
            flyType = com.gxy.java.SwipeCardsView.SlideType.RIGHT
        } else if (dx < -X_DISTANCE_THRESHOLD || xvel < -X_VEL_THRESHOLD && dx < 0) {//向左边滑出
            finalX = -mCardWidth
            finalY = dy * (mCardWidth + initLeft) / -dx + dy + initTop
            flyType = com.gxy.java.SwipeCardsView.SlideType.LEFT
        }

        if (finalY > mHeight) {
            finalY = mHeight
        } else if (finalY < -mHeight / 2) {
            finalY = -mHeight / 2
        }
        startScrollTopView(finalX, finalY, SCROLL_DURATION, flyType)
    }

    /**
     * use this method to Slide the card out of the screen
     *
     */
    fun slideCardOut(type: com.gxy.java.SwipeCardsView.SlideType) {
        if (!canMoveCard()) {
            return
        }
        mScroller.abortAnimation()
        resetViewGroup()
        val topview = topView ?: return
        if (releasedViewList.contains(topview) || type == com.gxy.java.SwipeCardsView.SlideType.NONE) {
            return
        }
        var finalX = 0
        when (type) {
            SwipeCardsView.SlideType.LEFT -> finalX = -mCardWidth
            SwipeCardsView.SlideType.RIGHT -> finalX = mWidth
        }
        if (finalX != 0) {
            startScrollTopView(finalX, initTop + mHeight, SCROLL_DURATION, type)
        }
    }

    /**
     * 设置卡片操作回调
     *
     * @param cardsSlideListener 回调接口
     */
    fun setCardsSlideListener(cardsSlideListener: com.gxy.java.SwipeCardsView.CardsSlideListener) {
        this.mCardsSlideListener = cardsSlideListener
    }

    /**
     * 卡片回调接口
     */
    interface CardsSlideListener {
        /**
         * 新卡片显示回调
         *
         * @param index 最顶层显示的卡片的index
         */
        fun onShow(index: Int)

        /**
         * 卡片飞向两侧回调
         *
         * @param index 飞向两侧的卡片数据index
         * @param type  飞向哪一侧[com.gxy.java.SwipeCardsView.SlideType.LEFT]或[com.gxy.java.SwipeCardsView.SlideType.RIGHT]
         */
        fun onCardVanish(index: Int, type: com.gxy.java.SwipeCardsView.SlideType)

        /**
         * 卡片点击事件
         *
         * @param cardImageView 卡片上的图片view
         * @param index         点击到的index
         */
        fun onItemClick(cardImageView: View, index: Int)
    }

    /**
     *
     *
     * [.LEFT] 从屏幕左边滑出
     *
     * [.RIGHT] 从屏幕右边滑出
     */
    enum class SlideType {
        LEFT, RIGHT, NONE
    }

    companion object {

        private val MAX_SLIDE_DISTANCE_LINKAGE = 400 // 水平距离+垂直距离

        private val X_VEL_THRESHOLD = 900
        private val X_DISTANCE_THRESHOLD = 300

        /**
         * Interpolator defining the animation curve for mScroller
         */
        private val sInterpolator = object : Interpolator {

            private val mTension = 1.6f

            override fun getInterpolation(t: Float): Float {
                var t = t
                t -= 1.0f
                return t * t * ((mTension + 1) * t + mTension) + 1.0f
            }
        }

        fun resolveSizeAndState(size: Int, measureSpec: Int, childMeasuredState: Int): Int {
            var result = size
            val specMode = View.MeasureSpec.getMode(measureSpec)
            val specSize = View.MeasureSpec.getSize(measureSpec)
            when (specMode) {
                View.MeasureSpec.UNSPECIFIED -> result = size
                View.MeasureSpec.AT_MOST -> if (specSize < size) {
                    result = specSize or View.MEASURED_STATE_TOO_SMALL
                } else {
                    result = size
                }
                View.MeasureSpec.EXACTLY -> result = specSize
            }
            return result or (childMeasuredState and View.MEASURED_STATE_MASK)
        }
    }
}
