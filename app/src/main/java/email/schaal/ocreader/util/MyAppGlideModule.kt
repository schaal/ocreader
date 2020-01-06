package email.schaal.ocreader.util

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import email.schaal.ocreader.R

/**
 * Created by daniel on 7/15/17.
 */
@GlideModule
class MyAppGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        builder.setDefaultRequestOptions(RequestOptions.placeholderOf(R.drawable.ic_feed_icon))
    }
}