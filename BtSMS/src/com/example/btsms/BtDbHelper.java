package com.example.btsms;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Класс BtSbHelper создает БД btSMS и таблицу 
 * btTable с указанными в методе onCreate записями.
 * 
 * @version 	1.0 17 октября 2014
 * @author 	Карпенко Александр karpenkoAV@ukr.net
 * Лицензия Apache License 2
 */
public class BtDbHelper extends SQLiteOpenHelper {

	public BtDbHelper(Context context) {
		 super(context, "btSMS", null, 1);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		Log.e(this.toString(), "--- onCreate database ---");
		db.execSQL("create table btTable ("
					+ "id integer primary key autoincrement,"
					+ "bank text," //хранит тип банка памяти
					+ "status text," //хранит стату сообщения (прочитано/не прочитано; отвечено/не отвечено тд.)
					+ "date text," //хранит дату прихода смс
					+ "number text, "//хранит номер отправителя
					+ "content text," //содержимое смс
					+ "num text" + ");");//хранит номер смс 
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		
	}

}
