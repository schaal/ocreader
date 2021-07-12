package email.schaal.ocreader.database.model

import android.content.Context
import androidx.annotation.StringRes

abstract class SpecialFolder(@StringRes private val name_res: Int): TreeItem {
    override fun treeItemName(context: Context): String {
        return context.getString(name_res)
    }
}