
package com.nio.ocr.ektp.mobile.db;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;
import com.nio.ocr.ektp.mobile.Constants;
import com.nio.ocr.ektp.mobile.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class DBHelper extends SQLiteOpenHelper {

    private final String TAG = Constants.TAG;

    private Context context = null;
    private static final String DB_Name =  "idektpocr.db";
    public static final int DB_VERSION = 2;
    private static final String CREATE_TABLE_Provinsi   = "CREATE TABLE provinsi      ( id_prov TEXT PRIMARY KEY, name TEXT NOT NULL)";
    private static final String CREATE_TABLE_kabupaten  = "CREATE TABLE kabupaten    ( id_kab TEXT PRIMARY KEY, name TEXT NOT NULL)";
    private static final String CREATE_TABLE_kecamatan  = "CREATE TABLE kecamatan    ( id_kec TEXT PRIMARY KEY, name TEXT NOT NULL)";
    private static final String CREATE_TABLE_config  = "CREATE TABLE config    ( cd TEXT PRIMARY KEY, cdvalue TEXT NOT NULL)";

    private static DBHelper sInstance = null;

    public static DBHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DBHelper(context.getApplicationContext());
        }

        return sInstance;
    }

    private DBHelper(Context context) {
        super(context, DB_Name, null, DB_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_Provinsi);
        db.execSQL(CREATE_TABLE_kabupaten);
        db.execSQL(CREATE_TABLE_kecamatan);
        db.execSQL(CREATE_TABLE_config);


        InputStream isProvinsi = this.context.getResources().openRawResource(R.raw.provinsi);
        populateData(db,isProvinsi);

        InputStream isKabupaten = this.context.getResources().openRawResource(R.raw.kabupaten);
        populateData(db,isKabupaten);

        InputStream isKecamatan = this.context.getResources().openRawResource(R.raw.kecamatan);
        populateData(db,isKecamatan);


        db.execSQL("insert into config (cd,cdvalue) values ('ocrdataversion','0')");


    }

    private void populateData(SQLiteDatabase db,InputStream is) {

        try {

            InputStreamReader isr = (new InputStreamReader(is,"UTF-8"));

            BufferedReader br =  new BufferedReader(isr);
            String s;
            while ((s = br.readLine()) != null) {
                db.execSQL(s);
            }

            br.close();

        } catch (IOException e) {
            Log.e(TAG, "Error loading init SQL from raw provinsi", e);
        } catch (SQLException e) {
            Log.e(TAG, "Error executing init SQL", e);
        } finally {
            if (is!=null) {
                try {is.close();} catch(Exception e){
                    Log.e(TAG,e.getMessage(),e);
                }
            }
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        if (oldVersion==1 && newVersion ==2) {
            db.execSQL(CREATE_TABLE_config);
            db.execSQL("insert into config (cd,cdvalue) values ('ocrdataversion','0')");
        }
    }

    public String lookupProvinsi(String provinceId) {

        String status = null;

        SQLiteDatabase db = getReadableDatabase();

        String SELECT_QUERY = "SELECT name FROM provinsi WHERE id_prov = ?";

        Cursor cr = db.rawQuery(SELECT_QUERY, new String[]{provinceId});

        while (cr.moveToNext()) {
            status = cr.getString(0)+"?";
        }

        cr.close();

        return status;

    }
    public String getOcrDataVersion() {

        String status = "0";

        SQLiteDatabase db = getReadableDatabase();

        String SELECT_QUERY = "SELECT cdvalue FROM config WHERE cd = ? ";

        Cursor cr = db.rawQuery(SELECT_QUERY,new String[] {"ocrdataversion"});

        while (cr.moveToNext()) {
            status = cr.getString(0);
        }

        cr.close();

        return status;

    }

    public void updateOCRDataVersion(String newVersion) {

        SQLiteDatabase db = getWritableDatabase();
        SQLiteStatement stmt = db.compileStatement("UPDATE config SET cdvalue = ? where cd = 'ocrdataversion'");
        stmt.bindString(1, newVersion);
        stmt.execute();
        stmt.close();
    }


    public String lookupKabupaten(String kabupatenId) {

        String status = null;

        SQLiteDatabase db = getReadableDatabase();

        String SELECT_QUERY = "SELECT name FROM kabupaten WHERE id_kab = ?";

        Cursor cr = db.rawQuery(SELECT_QUERY, new String[]{kabupatenId});

        while (cr.moveToNext()) {
            status = cr.getString(0) +"?";
        }

        cr.close();

        return status;

    }
    public String lookupKecamatan(String kecamatanId) {

        String status = null;

        SQLiteDatabase db = getReadableDatabase();

        String SELECT_QUERY = "SELECT name FROM kecamatan WHERE id_kec = ?";

        Cursor cr = db.rawQuery(SELECT_QUERY, new String[]{kecamatanId});

        while (cr.moveToNext()) {
            status = cr.getString(0) +"?";
        }

        cr.close();

        return status;

    }

}

