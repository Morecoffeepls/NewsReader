package com.example.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> mTitles = new ArrayList<>();
    ArrayList<String> mContent = new ArrayList<>();

    ArrayAdapter mArrayAdapter;

    SQLiteDatabase mArticlesDB;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mArticlesDB = this.openOrCreateDatabase("Articles", MODE_PRIVATE,null);

        // clear the DB
        //mArticlesDB.execSQL("DELETE FROM articles ");

        mArticlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleId INTEGER, title VARCHAR, mContent VARCHAR)");


        DownloadTask task = new DownloadTask();
        try{
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        } catch (Exception e){
            e.printStackTrace();
        }

        ListView mListView = findViewById(R.id.articlesListView);
        mArrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, mTitles);
        mListView.setAdapter(mArrayAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent mIntent = new Intent(getApplicationContext(), ArticleActivity.class);
                mIntent.putExtra("content", mContent.get(position));

                startActivity(mIntent);
            }
        });

        updateListView();
    }

    public void updateListView(){
        Cursor mDBCursor = mArticlesDB.rawQuery("SELECT * FROM articles", null);

        int mContentIndex = mDBCursor.getColumnIndex("content");
        int titleIndex = mDBCursor.getColumnIndex("title");

        if (mDBCursor.moveToFirst()){
            mTitles.clear();
            mContent.clear();

            do {
                mTitles.add(mDBCursor.getString(titleIndex));
                mContent.add(mDBCursor.getString(mContentIndex));
            }while (mDBCursor.moveToNext());

            mArrayAdapter.notifyDataSetChanged();
        }
    }

    public class DownloadTask extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... urls) {

            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;

            try {

                url = new URL(urls[0]);

                urlConnection = (HttpURLConnection) url.openConnection();

                InputStream inputStream = urlConnection.getInputStream();

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);

                int data = inputStreamReader.read();

                while (data != -1){
                    char current = (char) data;
                    result += current;
                    data = inputStreamReader.read();
                }

                JSONArray jsonArray = new JSONArray(result);

                int numberOfItems = 20;

                if (jsonArray.length() < 20){
                    numberOfItems = jsonArray.length();
                }

                mArticlesDB.execSQL("DELETE FROM articles");

                for (int i = 0; i < numberOfItems; i++){
                    String articleId = jsonArray.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" +articleId+".json?print=pretty");

                    urlConnection = (HttpURLConnection) url.openConnection();

                    inputStream = urlConnection.getInputStream();

                    inputStreamReader = new InputStreamReader(inputStream);

                    data = inputStreamReader.read();

                    String articleInfo = "";

                    while (data != -1){
                        char current = (char) data;
                        articleInfo += current;
                        data = inputStreamReader.read();
                    }

                    JSONObject jsonObject = new JSONObject(articleInfo);

                    if (!jsonObject.isNull("title") && !jsonObject.isNull("url")){
                        String articleTitle = jsonObject.getString("title");
                        String articleUrl = jsonObject.getString("url");

                        url = new URL(articleUrl);
                        urlConnection = (HttpURLConnection) url.openConnection();
                        inputStream = urlConnection.getInputStream();
                        inputStreamReader = new InputStreamReader(inputStream);
                        data = inputStreamReader.read();
                        String articleContent = "";

                        while (data != -1){
                            char current = (char) data;
                            articleContent += current;
                            data = inputStreamReader.read();
                        }

                        Log.i("HTML", articleContent);

                        String sql = "INSERT INTO articles (articleId, title, mContent) VALUES(?,?,?)";
                        SQLiteStatement statement = mArticlesDB.compileStatement(sql);
                        statement.bindString(1, articleId);
                        statement.bindString(2, articleTitle);
                        statement.bindString(3, articleContent);

                        statement.execute();
                    }

                }


                Log.i("URL Content", result);
                return result;

            } catch (Exception e){
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListView();
        }
    }

}