package email.schaal.ocreader.view;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import email.schaal.ocreader.R;
import email.schaal.ocreader.database.model.TreeItem;
import email.schaal.ocreader.databinding.ListLoadmoreBinding;
import email.schaal.ocreader.view.drawer.DrawerManager;
import io.realm.Realm;

/**
 * Add a Load More item to ItemsAdapter
 */
public class LoadMoreAdapter extends ItemsAdapter {
    private final OnLoadMoreListener loadMoreListener;
    private TreeItem loadingMoreTreeItem;

    public LoadMoreAdapter(Context context, Realm realm, DrawerManager.State state, ItemViewHolder.OnClickListener clickListener, OnLoadMoreListener loadMoreListener) {
        super(context, realm, state, clickListener);
        this.loadMoreListener = loadMoreListener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == R.id.viewtype_loadmore) {
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
        return state.getTreeItem().canLoadMore() ? 1 : 0;
    }

    @Override
    public long getItemId(int position) {
        if(position == getItemCount() && state.getTreeItem().canLoadMore())
            return -1;
        else {
            return super.getItemId(position);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if(state.getTreeItem().canLoadMore() && position == super.getItemCount())
            return R.id.viewtype_loadmore;
        return super.getItemViewType(position);
    }

    public void resetLoadMore() {
        loadingMoreTreeItem = null;
    }

    public interface OnLoadMoreListener {
        void onLoadMore(TreeItem treeItem);
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
