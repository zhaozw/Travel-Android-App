package com.harjup_kdhyne.TravelApp.Translation;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.harjup_kdhyne.TravelApp.MySQLiteHelper;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Paul on 3/1/14.
 * TODO: Write short summary of class
 */
public class TranslationDataSource
{
    private SQLiteDatabase database;
    private MySQLiteHelper dbHelper;
    private String[] translationColumns = {
            MySQLiteHelper.TRANSLATIONS_COLUMN_ID,
            MySQLiteHelper.TRANSLATIONS_COLUMN_HOMEPHRASE,
            MySQLiteHelper.TRANSLATIONS_COLUMN_HOMELANGUAGE,
            MySQLiteHelper.TRANSLATIONS_COLUMN_IMAGEURI
    };

    private String[] phraseColumns = {
            MySQLiteHelper.PHRASE_COLUMN_ID,
            MySQLiteHelper.PHRASE_COLUMN_TRANSLATION_ID,
            MySQLiteHelper.PHRASE_COLUMN_LANGUAGE,
            MySQLiteHelper.PHRASE_COLUMN_CONTENT
    };

    private String[] translationCategoryMapColumns = {
            MySQLiteHelper.TRANSLATIONS_TO_CATEGORY_COLUMN_ID,
            MySQLiteHelper.TRANSLATIONS_TO_CATEGORY_COLUMN_TRANSLATION_ID,
            MySQLiteHelper.TRANSLATIONS_TO_CATEGORY_COLUMN_CATEGORY_ID
    };

    private String[] categoryColumns = {
            MySQLiteHelper.CATEGORY_COLUMN_ID,
            MySQLiteHelper.CATEGORY_COLUMN_NAME
    };

    public TranslationDataSource(Context context){
        dbHelper = new MySQLiteHelper(context);
    }

    public void open() throws SQLException
    {
        database = dbHelper.getWritableDatabase();
        CheckDBContents();
    }

    public void close(){
        dbHelper.close();
    }

    public void saveTranslation(Translation translation)
    {
        //Prepare the translation values to be saved
        ContentValues values = new ContentValues();
        values.put(MySQLiteHelper.TRANSLATIONS_COLUMN_HOMEPHRASE, translation.getHomePhrase());
        values.put(MySQLiteHelper.TRANSLATIONS_COLUMN_HOMELANGUAGE, translation.getHomeLanguage());
        values.put(MySQLiteHelper.TRANSLATIONS_COLUMN_IMAGEURI, translation.getImageId());

        long translationId = translation.getId();

        //Check if the translation exists in the table
        Cursor cursor = database.query(MySQLiteHelper.TRANSLATIONS_TABLE,
                translationColumns,
                MySQLiteHelper.TRANSLATIONS_COLUMN_HOMEPHRASE + " = " + "'" + translation.getHomePhrase() + "'",
                null, null, null, null);

        //If translation exists
        if (cursor.moveToFirst())
        {
            database.update(MySQLiteHelper.TRANSLATIONS_TABLE,
                    values,
                    //MySQLiteHelper.NOTES_COLUMN_ID + "=" + translation.getId(),
                    MySQLiteHelper.TRANSLATIONS_COLUMN_HOMEPHRASE + " = " + "'" + translation.getHomePhrase() + "'",
                    null);

            //Set the translation object's Id to the Id of the found translation in the DB
            translationId = cursor.getLong(cursor.getColumnIndex(translationColumns[0]));

        }
        else
        {
            //Get the ID for the newly inserted translation
            translationId =
                    database.insert(MySQLiteHelper.TRANSLATIONS_TABLE,
                    null,
                    values);


        }
        translation.setId(translationId);

        //Iterate over the phrase objects held by the translation
        //and store them in their respective table
        Set<Map.Entry<String,Phrase>> set = translation.getPhraseHashMap().entrySet();
        Iterator it = set.iterator();
        while(it.hasNext())
        {
            Map.Entry pair = (Map.Entry)it.next();
            Phrase phraseToSave = (Phrase)pair.getValue();
            phraseToSave.setTranslationId(translationId);
            savePhrase(phraseToSave);
            it.remove();
        }

        //For each category in the list...
        List<Category> categoryList = translation.getCategories();

        //We are refreshing the translation-category map to reflect the set in the translation object,
        //So drop all the current map rows for the current translation object...
        database.delete(
                MySQLiteHelper.TRANSLATION_TO_CATEGORY_TABLE,
                translationCategoryMapColumns[1] + " =?",
                new String[]{String.valueOf(translationId)}
                );

        //And then add new rows reflecting the currently selected categories
        for(Iterator<Category> iterator = categoryList.iterator(); iterator.hasNext();)
        {
            Category category = iterator.next();
            long categoryId = saveCategory(category);
            addTranslationCategoryMap(translationId, categoryId);
        }
    }

    public void savePhrase(Phrase phrase)
    {
        //Save the phrase in a similar pattern to the translation
        ContentValues values = new ContentValues();
        values.put(MySQLiteHelper.PHRASE_COLUMN_TRANSLATION_ID, phrase.getTranslationId());
        values.put(MySQLiteHelper.PHRASE_COLUMN_LANGUAGE, phrase.getLanguage());
        values.put(MySQLiteHelper.PHRASE_COLUMN_CONTENT, phrase.getContent());

        long phraseId = phrase.getId();

        //We are looking for a phrase that is in the given language AND is associated with the correct phrase
        //By ignoring a check on context we can update the content value if it changes
        String whereStatement =
                "(" + MySQLiteHelper.PHRASE_COLUMN_LANGUAGE + " = " + "'" + phrase.getLanguage() + "') AND"
                + "(" + MySQLiteHelper.PHRASE_COLUMN_TRANSLATION_ID + " = " + phrase.getTranslationId() + ")";


        Cursor cursor = database.query(MySQLiteHelper.PHRASE_TABLE,
                phraseColumns,
                whereStatement,
                null, null, null, null);

        Boolean phraseExists = cursor.moveToFirst();
                /*checkIfExists(MySQLiteHelper.PHRASE_TABLE,
                phraseColumns,
                MySQLiteHelper.PHRASE_COLUMN_ID,
                String.valueOf(phraseId));*/

        if (phraseExists)
        {
            database.update(MySQLiteHelper.PHRASE_TABLE,
                    values,
                    whereStatement,
                    null);
        }
        else
        {
            database.insert(MySQLiteHelper.PHRASE_TABLE,
                    null,
                    values);
        }

    }

    public long saveCategory(Category category)
    {
        final String myTable =  MySQLiteHelper.CATEGORY_TABLE;
        final String myNameColumn =  MySQLiteHelper.CATEGORY_COLUMN_NAME;

        ContentValues values = new ContentValues();
        values.put(MySQLiteHelper.CATEGORY_COLUMN_NAME, category.getName());


        String categoryName = category.getName();
        long categoryId = category.getId();

       /* Boolean categoryExists =
                checkIfExists(myTable,
                        categoryColumns,
                        myNameColumn,
                        String.valueOf(categoryName));*/



        Cursor c = database.query(myTable,
                categoryColumns,
                myNameColumn + " = " + "'" + categoryName + "'",
                null, null, null, null);

        Boolean categoryExists = c.moveToFirst();

        if (categoryExists)
        {
            database.update(myTable,
                    values,
                    myNameColumn + "=" + "'" + categoryName + "'",
                    null);

            categoryId = c.getLong(c.getColumnIndex(categoryColumns[0]));
        }
        else
        {
            categoryId =
                    database.insert(myTable,
                    null,
                    values);
        }

        return categoryId;
    }

    public void addTranslationCategoryMap(long translationId, long categoryId)
    {
        final String myTable =  MySQLiteHelper.TRANSLATION_TO_CATEGORY_TABLE;




        Boolean entryExists = database.query(myTable,
                translationCategoryMapColumns,
                "(" + translationCategoryMapColumns[1] + " = " + translationId + ") AND ("
                + translationCategoryMapColumns[2] + " = " + categoryId + ")",
                null, null, null, null).moveToFirst();

        if (!entryExists)
        {
            ContentValues values = new ContentValues();
            values.put(translationCategoryMapColumns[1], translationId);
            values.put(translationCategoryMapColumns[2], categoryId);

            database.insert(myTable,
                    null,
                    values);
        }
    }


    public Translation[] getTranslations(){
        return null;
    }

    public Category[] getAllCategories()
    {
        return null;
    }




    //TODO: Refactor and shove in the save methods
    Boolean checkIfExists(String tableName, String[] tableColumns, String key, String value){
        //Check if the entry exists in the table
        return database.query(tableName,
                tableColumns,
                key + " = " + value,
                null, null, null, null).moveToFirst();
    }



    void CheckDBContents(){
        Log.d("database", "Checking db contents...");
        Cursor cursor = database.rawQuery("select * from " + MySQLiteHelper.TRANSLATIONS_TABLE, null);
        if (cursor .moveToFirst()) {

            while (!cursor.isAfterLast()) {

                for (int i = 0; i < translationColumns.length; i++)
                {
                    String name = cursor.getString(cursor
                            .getColumnIndex(translationColumns[i]));
                    Log.d("database", "Translation column: " + translationColumns[i] + " -- "  + name);
                }
                cursor.moveToNext();
            }
        }

        cursor = database.rawQuery("select * from " + MySQLiteHelper.PHRASE_TABLE, null);
        if (cursor .moveToFirst()) {

            while (!cursor.isAfterLast()) {

                for (int i = 0; i < phraseColumns.length; i++)
                {
                    String name = cursor.getString(cursor
                            .getColumnIndex(phraseColumns[i]));
                    Log.d("database", "Phrase column: " + phraseColumns[i] + " -- "  + name);
                }
                cursor.moveToNext();
            }
        }

        cursor = database.rawQuery("select * from " + MySQLiteHelper.CATEGORY_TABLE, null);
        if (cursor .moveToFirst()) {

            while (!cursor.isAfterLast()) {

                for (int i = 0; i < categoryColumns.length; i++)
                {
                    String name = cursor.getString(cursor
                            .getColumnIndex(categoryColumns[i]));
                    Log.d("database", "Category column: " + categoryColumns[i] + " -- "  + name);
                }
                cursor.moveToNext();
            }
        }

        cursor = database.rawQuery("select * from " + MySQLiteHelper.TRANSLATION_TO_CATEGORY_TABLE, null);
        if (cursor .moveToFirst()) {

            while (!cursor.isAfterLast()) {

                for (int i = 0; i < translationCategoryMapColumns.length; i++)
                {
                    String name = cursor.getString(cursor
                            .getColumnIndex(translationCategoryMapColumns[i]));
                    Log.d("database", "Translation-Category column: " + translationCategoryMapColumns[i] + " -- "  + name);
                }
                cursor.moveToNext();
            }
        }

        Log.d("database", "Finished checking db contents...");

    }
}
