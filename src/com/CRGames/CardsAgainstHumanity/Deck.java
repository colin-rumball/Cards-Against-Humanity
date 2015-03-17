package com.CRGames.CardsAgainstHumanity;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Random;

/**
 * Created by Colin on 23/02/2015.
 */
public class Deck
{
    public JSONArray deck;

    public Deck(JSONArray _deck)
    {
        deck = _deck;
    }

    public JSONObject getRandomCard()
    {
        if (deck.size() > 0) {
            Random rand = new Random();
            int n = rand.nextInt(deck.size());
            JSONObject card = (JSONObject) deck.get(n);
            deck.remove(n);

            return card;
        }
        return null;
    }

    public void refillAndShuffle(JSONArray _deck)
    {
        refill(_deck);
    }
    public void refill(JSONArray _deck)
    {
        deck = _deck;
    }

    public void shuffle()
    {

    }
}