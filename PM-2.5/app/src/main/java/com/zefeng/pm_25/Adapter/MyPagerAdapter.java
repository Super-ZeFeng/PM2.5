package com.zefeng.pm_25.Adapter;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * Created by zefeng on 2016/7/11.
 */
public class MyPagerAdapter extends PagerAdapter {
    private List<View> viewList;

    @Override
    //返回页卡数量
    public int getCount() {
        return viewList.size();
    }

    @Override
    //是否来自于对象
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    //实例化一个页卡
    public Object instantiateItem(ViewGroup container, int position) {
        container.addView(viewList.get(position));
        return viewList.get(position);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView(viewList.get(position));
    }

    public MyPagerAdapter(List viewList) {
        this.viewList = viewList;
    }
}
