package email.schaal.ocreader.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.annotation.LayoutRes
import email.schaal.ocreader.R
import email.schaal.ocreader.database.model.Folder
import io.realm.OrderedRealmCollection

/**
 * Adapter for Spinner to display Folders
 */
class FolderSpinnerAdapter(context: Context, private val folders: OrderedRealmCollection<Folder>) : BaseAdapter() {
    private val rootFolder: String = context.getString(R.string.root_folder)

    override fun getCount(): Int {
        return folders.size + 1
    }

    override fun getItem(position: Int): Any {
        return if (position == 0) rootFolder else folders[position - 1].name
    }

    override fun getItemId(position: Int): Long {
        return if (position == 0) 0 else folders[position - 1].id
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_NONE else VIEW_TYPE_FOLDER
    }

    override fun getDropDownView(position: Int, convertView: View, parent: ViewGroup): View {
        return getSpinnerView(position, convertView, parent, R.layout.spinner_folder_dropdown)
    }

    override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
        return getSpinnerView(position, convertView, parent, R.layout.spinner_folder)
    }

    private fun getSpinnerView(position: Int, convertView: View?, parent: ViewGroup, @LayoutRes layout: Int): View {
        val view = convertView ?: View.inflate(parent.context, layout, null)
        (view as TextView).text = getItem(position) as String
        return view
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    fun getPosition(folderId: Long): Int {
        if (folderId == 0L) return 0
        var i = 1
        val foldersSize = folders.size
        while (i - 1 < foldersSize) {
            if (folders[i - 1].id == folderId) {
                return i
            }
            i++
        }
        return -1
    }

    companion object {
        private const val VIEW_TYPE_NONE = 0
        private const val VIEW_TYPE_FOLDER = 1
    }

}