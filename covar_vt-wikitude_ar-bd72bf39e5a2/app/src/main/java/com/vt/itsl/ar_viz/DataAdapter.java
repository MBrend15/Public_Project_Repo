package com.vt.itsl.ar_viz;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.IOException;

/**
 * Created by bcmattina on 01-Dec-16.
 */

public class DataAdapter {

    private Context mContext;
    private SQLiteDatabase amp_parseql;
    private DBOpenHelper dbOpenHelper;

    //constructor
    public DataAdapter(Context context){

        this.mContext = context;
        dbOpenHelper = new DBOpenHelper(mContext);

    }

    //use helper class methods to create the database
    public DataAdapter createDatabase() throws SQLException, IOException {

        dbOpenHelper.createDataBase();
        return this;

    }

    //the only thing i can reason after starting at this for a while is
    //that you check the connection by opening and closing before you open
    //a readable connection
    public DataAdapter open(){

        try{
            dbOpenHelper.openDataBase();
            dbOpenHelper.close();
            amp_parseql = dbOpenHelper.getReadableDatabase();

        }catch(SQLException excpetion)
        {
            Log.i("database", excpetion.toString());
        }

        return this;
    }

    //this just closes the database
    public void close(){
        dbOpenHelper.close();
    }


    //executes a query on the database, accepts a string from the wifi scan and then
    //queries that mac for ap name and also lat and long information
    public Cursor getData(String query) {

        try {
            Cursor mCur = amp_parseql.rawQuery(query, null);
            if (mCur != null) {
                mCur.moveToNext();
            }
            return mCur;
        } catch (SQLException exception) {

            Log.i("database", exception.toString());
            throw exception;

        }

    }

}
