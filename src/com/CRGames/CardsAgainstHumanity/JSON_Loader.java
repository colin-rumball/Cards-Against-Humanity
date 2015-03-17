package com.CRGames.CardsAgainstHumanity;

import java.io.FileReader;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Created by Colin on 23/02/2015.
 */
public class JSON_Loader
{
    JSONArray allObjects;
    @SuppressWarnings("unchecked")
    public JSON_Loader(String file)
    {

        JSONParser parser = new JSONParser();
        try {
            System.out.println("Reading JSON data from "+file);
            FileReader fileReader = new FileReader(file);
            JSONObject json = (JSONObject) parser.parse(fileReader);
            //String title = (String) json.get("masterCards");
            //String author = (String) json.get("author");
            //System.out.println("title: " + title);
            //System.out.println("author: " + author);
            allObjects = (JSONArray) json.get("masterCards");
            /*Iterator i = allObjects.iterator();
            while (i.hasNext()) {
                JSONObject obj = (JSONObject) i.next();
                System.out.println(" " + obj);
            }*/
        } catch (Exception ex) {
            System.out.println("JSON loader exception");
            ex.printStackTrace();
        }
    }

    public JSONArray getAllObjectsWithValue(String key, String Value)
    {
        JSONArray returnObjects = new JSONArray();
        Iterator i = allObjects.iterator();
        while (i.hasNext())
        {
            JSONObject obj = (JSONObject) i.next();
            if (obj.get(key).equals(Value))
            {
                returnObjects.add(obj);
            }
        }
        return  returnObjects;
    }
}
