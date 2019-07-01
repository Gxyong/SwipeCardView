# SwipeCardView
仿探探，卡片滑动，Java 版本和kotlin版本，享受版，彻底解决加载图片时出现重影

#使用
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
  dependencies {
	        implementation 'com.github.Gxyong:SwipeCardView:1.0'
	}
  #Android 要想实现滑动卡片堆叠效果，在CardView卡片必须设置属性：
  ##1、 card_view:cardUseCompatPadding="true"
  ##2、com.gxy.java.SwipeCardsView 属性设置： android:clipChildren="true"
  
  #有空再添加卡片上的视频
  
