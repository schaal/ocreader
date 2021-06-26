/*
 * Copyright © 2020. Daniel Schaal <daniel@schaal.email>
 *
 * This file is part of ocreader.
 *
 * ocreader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ocreader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package email.schaal.ocreader.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.launch
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import email.schaal.ocreader.ListActivity
import email.schaal.ocreader.Preferences
import email.schaal.ocreader.R
import email.schaal.ocreader.database.FeedViewModel
import email.schaal.ocreader.databinding.BottomNavigationviewBinding
import email.schaal.ocreader.databinding.UserBottomsheetBinding
import email.schaal.ocreader.util.GlideApp

class UserBottomSheetDialogFragment: BottomSheetDialogFragment() {
    private lateinit var binding: BottomNavigationviewBinding
    private lateinit var headerBinding: UserBottomsheetBinding
    private val feedViewModel: FeedViewModel by viewModels { FeedViewModel.FeedViewModelFactory(requireContext()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = BottomNavigationviewBinding.inflate(inflater, container, true)
        headerBinding = UserBottomsheetBinding.bind(binding.navigationView.getHeaderView(0))
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val url = Preferences.URL.getString(PreferenceManager.getDefaultSharedPreferences(requireContext()))

        feedViewModel.user.observe(viewLifecycleOwner, Observer { user ->
            if(user != null && url != null) {
                headerBinding.textViewUrl.visibility = View.VISIBLE
                headerBinding.textViewUser.text = "${user.displayName} (${user.userId})"
                headerBinding.textViewUrl.text = url
                GlideApp.with(this).load(user.avatarUrl(url))
                    .placeholder(R.drawable.account_circle)
                    .into(headerBinding.imageviewAvatar)
            } else {
                headerBinding.textViewUser.text = getString(R.string.prompt_username)
                headerBinding.textViewUrl.visibility = View.GONE
            }
        })

        binding.navigationView.setNavigationItemSelectedListener {
            dismiss()
            (requireActivity() as? ListActivity)?.onNavigationItemClick(it) ?: false
        }

        headerBinding.button.setOnClickListener {
            (requireActivity() as? ListActivity)?.let {
                it.getLoginResult.launch()
            }
            dismiss()
        }
    }

}