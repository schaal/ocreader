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

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Base64InputStream
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import email.schaal.ocreader.LoginActivity
import email.schaal.ocreader.Preferences
import email.schaal.ocreader.R
import email.schaal.ocreader.database.FeedViewModel
import email.schaal.ocreader.databinding.UserBottomsheetBinding
import java.io.ByteArrayInputStream

class UserBottomSheetDialogFragment: BottomSheetDialogFragment() {
    private lateinit var binding: UserBottomsheetBinding
    private val feedViewModel: FeedViewModel by viewModels { FeedViewModel.FeedViewModelFactory(requireContext()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = UserBottomsheetBinding.inflate(inflater, container, true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        feedViewModel.userLiveData.observe(viewLifecycleOwner, Observer { user ->
            if(user != null) {
                val url = Preferences.URL.getString(PreferenceManager.getDefaultSharedPreferences(requireContext()))
                binding.textViewUrl.visibility = View.VISIBLE
                binding.textViewUser.text = user.displayName
                binding.textViewUrl.text =  "${user.userId}@${url}"
                user.avatar?.let {
                    binding.imageviewAvatar.setImageBitmap(BitmapFactory.decodeStream(Base64InputStream(ByteArrayInputStream(it.toByteArray()), Base64.DEFAULT)))
                }
            } else {
                binding.textViewUser.text = getString(R.string.prompt_username)
                binding.textViewUrl.visibility = View.GONE
            }
        })

        binding.button.setOnClickListener {
            val loginIntent = Intent(requireActivity(), LoginActivity::class.java)
            startActivityForResult(loginIntent, LoginActivity.REQUEST_CODE)
            dismiss()
        }
    }

}