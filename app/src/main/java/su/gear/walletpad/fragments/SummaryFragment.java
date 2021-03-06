package su.gear.walletpad.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionMenu;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import su.gear.walletpad.EditActivity;
import su.gear.walletpad.R;
import su.gear.walletpad.adapters.ChildrenPagesAdapter;
import su.gear.walletpad.adapters.OperationsAdapter;
import su.gear.walletpad.adapters.PlansAdapter;
import su.gear.walletpad.adapters.WalletsAdapter;
import su.gear.walletpad.model.Operation;
import su.gear.walletpad.model.OperationsListItem;
import su.gear.walletpad.model.Plan;
import su.gear.walletpad.model.PlansListItem;
import su.gear.walletpad.model.Separator;
import su.gear.walletpad.model.Wallet;
import su.gear.walletpad.model.WalletsListItem;
import su.gear.walletpad.utils.DateUtils;


public class SummaryFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = SummaryFragment.class.getSimpleName();

    private List<OperationsListItem> operations;
    private List<PlansListItem> plans;
    private List<WalletsListItem> wallets;

    private TextView tabSummaryError;
    private ProgressBar tabSummaryProgress;
    private RecyclerView tabSummaryRecycler;

    private TextView tabWalletsError;
    private ProgressBar tabWalletsProgress;
    private RecyclerView tabWalletsRecycler;

    private TextView tabPlansError;
    private ProgressBar tabPlansProgress;
    private RecyclerView tabPlansRecycler;

    private TextView totalAmount;
    private FloatingActionMenu menu;
    private double amount = 0;
    private double spent = 0;

    private ValueEventListener summaryListener;
    private DatabaseReference operationsReference;

    private ValueEventListener plansListener;
    private DatabaseReference plansReference;

    private ValueEventListener walletsListener;
    private DatabaseReference walletsReference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        operations = new ArrayList<>();
        plans = new ArrayList<>();
        wallets = new ArrayList<>();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_summary, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        final TextView summaryAmount = (TextView) view.findViewById(R.id.summary_amount);
        summaryAmount.setText(String.valueOf(amount - spent) + " $");

        ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
        final View overlay = view.findViewById(R.id.overlay);
        menu = (FloatingActionMenu) view.findViewById(R.id.add_menu);
        menu.setOnMenuToggleListener(new FloatingActionMenu.OnMenuToggleListener() {
            @Override
            public void onMenuToggle(boolean opened) {
                overlay.setVisibility(opened ? View.VISIBLE : View.GONE);
            }
        });
        overlay.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (overlay.getVisibility() == View.VISIBLE) {
                    overlay.setVisibility(View.GONE);
                    menu.close(true);
                }
                return true;
            }
        });

        view.findViewById(R.id.add_menu_income).setOnClickListener(this);
        view.findViewById(R.id.add_menu_expense).setOnClickListener(this);
        view.findViewById(R.id.add_menu_transfer).setOnClickListener(this);
        view.findViewById(R.id.add_menu_plan).setOnClickListener(this);
        view.findViewById(R.id.add_menu_wallet).setOnClickListener(this);

        ChildrenPagesAdapter pagerAdapter = new ChildrenPagesAdapter(viewPager);
        viewPager.setAdapter(pagerAdapter);
        TabLayout tabLayout = (TabLayout) view.findViewById(R.id.tablayout);
        tabLayout.setupWithViewPager(viewPager);

        tabSummaryError = (TextView) pagerAdapter.findViewById(R.id.tab_summary_error);
        tabSummaryProgress = (ProgressBar) pagerAdapter.findViewById(R.id.tab_summary_progress);
        tabSummaryRecycler = (RecyclerView) pagerAdapter.findViewById(R.id.tab_summary_recycler);

        final OperationsAdapter operationsAdapter = new OperationsAdapter(getActivity(), operations);
        tabSummaryRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        tabSummaryRecycler.setAdapter(operationsAdapter);

        tabPlansError = (TextView) pagerAdapter.findViewById(R.id.tab_plans_error);
        tabPlansProgress = (ProgressBar) pagerAdapter.findViewById(R.id.tab_plans_progress);
        tabPlansRecycler = (RecyclerView) pagerAdapter.findViewById(R.id.tab_plans_recycler);

        final PlansAdapter plansAdapter = new PlansAdapter(getActivity(), plans);
        tabPlansRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        tabPlansRecycler.setAdapter(plansAdapter);

        tabWalletsError = (TextView) pagerAdapter.findViewById(R.id.tab_wallets_error);
        tabWalletsProgress = (ProgressBar) pagerAdapter.findViewById(R.id.tab_wallets_progress);
        tabWalletsRecycler = (RecyclerView) pagerAdapter.findViewById(R.id.tab_wallets_recycler);

        final WalletsAdapter walletsAdapter = new WalletsAdapter(getActivity(), wallets);
        tabWalletsRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        tabWalletsRecycler.setAdapter(walletsAdapter);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        operationsReference = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid())
                .child("operations");

        summaryListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                operations.clear();
                spent = 0;
                String lastDate = "";
                SimpleDateFormat dateFormat = DateUtils.getDateFormat(getContext());
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    Operation operation = new Operation(postSnapshot);
                    String operationDate = dateFormat.format(operation.getDate());
                    if (!operationDate.equals(lastDate)) {
                        lastDate = operationDate;
                        operations.add(new Separator(operationDate));
                    }
                    operations.add(operation);
                    spent += operation.getAmount();
                }
                //operations.add(new RecyclerButton("Show all"));
                operationsAdapter.notifyDataSetChanged();
                if (operations.size() > 0) {
                    tabSummaryProgress.setVisibility(View.GONE);
                    tabSummaryError.setVisibility(View.GONE);
                    tabSummaryRecycler.setVisibility(View.VISIBLE);
                } else {
                    tabSummaryProgress.setVisibility(View.GONE);
                    tabSummaryError.setVisibility(View.VISIBLE);
                    tabSummaryError.setText("Operations not found");
                    tabSummaryRecycler.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                tabSummaryProgress.setVisibility(View.GONE);
                tabSummaryError.setVisibility(View.VISIBLE);
                tabSummaryError.setText(R.string.data_loading_error);
                tabSummaryRecycler.setVisibility(View.GONE);
            }
        };

        plansReference = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid())
                .child("plans");

        plansListener = new ValueEventListener() {
            public void onDataChange(DataSnapshot dataSnapshot) {
                plans.clear();

                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    Plan plan = new Plan(postSnapshot);
                    plans.add(plan);
                }

                plansAdapter.notifyDataSetChanged();

                if (plans.size() > 0) {
                    tabPlansProgress.setVisibility(View.GONE);
                    tabPlansError.setVisibility(View.GONE);
                    tabPlansRecycler.setVisibility(View.VISIBLE);
                } else {
                    tabPlansProgress.setVisibility(View.GONE);
                    tabPlansError.setVisibility(View.VISIBLE);
                    tabPlansError.setText("Plans not found");
                    tabPlansRecycler.setVisibility(View.GONE);
                }
            }

            public void onCancelled(DatabaseError databaseError) {
                tabPlansProgress.setVisibility(View.GONE);
                tabPlansError.setVisibility(View.VISIBLE);
                tabPlansError.setText(R.string.data_loading_error);
                tabPlansRecycler.setVisibility(View.GONE);
            }
        };

        walletsReference = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid())
                .child("wallets");

        walletsListener = new ValueEventListener() {
            public void onDataChange(DataSnapshot dataSnapshot) {
                wallets.clear();
                amount = 0;
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    Wallet wallet = new Wallet(postSnapshot);
                    wallets.add(wallet);
                    amount += wallet.getAmount();
                }

                summaryAmount.setText(String.valueOf(amount - spent) + " $");

                walletsAdapter.notifyDataSetChanged();

                if (wallets.size() > 0) {
                    tabWalletsProgress.setVisibility(View.GONE);
                    tabWalletsError.setVisibility(View.GONE);
                    tabWalletsRecycler.setVisibility(View.VISIBLE);
                } else {
                    tabWalletsProgress.setVisibility(View.GONE);
                    tabWalletsError.setVisibility(View.VISIBLE);
                    tabWalletsError.setText("Wallets not found");
                    tabWalletsRecycler.setVisibility(View.GONE);
                }
            }

            public void onCancelled(DatabaseError databaseError) {
                tabWalletsProgress.setVisibility(View.GONE);
                tabWalletsError.setVisibility(View.VISIBLE);
                tabWalletsError.setText(R.string.data_loading_error);
                tabWalletsRecycler.setVisibility(View.GONE);
            }
        };
    }

    @Override
    public void onClick(View v) {
        menu.close(false);
        Intent intent = new Intent(getActivity(), EditActivity.class);
        intent.putExtra(EditActivity.MODE_TAG, EditActivity.MODE_ADD);
        switch (v.getId()) {
            case R.id.add_menu_income:
                intent.putExtra(EditActivity.TYPE_TAG, EditActivity.TYPE_INCOME);
                break;
            case R.id.add_menu_expense:
                intent.putExtra(EditActivity.TYPE_TAG, EditActivity.TYPE_EXPENSE);
                break;
            case R.id.add_menu_transfer:
                intent.putExtra(EditActivity.TYPE_TAG, EditActivity.TYPE_TRANSFER);
                break;
            case R.id.add_menu_plan:
                intent.putExtra(EditActivity.TYPE_TAG, EditActivity.TYPE_PLAN);
                break;
            case R.id.add_menu_wallet:
                intent.putExtra(EditActivity.TYPE_TAG, EditActivity.TYPE_WALLET);
                break;
            default:
                throw new RuntimeException("Unknown type of item");
        }
        startActivity(intent);
    }

    @Override
    public void onStart() {
        super.onStart();
        operationsReference.addValueEventListener(summaryListener);
        plansReference.addValueEventListener(plansListener);
        walletsReference.addValueEventListener(walletsListener);
    }

    @Override
    public void onStop() {
        operationsReference.removeEventListener(summaryListener);
        plansReference.removeEventListener(plansListener);
        walletsReference.removeEventListener(walletsListener);
        super.onStop();
    }
}
