package email.schaal.ocreader.view;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import email.schaal.ocreader.R;
import email.schaal.ocreader.database.model.Feed;
import email.schaal.ocreader.database.model.Folder;
import email.schaal.ocreader.database.model.TreeItem;
import email.schaal.ocreader.databinding.ListLoadmoreBinding;
import email.schaal.ocreader.view.drawer.DrawerManager;
import io.realm.Realm;
import io.realm.Sort;

/**
 * Add a Load More item to ItemsAdapter
 */
public class LoadMoreAdapter extends ItemsAdapter {
    private final ItemsAdapter.OnLoadMoreListener loadMoreListener;
    private TreeItem loadingMoreTreeItem;

    public LoadMoreAdapter(Realm realm, DrawerManager.State state, ItemViewHolder.OnClickListener clickListener, Sort order, String sortField, OnLoadMoreListener loadMoreListener) {
        super(realm, state, clickListener, order, sortField);
        this.loadMoreListener = loadMoreListener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == VIEW_TYPE_LOADMORE) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_loadmore, parent, false);
            return new LoadMoreViewHolder(view);
        } else {
            return super.onCreateViewHolder(parent, viewType);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if(holder instanceof LoadMoreViewHolder) {
            ((LoadMoreViewHolder) holder).showProgress(
                    loadingMoreTreeItem != null && loadingMoreTreeItem.equals(state.getTreeItem()));
        } else {
            super.onBindViewHolder(holder, position);
        }
    }

    @Override
    public int getItemCount() {
        return super.getItemCount() + footerCount();
    }

    protected int footerCount() {
        return hasLoadMore() ? 1 : 0;
    }

    @Override
    public long getItemId(int position) {
        if(position == getItemCount() && hasLoadMore())
            return -1;
        else {
            return super.getItemId(position);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if(hasLoadMore() && position == super.getItemCount())
            return VIEW_TYPE_LOADMORE;
        return super.getItemViewType(position);
    }

    private boolean hasLoadMore() {
        return state.getTreeItem() instanceof Feed || state.getTreeItem() instanceof Folder;
    }

    public void resetLoadMore() {
        loadingMoreTreeItem = null;
    }

    private class LoadMoreViewHolder extends RecyclerView.ViewHolder {
        private final static int CHILD_BUTTON_INDEX = 0;
        private final static int CHILD_PROGRESS_INDEX = 1;

        private final ListLoadmoreBinding binding;

        LoadMoreViewHolder(View itemView) {
            super(itemView);

            binding = ListLoadmoreBinding.bind(itemView);

            binding.buttonLoadMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showProgress(true);
                    loadingMoreTreeItem = state.getTreeItem();
                    loadMoreListener.onLoadMore(state.getTreeItem());
                }
            });
        }

        void showProgress(boolean loading) {
            if(loading)
                binding.loadMoreViewSwitcher.setDisplayedChild(CHILD_PROGRESS_INDEX);
            else
                binding.loadMoreViewSwitcher.setDisplayedChild(CHILD_BUTTON_INDEX);
        }
    }
}
