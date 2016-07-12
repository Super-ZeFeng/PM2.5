package com.zefeng.pm_25;

import android.app.Activity;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.zefeng.pm_25.Adapter.MyPagerAdapter;
import com.zefeng.pm_25.cityobj.City_info;
import com.zefeng.pm_25.db.PM25db;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * 版本1.0；
 * 只可以通过输入城市来实现天气的差询。
 * <p/>
 * 版本2.0：
 * 可以用个列表的选择来选择，将传来的城市列表进行分割，在存进数据库；
 */
public class MainActivity extends Activity implements View.OnClickListener {

    private Button send;
    private TextView cityNameTxt;
    private TextView Txt_colud;
    private TextView updaDat;
    private TextView min;
    private TextView max;
    private TextView pm2_5data;
    private TextView qlty;
    private TextView sr;
    private TextView ss;
    private RelativeLayout layoutOfMain;
    private ImageView img;
    private EditText input;

    private ViewPager pager;
    private List<View> viewList;
    private List<City_info> list;
    private Bitmap bitmap;

    private PM25db PMdb;
    private SQLiteDatabase db;

    private String Key = "&key=5de12a1d35ab4bb6a866ccd1c5904cec";
    //    private String cityName;
    private static int tag = 0;
    private static final int SHOW_RESPONSE = 0;
    private static final int SHOW_PICTURE = 1;

    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_RESPONSE:
                    List<String> list = (List<String>) msg.obj;
                    Log.d("ddd",list.get(0)+"错误");
                    updaDat.setText(list.get(0));
                    qlty.setText(list.get(1));
                    pm2_5data.setText(list.get(2));
                    sr.setText(list.get(3));
                    ss.setText(list.get(4));
                    Txt_colud.setText(list.get(5));
                    min.setText(list.get(9));
                    max.setText(list.get(10));
                    list.clear();
//                    Log.d("ddd",list.toString());
                    break;
                case SHOW_PICTURE:
                    Log.d("ddd", "done");
                    img.setImageBitmap(bitmap);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

//        viewList = new ArrayList<>();
//        View day = View.inflate(this,R.layout.day,null);
//        View night = View.inflate(this,R.layout.night,null);
//        viewList.add(day);
//        viewList.add(night);
//        pager = (ViewPager) findViewById(R.id.viewPager);
//        MyPagerAdapter adapter = new MyPagerAdapter(viewList);
//        pager.setAdapter(adapter);

        send = (Button) findViewById(R.id.send);
        Txt_colud = (TextView) findViewById(R.id.Txt_colud);
        updaDat = (TextView) findViewById(R.id.updaDat);
        min = (TextView) findViewById(R.id.min);
        max = (TextView) findViewById(R.id.max);
        pm2_5data = (TextView) findViewById(R.id.pm2_5data);
        qlty = (TextView) findViewById(R.id.qlty);
        sr = (TextView) findViewById(R.id.sr);
        ss = (TextView) findViewById(R.id.ss);
        img = (ImageView) findViewById(R.id.img);
        cityNameTxt = (TextView) findViewById(R.id.cityName);
//        layoutOfMain = (RelativeLayout) findViewById(R.id.layoutOfMain);
//        layoutOfMain.setVisibility(View.INVISIBLE);

        input = (EditText) findViewById(R.id.input);
        PMdb = new PM25db(this, "PM2_5.db", null, 1);
        db = PMdb.getWritableDatabase();

//        LoadingDB();
        requestCityDB();
//        list = LoadCityId();                         //查找对应城市名的 IDcode
        send.setOnClickListener(this);

    }

//    private void LoadingDB() {
//    }


    @Override
    public void onClick(View v) {
        String idCode = null;
        String cityName = input.getText().toString();
        Cursor cursor = db.query("Province", null, "city=?", new String[]{cityName}, null, null, null);
        if (cursor.moveToFirst()) {
            idCode = cursor.getString(cursor.getColumnIndex("code"));
            Toast.makeText(MainActivity.this, "successs" + idCode, Toast.LENGTH_SHORT).show();
            findTheDetailAndShow(idCode);
        }else{

        }
        //        layoutOfMain.setVisibility(View.VISIBLE);
//        if (v.getId() == R.id.send) {
//            String cityName = input.getText().toString();
//
//            cityNameTxt.setText(cityName);
////            list = LoadCityId();                         //查找对应城市名的 IDcode,错误地方：list放错地方，
////                                                         //导致每次按一次按键就存一次。以至于输入第二个城市查询是出现奔溃；
//            for (int i = 0; i < list.size(); i++) {
//                if (cityName.equals(list.get(i).getCity())) {
//                    idCode = list.get(i).getId();
//                    break;
//                }
//            }
////            send.setClickable(false);
//        }
    }

    /**
     * 将查询到的城市ID再提交，获得详细的天气信息
     *
     * @param idCode
     */
    private void findTheDetailAndShow(final String idCode) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    URL url = new URL("https://api.heweather.com/x3/weather?cityid=" + idCode + Key);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);

                    InputStream is = connection.getInputStream();
                    BufferedReader response = new BufferedReader(new InputStreamReader(is));
                    StringBuilder sb = new StringBuilder();

                    String line;
                    while ((line = response.readLine()) != null) {
                        sb.append(line);
                        Log.d("ddd", "show 。。。。" + sb.toString());
                        analysiJson(sb.toString());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        }).start();
    }

    /**
     * 解析返回来的天气JSON信息，(并判断更新时间是否一致，从JSON再提取中和储存)
     *
     * @param detail
     */
    private void analysiJson(String detail) {
        try {
            List<String> weatherDateList = new ArrayList<>();
//            SharedPreferences pref = getSharedPreferences("weatherData", MODE_PRIVATE);
            JSONObject object = new JSONObject(detail);
            JSONArray array = object.getJSONArray("HeWeather data service 3.0");

            JSONObject jsonObject = array.getJSONObject(0);

            String qlty = " * ";
            String updateTime = jsonObject.getJSONObject("basic").getJSONObject("update").getString("loc");
//            if (!(pref.getString("updateTime", "").equals(updateTime))) {
            weatherDateList.add(updateTime);    // 0  添加到list的序号
            Log.d("ddd",updateTime);
            qlty = jsonObject.getJSONObject("aqi").getJSONObject("city").getString("qlty");
            String pm2_5da = jsonObject.getJSONObject("aqi").getJSONObject("city").getString("pm25");
            weatherDateList.add(qlty);   // 1
            weatherDateList.add(pm2_5da); // 2

            jsonObject = array.getJSONObject(0).getJSONArray("daily_forecast").getJSONObject(0);

            String sr = jsonObject.getJSONObject("astro").getString("sr");
            String ss = jsonObject.getJSONObject("astro").getString("ss");
            weatherDateList.add(sr);    // 3
            weatherDateList.add(ss);    // 4

            String Txt_cond = jsonObject.getJSONObject("cond").getString("txt_d");
            String Txt_condNi = jsonObject.getJSONObject("cond").getString("txt_n");
            String pngCodeD = jsonObject.getJSONObject("cond").getString("code_d");
            String pngCodeN = jsonObject.getJSONObject("cond").getString("code_d");
            weatherDateList.add(Txt_cond);    // 5
            weatherDateList.add(Txt_condNi);  // 6
            weatherDateList.add(pngCodeD);    // 7
            weatherDateList.add(pngCodeN);   // 8

            String min = jsonObject.getJSONObject("tmp").getString("min");
            String max = jsonObject.getJSONObject("tmp").getString("max");
            weatherDateList.add(min);     // 9
            weatherDateList.add(max);     // 10

            Message message = new Message();
            message.what = SHOW_RESPONSE;
            message.obj = weatherDateList;
            handler.sendMessage(message);

            Log.d("ddd", weatherDateList.toString());
            saveInSP(updateTime, qlty, pm2_5da, sr, ss, Txt_cond, Txt_condNi, min, max);
            findThePng(pngCodeD);       //查找对应的图片

            Log.d("ddd", updateTime + "..." + qlty + "..." + sr + "..." + ss + "..." + Txt_cond + "..." + min + "..." + max);
//            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 将信息存到SP中
     *
     * @param updateTime 更新时间
     * @param qlty       空气质量
     * @param sr         日出时间
     * @param ss         日落时间
     * @param txt_condni 夜间天气信息
     * @param txt_cond   日间天气信息
     * @param min        温度最低
     * @param max        最高温度
     */
    private void saveInSP(String updateTime, String qlty, String pm2_5da, String sr, String ss, String txt_cond, String txt_condni, String min, String max) {
        SharedPreferences.Editor editor = getSharedPreferences("weatherData", MODE_PRIVATE).edit();
        editor.putString("updateTime", updateTime);
        editor.putString("pm2_5data", pm2_5da);
        editor.putString("qlty", qlty);
        editor.putString("sr", sr);
        editor.putString("ss", ss);
        editor.putString("txt_condni", txt_condni);
        editor.putString("txt_cond", txt_cond);
        editor.putString("min", min);
        editor.putString("max", max);
        editor.commit();
//        return true;
    }

    /**
     * 查找对应天气的图片
     *
     * @param pngCode
     */
    private void findThePng(final String pngCode) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    URL url = new URL("http://files.heweather.com/cond_icon/" + pngCode + ".png");

                    Log.d("ddd", "go1");
                    InputStream it = url.openStream();
                    bitmap = BitmapFactory.decodeStream(it);
                    handler.sendEmptyMessage(SHOW_PICTURE);
                    it.close();
//
//                    it = url.openStream();
//                    OutputStream os = openFileOutput("cound.png", MODE_PRIVATE);
////                    Log.d("ddd", "go2"
//                    byte[] bytes = new byte[1024];
//                    int len = 0;
//                    while ((len = it.read(bytes)) > 0) {
//                        os.write(bytes, 0, len);
//                    }
//                    it.close();
//                    os.close();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        }).start();
    }

//    /**
//     * 将数据从数据库中取出，为了方便查询，将其实例化并存到list中，
//     * 查询完立刻 clear list列表
//     *
//     * @return
//     */
//    private List<City_info> LoadCityId() {
//        List<City_info> list2 = new ArrayList<>();
//        Cursor cursor = db.query("Province", null, null, null, null, null, null);
//        if (cursor.moveToFirst()) {
//            do {
//                City_info city_info = new City_info();
//                city_info.setCity(cursor.getString(cursor.getColumnIndex("city")));
//                city_info.setCnty(cursor.getString(cursor.getColumnIndex("cnty")));
////                Log.d("ddd",cursor.getString(cursor.getColumnIndex("cnty")));
//                city_info.setProv(cursor.getString(cursor.getColumnIndex("provinceName")));
//                city_info.setId(cursor.getString(cursor.getColumnIndex("code")));
//                list2.add(city_info);
//            } while (cursor.moveToNext());
//        }
//        return list2;
//    }

    /**
     * 请求并返回城市列表
     */
    private void requestCityDB() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    URL url = new URL("https://api.heweather.com/x3/citylist?search=allchina" + Key);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setReadTimeout(8000);
                    connection.setConnectTimeout(8000);
                    InputStream in = connection.getInputStream();

                    BufferedReader reade = new BufferedReader(new InputStreamReader(in));
                    StringBuilder datas = new StringBuilder();
                    String line;
                    int i = 0;
                    while ((line = reade.readLine()) != null) {
                        datas.append(line);
                        parseCityCodeJSON(datas.toString());
//                        Log.d("ddd", i++ + "");
                    }
                    //设计一个下载城市数据库的进度条，利用遍历完是的加一
                    in.close();
                    reade.close();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        }).start();
    }

    /**
     * 将数据存进数据库
     *
     * @param jsonData
     */
    private void parseCityCodeJSON(String jsonData) {

        try {
            ContentValues values = new ContentValues();
            JSONObject jsonObject = new JSONObject(jsonData);
            JSONArray jsonArray = jsonObject.getJSONArray("city_info");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObj = jsonArray.getJSONObject(i);
                values.put("city", jsonObj.getString("city"));
                values.put("cnty", jsonObj.getString("cnty"));
                values.put("code", jsonObj.getString("id"));
                values.put("provinceName", jsonObj.getString("prov"));
                db.insert("Province", null, values);
                values.clear();
//                saveInDB(city, cnty, id, prov);  //保存到数据库中
//                City_info city_info = new City_info();
//                city_info.setCity(jsonObject1.getString("city"));
//                city_info.setCnty(jsonObject1.getString("cnty"));
//                city_info.setId(jsonObject1.getString("id"));
//                city_info.setLat(jsonObject1.getString("lat"));
//                city_info.setLon(jsonObject1.getString("lon"));
//                city_info.setProv(jsonObject1.getString("prov"));
//                list2.add(city_info);
//                for (int j =0;j<list2.size();j++){
//                   City_info city_info1 = list2.get(j);
//                    Log.d("ddd",city_info1.toString());
//                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


//    private void sendRequest(final String cityName) {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                HttpURLConnection connection = null;
//                try {
//                    URL url = new URL("https://api.heweather.com/x3/citylist?search=allchina&key=5de12a1d35ab4bb6a866ccd1c5904cec");
//                    connection = (HttpURLConnection) url.openConnection();
//                    connection.setRequestMethod("GET");
////                    connection.setRequestProperty("apikey","9e99797682748f85e5d82cdab919f537");
//                    connection.setConnectTimeout(8000);
//                    connection.setReadTimeout(8000);
//                    InputStream in = connection.getInputStream();
//
//                    BufferedReader reader = new BufferedReader(new InputStreamReader(in,"UTF-8"));
//                    StringBuilder response = new StringBuilder();
//                    String line;
//                    while ((line = reader.readLine())!= null){
//                        response.append(line);
//                        response.append("\r\n");
//                    }
//                    Message message = new Message();
//                    message.what = SHOW_RESPONSE;
//                    message.obj = response.toString();
//                    handler.sendMessage(message);
//                }catch (Exception e){
//                    e.printStackTrace();
//                }finally {
//                    if (connection != null){
//                        connection.disconnect();
//                    }
//                }
//            }
//        }).start();
//    }
}
