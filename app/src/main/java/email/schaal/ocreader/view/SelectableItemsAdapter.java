package email.schaal.ocreader.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;

import email.schaal.ocreader.database.model.Item;
import email.schaal.ocreader.view.drawer.DrawerManager;
import io.realm.Realm;

/**
 * Make item viewholders selectable
 */
public class SelectableItemsAdapter extends ErrorAdapter {
    private final SparseArray<Item> selectedItems = new SparseArray<>();

    public SelectableItemsAdapter(Context context, Realm realm, DrawerManager.State state, ItemViewHolder.OnClickListener clickListener, OnLoadMoreListener loadMoreListener) {
        super(context, realm, state, clickListener, loadMoreListener);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        if(holder instanceof ItemViewHolder)
            ((ItemViewHolder) holder).setSelected(selectedItems.indexOfKey(position) >= 0);
    }

    public void clearSelection() {
        selectedItems.clear();
        notifyDataSetChanged();
    }

    @Nullable
    public Boolean firstSelectedUnread() {
        if(selectedItems.size() > 0) {
            return selectedItems.valueAt(0).isUnread();
        } else {
            return null;
        }
    }

    @Nullable
    public Boolean firstSelectedStarred() {
        if(selectedItems.size() > 0) {
            return selectedItems.valueAt(0).isStarred();
        } else {
            return null;
        }
    }

    public int getSelectedItemsCount() {
        return selectedItems.size();
    }

    public Item[] getSelectedItems() {
        Item[] itemsIterable = new Item[selectedItems.size()];
        for (int index = 0; index < selectedItems.size(); index++) {
            itemsIterable[index] = selectedItems.valueAt(index);
        }
        return itemsIterable;
    }

    public void toggleSelection(Item item, int position) {
        if(selectedItems.indexOfKey(position + headerCount()) >= 0) {
            selectedItems.remove(position + headerCount());
        } else {
            selectedItems.put(position + headerCount(), item);
        }
        notifyItemChanged(position + headerCount());
    }
}
