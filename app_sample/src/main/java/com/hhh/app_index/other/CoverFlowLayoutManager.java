package com.hhh.app_index.other;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.support.v7.widget.RecyclerView.LayoutParams;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.View;

public class CoverFlowLayoutManager extends LayoutManager {
    private int mSumDx = 0;
    private int mTotalHeight = 0;
    private int mTotalWidth = 0;

    @Override
    public LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
    }

    private int mItemWidth, mItemHeight;
    private SparseArray<Rect> mItemRects = new SparseArray<>();
    /**
     * 记录Item是否出现过屏幕且还没有回收。true表示出现过屏幕上，并且还没被回收
     */
    private SparseBooleanArray mHasAttachedItems = new SparseBooleanArray();

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getItemCount() == 0) {//没有Item，界面空着吧
            detachAndScrapAttachedViews(recycler);
            return;
        }
        mHasAttachedItems.clear();
        mItemRects.clear();

        detachAndScrapAttachedViews(recycler);

        //将item的位置存储起来
        View childView = recycler.getViewForPosition(0);
        measureChildWithMargins(childView, 0, 0);
        mItemWidth = getDecoratedMeasuredWidth(childView);
        mItemHeight = getDecoratedMeasuredHeight(childView);

        int visibleCount = getHorizontalSpace() / mItemWidth;


        //定义水平方向的偏移量
        int offsetX = 0;

        for (int i = 0; i < getItemCount(); i++) {
            Rect rect = new Rect(offsetX, 0, offsetX + mItemWidth, mItemHeight);
            mItemRects.put(i, rect);
            mHasAttachedItems.put(i, false);
            offsetX += mItemWidth;
        }

        Rect visibleRect = getVisibleArea();
        for (int i = 0; i < visibleCount; i++) {
            insertView(i, visibleRect, recycler, false);
        }

        //如果所有子View的宽度和没有填满RecyclerView的宽度，
        // 则将宽度设置为RecyclerView的宽度
        mTotalWidth = Math.max(offsetX, getHorizontalSpace());
    }

    private int getHorizontalSpace() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getChildCount() <= 0) {
            return dx;
        }

        int travel = dx;
        //如果滑动到最顶部
        if (mSumDx + dx < 0) {
            travel = -mSumDx;
        } else if (mSumDx + dx > mTotalWidth - getHorizontalSpace()) {
            //如果滑动到最底部
            travel = mTotalWidth - getHorizontalSpace() - mSumDx;
        }

        mSumDx += travel;

        Rect visibleRect = getVisibleArea();

        //回收越界子View
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View child = getChildAt(i);
            int position = getPosition(child);
            Rect rect = mItemRects.get(position);

            if (!Rect.intersects(rect, visibleRect)) {
                removeAndRecycleView(child, recycler);
                mHasAttachedItems.put(position, false);
            } else {
                layoutDecoratedWithMargins(child, rect.left - mSumDx, rect.top, rect.right - mSumDx, rect.bottom);
                mHasAttachedItems.put(position, true);
            }
        }

        //填充空白区域
        View lastView = getChildAt(getChildCount() - 1);
        View firstView = getChildAt(0);
        if (travel >= 0) {
            int minPos = getPosition(firstView);
            for (int i = minPos; i < getItemCount(); i++) {
                insertView(i, visibleRect, recycler, false);
            }
        } else {
            int maxPos = getPosition(lastView);
            for (int i = maxPos; i >= 0; i--) {
                insertView(i, visibleRect, recycler, true);
            }
        }
        return travel;
    }

    private void insertView(int pos, Rect visibleRect, RecyclerView.Recycler recycler, boolean firstPos) {
        Rect rect = mItemRects.get(pos);
        if (Rect.intersects(visibleRect, rect) && !mHasAttachedItems.get(pos)) {
            View child = recycler.getViewForPosition(pos);
            if (firstPos) {
                addView(child, 0);
            } else {
                addView(child);
            }
            measureChildWithMargins(child, 0, 0);
            layoutDecoratedWithMargins(child, rect.left - mSumDx, rect.top, rect.right - mSumDx, rect.bottom);

            mHasAttachedItems.put(pos, true);
        }
    }

    /**
     * 获取可见的区域Rect
     *
     * @return
     */
    private Rect getVisibleArea() {
        Rect result = new Rect(getPaddingLeft() + mSumDx, getPaddingTop(), getWidth() - getPaddingRight() + mSumDx, getHeight()-getPaddingBottom());
        return result;
    }
}
