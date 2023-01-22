package com.pixlbee.heros.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.pixlbee.heros.R
import com.pixlbee.heros.fragments.ItemsAllFragment
import com.pixlbee.heros.fragments.ItemsAvailabilityFragment

class ItemsViewPagerAdapter(
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {

    val mFragmentNames = listOf<String>(
        fragmentActivity.resources.getString(R.string.items_tab_all),
        fragmentActivity.resources.getString(R.string.items_tab_taken)
    )

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ItemsAllFragment()
            1 -> ItemsAvailabilityFragment()
            else -> ItemsAllFragment()
        }
    }

    override fun getItemCount(): Int {
        return 2
    }
}