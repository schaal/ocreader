package email.schaal.ocreader.api.json.v12;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import email.schaal.ocreader.database.model.Item;

/**
 * Aggregates feedIds and guidHashes, used to mark multiple items as starred
 */
public class ItemMap {
    private final Set<Map<String, Object>> items = new HashSet<>();

    public ItemMap(Iterable<Item> items) {
        for (Item item : items) {
            HashMap<String, Object> itemMap = new HashMap<>();
            itemMap.put("feedId", item.getFeedId());
            itemMap.put("guidHash", item.getGuidHash());
            this.items.add(itemMap);
        }
    }

    public Set<Map<String, Object>> getItems() {
        return items;
    }
}
