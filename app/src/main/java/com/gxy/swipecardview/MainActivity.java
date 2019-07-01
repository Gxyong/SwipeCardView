package com.gxy.swipecardview;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;


import com.gxy.java.SwipeCardsView;


import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity{

    private SwipeCardsView swipeCardsView;
    private HomeCardAdapter adapter;
    private List<HomeBean> mList = new ArrayList<>();
    private int curIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card);
        loadData();
        initView();
        linsener();
    }

    private void initView() {
        swipeCardsView = findViewById(R.id.swipCardsView);
        swipeCardsView.retainLastCard(true);
        swipeCardsView.enableSwipe(true);
        initLoadData();
    }

    private void linsener() {
        swipeCardsView.setCardsSlideListener(new SwipeCardsView.CardsSlideListener() {
            @Override
            public void onShow(int index) {
                Log.e("-----", "test showing index = " + index);
                Log.e("-----", "adapter.getCount()-1 = " + (adapter.getCount() - 1));
                Log.e("-----", "mList.size() = " + mList.size());
                Log.e("-----", "adapter.datas.size()) = " + adapter.datas.size());
                Log.e("-----", "(adapter.datas.size() - curIndex)) = " + (adapter.datas.size() - curIndex));
                curIndex = index;
                if ((adapter.datas.size() - curIndex) == 3) {
                    mList.addAll(loadData());
                    show();
                }
            }

            @Override
            public void onCardVanish(int index, SwipeCardsView.SlideType type) {
                String orientation = "";
                switch (type) {
                    case LEFT:
                        orientation = "向左飞出";
                        break;
                    case RIGHT:
                        orientation = "向右飞出";
                        break;
                }
                Log.e("----", orientation);
            }

            @Override
            public void onItemClick(View cardImageView, int index) {
                Log.e("-------", "点击了 position=" + index);
                Log.e("-----111--", "值是：" + adapter.datas.get(index - 1).urls);
            }
        });

    }

    private void initLoadData() {
        List<HomeBean> list = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            HomeBean bean = new HomeBean();
            bean.setId(String.valueOf(i));
            bean.setUrls(getUrls().get(i));
            list.add(bean);
        }
        mList.addAll(list);
        show();

    }

    private List<HomeBean> loadData() {
        List<HomeBean> list = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            HomeBean bean = new HomeBean();
            bean.setId(String.valueOf(i));
            bean.setUrls(getUrls().get(i));
            list.add(bean);
        }

        return list;
    }

    private List<String> getUrls() {
        List<String> list = new ArrayList<>();
        list.add("http://olive-room.oss-cn-hongkong.aliyuncs.com/olive-video/test/1/1.jpg");
        list.add("https://party-client.oss-cn-shanghai.aliyuncs.com/1554974041.jpg");
        list.add("http://media.ppparty.cn/profile-img/s1v6dce5f88650b5740f.jpg");
        list.add("https://party-client.oss-cn-shanghai.aliyuncs.com/1554974041.jpg");
        list.add("http://media.ppparty.cn/profile-img/s1v6dce5f88650b5740f.jpg");
        list.add("https://party-client.oss-cn-shanghai.aliyuncs.com/1554974041.jpg");
        list.add("http://olive-room.oss-cn-hongkong.aliyuncs.com/olive-video/test/1/1.jpg");
        list.add("https://party-client.oss-cn-shanghai.aliyuncs.com/1554974041.jpg");
        list.add("http://media.ppparty.cn/profile-img/fps1031111865d87e1ef.jpg");
        list.add("http://media.ppparty.cn/profile-img/fps1031111865d87e1ef.jpg");
        return list;
    }

    /**
     * 显示cardsview
     */
    private void show() {
        if (adapter == null) {
            adapter = new HomeCardAdapter(this, mList);
            swipeCardsView.setAdapter(adapter);
        } else {
            //if you want to change the UI of SwipeCardsView,you must modify the data first
            adapter.setData(mList);
            swipeCardsView.notifyDatasetChanged(curIndex);
        }
    }


}
