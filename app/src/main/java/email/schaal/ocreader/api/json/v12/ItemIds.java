package email.schaal.ocreader.api.json.v12;

import java.util.HashSet;
import java.util.Set;

import email.schaal.ocreader.model.Item;

/**
 * Aggregates item ids, used to mark multiple items as read
 */
public class ItemIds {
    private final Set<Long> items = new HashSet<>();

    public ItemIds(Iterable<Item> items) {
        for (Item item : items) {
            this.items.add(item.getId());
        }
    }

    public Set<Long> getItems() {
        return items;
    }
}
