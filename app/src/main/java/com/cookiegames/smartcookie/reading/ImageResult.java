package com.cookiegames.smartcookie.reading;

import org.jsoup.nodes.Element;

/**
 * Class which encapsulates the data from an image found under an element
 * 
 * @author Chris Alexander, chris@chris-alexander.co.uk
 */
class ImageResult {

    public final Integer weight;
    public Element element;

    public ImageResult(String src, Integer weight, String title, int height, int width, String alt,
            boolean noFollow) {
        this.weight = weight;
    }
}
