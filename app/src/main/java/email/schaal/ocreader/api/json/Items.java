package email.schaal.ocreader.api.json;

import java.util.List;

import email.schaal.ocreader.database.model.Item;

/**
 * Class to deserialize the json response for items
 */
public class Items {
    private List<Item> items;

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }
}
