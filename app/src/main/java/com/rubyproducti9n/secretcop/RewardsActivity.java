package com.rubyproducti9n.secretcop;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RewardsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvDisclaimer;
    private RewardsAdapter adapter;
    private List<RewardItem> rewardList;

    private FirebaseUser currentUser;
    private DatabaseReference userRewardsRef, allCouponsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rewards);

        recyclerView = findViewById(R.id.recyclerRewards);
        tvDisclaimer = findViewById(R.id.tvDisclaimer);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        rewardList = new ArrayList<>();
        adapter = new RewardsAdapter(rewardList);
        recyclerView.setAdapter(adapter);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        userRewardsRef = FirebaseDatabase.getInstance().getReference("users")
                .child(currentUser.getUid())
                .child("rewards");

        allCouponsRef = FirebaseDatabase.getInstance().getReference("coupons");

        loadRewards();

        tvDisclaimer.setText("Disclaimer: This app is an independent civic initiative and is not affiliated, associated, authorized, endorsed by, or in any way officially connected with any brand, product, or service listed.");
    }

    private void loadRewards() {
        Log.d("RewardsDebug", "Fetching rewards...");
        userRewardsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                rewardList.clear();
                Log.d("RewardsDebug", "User rewards found: " + snapshot.getChildrenCount());

                for (DataSnapshot rewardSnap : snapshot.getChildren()) {
                    String couponId = rewardSnap.getValue(String.class);
                    Log.d("RewardsDebug", "Reward couponId: " + couponId);

                    if (couponId != null && !couponId.isEmpty()) {
                        allCouponsRef.child(couponId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                Log.d("RewardsDebug", "Coupon data snapshot: " + dataSnapshot);

                                RewardItem item = dataSnapshot.getValue(RewardItem.class);
                                if (item != null) {
                                    Log.d("RewardsDebug", "Coupon title: " + item.getTitle());
                                    if (!item.isClaimed()) {
                                        rewardList.add(item);
                                        adapter.notifyDataSetChanged();
                                    } else {
                                        Log.d("RewardsDebug", "Coupon already claimed.");
                                    }
                                } else {
                                    Log.d("RewardsDebug", "RewardItem is null.");
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("RewardsDebug", "Coupon read failed: " + error.getMessage());
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("RewardsDebug", "User rewards read failed: " + error.getMessage());
            }
        });

    }

    static class RewardsAdapter extends RecyclerView.Adapter<RewardsAdapter.ViewHolder> {

        private final List<RewardItem> list;

        RewardsAdapter(List<RewardItem> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reward, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            RewardItem item = list.get(position);
            holder.tvTitle.setText(item.getTitle());
            holder.tvDescription.setText(item.getDescription());
            holder.tvExpiry.setText("Expires on: " + formatDate(item.getExpiryDate()));

            Picasso.get().load(item.getImageUrl()).into(holder.imgBanner);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imgBanner;
            TextView tvTitle, tvDescription, tvExpiry;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                imgBanner = itemView.findViewById(R.id.imgBanner);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvDescription = itemView.findViewById(R.id.tvDescription);
                tvExpiry = itemView.findViewById(R.id.tvExpiry);
            }
        }

        private String formatDate(long millis) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            return sdf.format(new Date(millis));
        }
    }

    public static class RewardItem {
        private String title;
        private String description;
        private String imageUrl;
        private long expiryDate;
        private boolean isClaimed;

        public RewardItem() {}

        public RewardItem(String title, String description, String imageUrl, long expiryDate, boolean isClaimed) {
            this.title = title;
            this.description = description;
            this.imageUrl = imageUrl;
            this.expiryDate = expiryDate;
            this.isClaimed = isClaimed;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public long getExpiryDate() {
            return expiryDate;
        }

        public boolean isClaimed() {
            return isClaimed;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public void setExpiryDate(long expiryDate) {
            this.expiryDate = expiryDate;
        }

        public void setClaimed(boolean claimed) {
            isClaimed = claimed;
        }
    }
}