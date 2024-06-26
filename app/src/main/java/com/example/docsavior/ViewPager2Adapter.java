package com.example.docsavior;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPager2Adapter extends FragmentStateAdapter {

    private FragmentNavigation fragmentNavigation;

    public ViewPager2Adapter(@NonNull FragmentActivity fragmentActivity, FragmentNavigation fragmentNavigation) {
        super(fragmentActivity);
        this.fragmentNavigation = fragmentNavigation;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {

        // Khi lướt tới vị trí nào là sẽ hiển thị fragment của page đó
        switch (position) {
            case 0:
                return new NewsfeedFragment();
            case 1:
                return new ConversationFragment();
            case 2:
                return new FriendFragment();
            case 3:
                return new NotificationFragment(fragmentNavigation);
            case 4:
                return new SettingFragment();
        }
        return null;
    }

    @Override
    // Vì có 5 fragment nên sẽ cần trả về 5
    public int getItemCount() {
        return 5;
    }
}
