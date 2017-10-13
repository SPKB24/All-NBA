package com.gmail.jorgegilcavazos.ballislife.features.games;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.gmail.jorgegilcavazos.ballislife.R;
import com.gmail.jorgegilcavazos.ballislife.data.firebase.MyMessagingService;
import com.gmail.jorgegilcavazos.ballislife.features.application.BallIsLifeApplication;
import com.gmail.jorgegilcavazos.ballislife.features.gamethread.CommentsActivity;
import com.gmail.jorgegilcavazos.ballislife.features.model.GameV2;
import com.gmail.jorgegilcavazos.ballislife.features.model.NbaGame;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Displays a list of {@link NbaGame}s for the selected date.
 */
public class GamesFragment extends Fragment implements GamesView,
        SwipeRefreshLayout.OnRefreshListener {
    private static final String SELECTED_TIME = "SelectedTime";
    private static final String LIST_STATE = "ListState";
    public final static String TAG = "GamesFragment";
    public final static String GAME_THREAD_HOME = "GAME_THREAD_HOME";
    public final static String GAME_THREAD_AWAY = "GAME_THREAD_AWAY";
    public final static String GAME_ID = "GAME_ID";
    public final static String GAME_DATE = "GAME_DATE";
    @Inject
    GamesPresenter presenter;

    @BindView(R.id.navigator_button_left) ImageButton btnPrevDay;
    @BindView(R.id.navigator_button_right) ImageButton btnNextDay;
    @BindView(R.id.navigator_text) TextView tvNavigatorDate;
    @BindView(R.id.no_games_text) TextView tvNoGames;
    @BindView(R.id.games_rv) RecyclerView rvGames;
    @BindView(R.id.swipe_refresh_layout) SwipeRefreshLayout swipeRefreshLayout;

    private Calendar selectedDate = Calendar.getInstance();
    private Parcelable listState;
    private RecyclerView.LayoutManager layoutManager;
    private GameAdapter gameAdapter;
    private Snackbar snackbar;
    private Unbinder unbinder;
    private GameItemListener gameItemListener = new GameItemListener() {
        @Override
        public void onGameClick(GameV2 clickedGame) {
            presenter.openGameDetails(clickedGame, selectedDate);
        }
    };
    private BroadcastReceiver scoresUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String gameData = intent.getStringExtra(MyMessagingService.KEY_SCORES_UPDATED);
            presenter.updateGames(gameData, selectedDate);
        }
    };

    public GamesFragment() {
        // Required empty public constructor.
    }

    public static GamesFragment newInstance() {
        return new GamesFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        BallIsLifeApplication.getAppComponent().inject(this);
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        layoutManager = new LinearLayoutManager(getActivity());
        gameAdapter = new GameAdapter(new ArrayList<>(0), gameItemListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_games, container, false);
        unbinder = ButterKnife.bind(this, view);

        swipeRefreshLayout.setOnRefreshListener(this);

        rvGames.setLayoutManager(layoutManager);
        rvGames.setAdapter(gameAdapter);

        btnPrevDay.setOnClickListener(v -> {
            selectedDate.add(Calendar.DAY_OF_YEAR, -1);
            presenter.onDateChanged();
            presenter.loadModels(selectedDate, false);
        });
        btnNextDay.setOnClickListener(v -> {
            selectedDate.add(Calendar.DAY_OF_YEAR, 1);
            presenter.onDateChanged();
            presenter.loadModels(selectedDate, false);
        });

        presenter.attachView(this);

        if (savedInstanceState != null) {
            selectedDate.setTimeInMillis(savedInstanceState.getLong(SELECTED_TIME));
        }

        return view;
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            listState = savedInstanceState.getParcelable(LIST_STATE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.loadModels(selectedDate, false);
        getActivity().registerReceiver(scoresUpdateReceiver,
                new IntentFilter(MyMessagingService.FILTER_SCORES_UPDATED));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        listState = layoutManager.onSaveInstanceState();
        outState.putLong(SELECTED_TIME, selectedDate.getTime().getTime());
        outState.putParcelable(LIST_STATE, listState);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(scoresUpdateReceiver);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
        presenter.stop();
        presenter.detachView();
    }

    @Override
    public void onRefresh() {
        presenter.loadModels(selectedDate, true);
    }

    @Override
    public void setLoadingIndicator(boolean active) {
        swipeRefreshLayout.setRefreshing(active);
    }

    @Override
    public void setDateNavigatorText(String dateText) {
        tvNavigatorDate.setText(dateText);
    }

    @Override
    public void hideGames() {
        rvGames.setVisibility(View.GONE);
    }

    @Override
    public void showGames(List<GameV2> games) {
        gameAdapter.swap(games);
        rvGames.setVisibility(View.VISIBLE);
    }

    @Override
    public void showGameDetails(GameV2 game, Calendar selectedDate) {
        Intent intent = new Intent(getActivity(), CommentsActivity.class);
        intent.putExtra(GAME_THREAD_HOME, game.getHomeTeamAbbr());
        intent.putExtra(GAME_THREAD_AWAY, game.getAwayTeamAbbr());
        intent.putExtra(GAME_ID, game.getId());
        intent.putExtra(GAME_DATE, selectedDate.getTimeInMillis());
        startActivity(intent);
    }

    @Override
    public void updateScores(List<GameV2> games) {
        gameAdapter.updateScores(games);
        rvGames.setVisibility(View.VISIBLE);
    }

    @Override
    public void setNoGamesIndicator(boolean active) {
        if (active) {
            tvNoGames.setVisibility(View.VISIBLE);
        } else {
            tvNoGames.setVisibility(View.GONE);
        }
    }

    @Override
    public void showNoNetSnackbar() {
        if (getView() == null) {
            return;
        }

        snackbar = Snackbar.make(getView(),
                                 R.string.your_device_is_offline,
                                 Snackbar.LENGTH_INDEFINITE)
                           .setAction(
                                   R.string.retry,
                                   v -> presenter.loadModels(selectedDate, true));
        snackbar.show();
    }

    @Override
    public void showErrorSnackbar() {
        if (getView() == null) {
            return;
        }

        snackbar = Snackbar.make(getView(),
                                 R.string.something_went_wrong, Snackbar.LENGTH_SHORT)
                           .setAction(
                                   R.string.retry,
                                   v -> presenter.loadModels(selectedDate, true));
        snackbar.show();
    }

    @Override
    public void dismissSnackbar() {
        if (snackbar != null) {
            snackbar.dismiss();
        }
    }

    public interface GameItemListener {
        void onGameClick(GameV2 clickedGame);
    }
}
