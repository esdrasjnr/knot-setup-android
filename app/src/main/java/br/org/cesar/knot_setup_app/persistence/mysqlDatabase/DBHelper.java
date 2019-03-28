package br.org.cesar.knot_setup_app.persistence.mysqlDatabase;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

import static android.content.Context.MODE_PRIVATE;
import static android.database.sqlite.SQLiteDatabase.openOrCreateDatabase;

public class DBHelper extends SQLiteOpenHelper {

    public DBHelper(Context context) {
        super(context, "KNoT_Devices" , null, 1);
    }

    //TODO: Find out the types of each column
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table KNoT_Devices " + "(id integer primary key, name text,Channel text,NetName text, PanID text,XpanID text, Masterkey text, IPV6 text)");
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS devices");
        onCreate(db);
    }

    //TODO: Find out the types of each column
    public boolean insertDevice (Integer id,String name, String channel, String netName, String panID,String xpanID, String masterkey, String ipv6) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("id", id);
        contentValues.put("name", name);
        contentValues.put("Channel", channel);
        contentValues.put("NetName", netName);
        contentValues.put("PanID", panID);
        contentValues.put("XpanID", xpanID);
        contentValues.put("MasterKey", masterkey);
        contentValues.put("IPV6", ipv6);
        db.insert("KNoT_Devices", null, contentValues);
        return true;
    }

    public Cursor getData(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from KNoT_Devices where id="+id+"", null );
        return res;
    }
    
    public boolean updateKNoTDevice (Integer id,String name, String channel, String netName, String panID,String xpanID, String masterkey, String ipv6) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("name", name);
        contentValues.put("Channel", channel);
        contentValues.put("NetName", netName);
        contentValues.put("PanID", panID);
        contentValues.put("XpanID", xpanID);
        contentValues.put("MasterKey", masterkey);
        contentValues.put("IPV6", ipv6);
        db.update("KNoT_Devices", contentValues, "id = ? ", new String[] { Integer.toString(id) } );
        return true;
    }

    public Integer deleteKNoTDevice (Integer id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("KNoT_Devices",
                "id = ? ",
                new String[] { Integer.toString(id) });
    }

    public ArrayList<String> getAllKNoTDevices() {
        ArrayList<String> array_list = new ArrayList<String>();

        //hp = new HashMap();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from KNoT_Devices", null );
        res.moveToFirst();

        while(res.isAfterLast() == false){
            array_list.add(res.getString(res.getColumnIndex("name")));
            res.moveToNext();
        }
        return array_list;
    }

}