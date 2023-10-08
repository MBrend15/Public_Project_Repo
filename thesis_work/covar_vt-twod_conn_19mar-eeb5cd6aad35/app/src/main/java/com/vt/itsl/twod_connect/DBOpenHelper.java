package com.vt.itsl.twod_connect;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by bcmattina on 01-Dec-16.
 */

public class DBOpenHelper extends SQLiteOpenHelper {

    //variables to help android locate existing db
    //private static final String DB_PATH = "/data/data/com.vt.itsl.itsiggle/databases/"
    private static String DB_PATH = "";
    private static final String DB_NAME = "lib_update_08Dec.db";

    private SQLiteDatabase amp_parseql;
    private Context mContext;

    private static final int DB_Version = 1;

    //custom constructor accepts only activity conext. the remaining parameters are handled
    //within the constructor itsel (superClass constructor).
    public DBOpenHelper(Context context) {
        super(context, DB_NAME, null, DB_Version);

        //find build path specific for the api version
        if(Build.VERSION.SDK_INT >= 17){
            DB_PATH = context.getApplicationInfo().dataDir+"/databases/";
        }
        else {
            DB_PATH = "/data/data/"+context.getPackageName()+"/databases/";
        }

        //set class context for the activity received
        mContext = context;
    }

    //following mehtod build off one another to support the create database function

    //check if database exists within local storage
    private boolean checkDatabase()
    {
        File dbFile = new File(DB_PATH+DB_NAME);
        return dbFile.exists();
    }

    //use input and output streams to copy database from assets folder to local storage
    private void copyDatabse() throws IOException{

        //use an input stream to input data into application, interesting
        InputStream mInput = null;
        try {
            mInput = mContext.getAssets().open(DB_NAME);
        } catch (IOException e) {
            Log.i("database",e.toString());
        }
        String outFileName = DB_PATH+DB_NAME;
        OutputStream mOutput = new FileOutputStream(outFileName);

        byte[] mBuffer = new byte[1024];

        int mLength;

        while ((mLength = mInput.read(mBuffer))>0){

            //read and write in 1024 bit chunks until the last chunk, which is less than the
            //original chunks
            mOutput.write(mBuffer,0,mLength);

        }

        mOutput.flush();
        mOutput.close();
        mInput.close();

    }

    public boolean openDataBase() throws SQLException {

        String mPath = DB_PATH+DB_NAME;
        amp_parseql = SQLiteDatabase.openDatabase(mPath,null,SQLiteDatabase.OPEN_READONLY);
        return amp_parseql != null;
    }

    //synchornized function holds the thread until completion of the object?
    public synchronized void close(){

        if(amp_parseql!= null){
            amp_parseql.close();
        }

        super.close();

    }

    public void createDataBase() throws IOException {

        boolean db_exist = checkDatabase();

        if(!db_exist){

            //remember this opens a connection to the readable database
            this.getReadableDatabase();
            this.close(); //commented out as per second example
            try{

                copyDatabse();
                Log.i("database","db created");

            }catch(IOException exception){

                throw new Error("Error copying database!");

            }

        }

    }


    //following are default methods. If database isn't explicitly created then it would
    //be with the onCreate method. Any times new tables/columns etc added, then it's done
    //in on upgrade, also need to adjust database version number.
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
