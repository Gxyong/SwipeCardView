package com.gxy.swipecardview;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.gxy.java.BaseCardAdapter;

import java.util.ArrayList;
import java.util.List;



/**
 * 时间: 2019-06-2614:22
 * 版本: 1.0
 * 描述:
 * 修改说明:
 */
public class HomeCardAdapter extends BaseCardAdapter {
    private Context context;
    public List<HomeBean> datas=new ArrayList<>();


    public HomeCardAdapter(Context context, List<HomeBean> datas) {
        this.context = context;
        this.datas = datas;
    }

    public void setData(List<HomeBean> datas) {
        this.datas = datas;
    }

    @Override
    public int getCount() {
        return datas.size();
    }

    @Override
    public int getCardLayoutId() {
        return R.layout.card_item;
    }

    @Override
    public void onBindData(int position, View cardview) {

        Log.e("-------",datas.get(position).getUrls());
        ImageView imageView = cardview.findViewById(R.id.iv_meizi);
        RequestOptions options = new RequestOptions();
        options.placeholder(R.drawable.i1); //设置加载未完成时的占位图
        options.error(R.drawable.i2); //设置加载异常时的占位图
        Glide.with(context)
                .load(datas.get(position).getUrls())
                .into(imageView);
    }

    @Override
    public int getVisibleCardCount() {
        return 3;
    }
}