/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.materialstudies.reply.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.transition.MaterialElevationScale
import com.google.android.material.transition.MaterialFadeThrough
import com.materialstudies.reply.R
import com.materialstudies.reply.data.Email
import com.materialstudies.reply.data.EmailStore
import com.materialstudies.reply.databinding.FragmentHomeBinding
import com.materialstudies.reply.ui.MainActivity
import com.materialstudies.reply.ui.MenuBottomSheetDialogFragment
import com.materialstudies.reply.ui.nav.NavigationModel

/**
 * A [Fragment] that displays a list of emails.
 */
class HomeFragment : Fragment(), EmailAdapter.EmailAdapterListener {

    private val args: HomeFragmentArgs by navArgs()

    private lateinit var binding: FragmentHomeBinding

    private val emailAdapter = EmailAdapter(this)

    // An on back pressed callback that handles replacing any non-Inbox HomeFragment with inbox
    // on back pressed.
    private val nonInboxOnBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            NavigationModel.setNavigationMenuItemChecked(NavigationModel.INBOX_ID)
            (requireActivity() as MainActivity)
                .navigateToHome(R.string.navigation_inbox, Mailbox.INBOX);
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    //由于HomeFragment设置了singleTop=true，意味着不存在从HomeFragment后退的情况，所以不用设置returnTransition
      enterTransition = MaterialFadeThrough().apply {
        duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
      }
      Log.i("TAG", "onCreate: $this")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
      Log.i("TAG", "onViewCreated: $this")
        postponeEnterTransition()
      binding.recyclerView.doOnPreDraw { startPostponedEnterTransition() }
        // Only enable the on back callback if this home fragment is a mailbox other than Inbox.
        // This is to make sure we always navigate back to Inbox before exiting the app.
        nonInboxOnBackCallback.isEnabled = args.mailbox != Mailbox.INBOX
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            nonInboxOnBackCallback
        )

        binding.recyclerView.apply {
            val itemTouchHelper = ItemTouchHelper(ReboundingSwipeActionCallback())
            itemTouchHelper.attachToRecyclerView(this)
            adapter = emailAdapter
        }
        binding.recyclerView.adapter = emailAdapter

        EmailStore.getEmails(args.mailbox).observe(viewLifecycleOwner) {
            emailAdapter.submitList(it)
        }
    }

    override fun onEmailClicked(cardView: View, email: Email) {
        // TODO: Set up MaterialElevationScale transition as exit and reenter transitions.
      exitTransition = MaterialElevationScale(false).apply {
        duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
      }
      reenterTransition = MaterialElevationScale(true).apply {
        duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
      }
      val transitionName = getString(R.string.email_card_detail_transition_name)
      val navigatorExtras = FragmentNavigatorExtras(cardView to transitionName)
        val directions = HomeFragmentDirections.actionHomeFragmentToEmailFragment(email.id)
        findNavController().navigate(directions,navigatorExtras)
    }

    override fun onEmailLongPressed(email: Email): Boolean {
        MenuBottomSheetDialogFragment
          .newInstance(R.menu.email_bottom_sheet_menu)
          .show(parentFragmentManager, null)

        return true
    }

    override fun onEmailStarChanged(email: Email, newValue: Boolean) {
        EmailStore.update(email.id) { isStarred = newValue }
    }

    override fun onEmailArchived(email: Email) {
        EmailStore.delete(email.id)
    }

  override fun onDestroyView() {
    super.onDestroyView()
    Log.i("TAG", "onDestroyView: $this")
  }

  override fun onDestroy() {
    super.onDestroy()
    Log.i("TAG", "onDestroy: $this")
  }
}
