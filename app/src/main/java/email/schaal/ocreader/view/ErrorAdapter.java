package email.schaal.ocreader.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import email.schaal.ocreader.R;
import email.schaal.ocreader.database.model.Feed;
import email.schaal.ocreader.view.drawer.DrawerManager;
import io.realm.Realm;

/**
 * Show a error viewholder for Feeds with too many update failures
 */
public class ErrorAdapter extends LoadMoreAdapter {
    public ErrorAdapter(Context context, Realm realm, DrawerManager.State state, ItemViewHolder.OnClickListener clickListener, OnLoadMoreListener loadMoreListener) {
        super(context, realm, state, clickListener, loadMoreListener);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == R.id.viewtype_error) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_error, parent, false);
            return new ErrorViewHolder(view);
        } else {
            return super.onCreateViewHolder(parent, viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(holder instanceof ErrorViewHolder) {
            ((ErrorViewHolder) holder).bindError(((Feed)state.getTreeItem()).getLastUpdateError());
        } else {
            super.onBindViewHolder(holder, position - headerCount());
        }
    }

    @Override
    public long getItemId(int position) {
        if(position == 0 && hasError())
            return -1;
        else
            return super.getItemId(position - headerCount());
    }

    @Override
    public int getItemCount() {
        return super.getItemCount() + headerCount();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 && hasError())
            return R.id.viewtype_error;
        return super.getItemViewType(position - headerCount());
    }

    protected int headerCount() {
        return hasError() ? 1 : 0;
    }

    private boolean hasError() {
        return state.getTreeItem() instanceof Feed && ((Feed) state.getTreeItem()).isConsideredFailed();
    }

    private class ErrorViewHolder extends RecyclerView.ViewHolder {
        private final TextView errorTextView;

        ErrorViewHolder(View itemView) {
            super(itemView);
            errorTextView = itemView.findViewById(R.id.textViewError);
        }

        void bindError(String error) {
            errorTextView.setText(error);
        }
    }

}
